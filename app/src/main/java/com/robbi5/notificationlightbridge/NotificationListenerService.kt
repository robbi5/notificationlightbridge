package com.robbi5.notificationlightbridge

import android.annotation.TargetApi
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import java.io.DataOutputStream


class NotificationListenerService: android.service.notification.NotificationListenerService() {

    companion object {
        const val COLOR_OFF = 0x02
        const val COLOR_ON = 0x03
    }

    private var notificationManager: NotificationManager? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        notificationManager = application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || notificationManager == null) return
        val color = pullColor(sbn)
        if (color != 0) {
            send(listOf(COLOR_ON, color)) // on
        }
    }

    fun pullColor(sbn: StatusBarNotification): Int {
        var color: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            color = pullColorFromChannelApi26(sbn)
        }
        if (color == null) {
            color = colorMapping(sbn.notification.ledARGB)
        }
        return color
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun pullColorFromChannelApi26(sbn: StatusBarNotification): Int? {
        Log.i("NLS", "got notification pkg=${sbn?.packageName}, txt=${sbn?.notification?.tickerText}")
        if (sbn.notification.channelId.isNullOrEmpty()) {
            Log.i("NLS", "notification has no channel id")
            return null
        }

        val channel = notificationManager!!.getNotificationChannel(sbn.notification.channelId)
        if (channel == null || !channel.shouldShowLights()) {
            Log.i("NLS", "notification has no channel or shouldn't light")
            return null
        }

        return colorMapping(channel.lightColor)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.i("NLS", "removed notification pkg=${sbn?.packageName}, txt=${sbn?.notification?.tickerText}")

        val notifications = activeNotifications.filter { pullColor(it) != 0 }
        if (notifications.isEmpty()) {
            Log.i("NLS", "found no other notifications, turning off")
            send(COLOR_OFF) // off
            return
        }
        val n = notifications.maxByOrNull { it.postTime }!!
        val color = pullColor(n)
        Log.i("NLS", "found still active notification, pkg=${n.packageName}, txt=${n.notification.tickerText}")
        send(color)
    }

    fun colorMapping(notificationColor: Int): Int {
        var hsv : FloatArray = floatArrayOf(0F, 0F, 0F)
        Color.colorToHSV(notificationColor, hsv)

        if (hsv[1] == 0F && hsv[2] == 100F) return 0x07 // white
        if (hsv[2] == 0F) return 0 // black

        return when(hsv[0]) {
            in   0F .. 30F  -> 0x04 // red
            in  30F .. 60F  -> 0x0C // orange
            in  60F .. 90F  -> 0x14 // yellow
            in  90F .. 120F -> 0x05 // green
            in 120F .. 150F -> 0x09
            in 150F .. 180F -> 0x0D
            in 210F .. 240F -> 0x06 // blue
            in 240F .. 270F -> 0x0E
            in 270F .. 300F -> 0x16 // magenta
            in 300F .. 330F -> 0x16
            in 330F .. 360F -> 0x04 // red
            else -> 0
        }
    }

    fun send(color: Int) {
        send(listOf(color))
    }

    fun send(colors: List<Int>) {
        val colorHexs = colors.map { "%02x".format(it) }
        Log.i("NLS", "sending ${colorHexs.joinToString()}")
        val commands = colorHexs.map { "echo w 0x${it} > /sys/devices/platform/led_con_h/zigbee_reset\nsleep 0.1" }
        rootedExec(commands)
    }

    fun rootedExec(commands: List<String>) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (command in commands) {
                Log.i("NLS", "command ${command}")
                os.writeBytes(command + "\n")
            }
            os.writeBytes("exit\n")
            os.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}