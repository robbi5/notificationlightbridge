package com.robbi5.notificationlightbridge

import android.annotation.TargetApi
import android.app.Notification.FLAG_SHOW_LIGHTS
import android.app.NotificationManager
import android.content.IntentFilter
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log


class NotificationListenerService: android.service.notification.NotificationListenerService() {

    private var notificationManager: NotificationManager? = null
    private val setColorReceiver = SetColorReceiver()
    private val led = LED()

    override fun onListenerConnected() {
        super.onListenerConnected()
        notificationManager = application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val setColorReceiverIntent = IntentFilter()
        setColorReceiverIntent.addAction("com.robbi5.notificationlightbridge.TURN_OFF")
        setColorReceiverIntent.addAction("com.robbi5.notificationlightbridge.SET_COLOR")
        registerReceiver(setColorReceiver, setColorReceiverIntent)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterReceiver(setColorReceiver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || notificationManager == null) return
        val color = pullColor(sbn)
        if (color != 0) {
            led.send(listOf(LED.TURN_ON, color))
        }
    }

    fun pullColor(sbn: StatusBarNotification): Int {
        var color: Int? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            color = pullColorFromChannelApi26(sbn)
        }
        @Suppress("DEPRECATION")
        if (color == null) {
            if (sbn.notification.ledOnMS == 0) return 0
            if (sbn.notification.flags and FLAG_SHOW_LIGHTS == 0) return 0
            color = led.colorMapping(sbn.notification.ledARGB)
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

        return led.colorMapping(channel.lightColor)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.i("NLS", "removed notification pkg=${sbn?.packageName}, txt=${sbn?.notification?.tickerText}")

        val notifications = activeNotifications.filter { pullColor(it) != 0 }
        if (notifications.isEmpty()) {
            Log.i("NLS", "found no other notifications, turning off")
            led.send(LED.TURN_OFF)
            return
        }
        val n = notifications.maxByOrNull { it.postTime }!!
        val color = pullColor(n)
        Log.i("NLS", "found still active notification, pkg=${n.packageName}, txt=${n.notification.tickerText}")
        led.send(color)
    }
}