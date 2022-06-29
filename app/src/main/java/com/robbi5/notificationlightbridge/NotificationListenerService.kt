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
    private val pretixReceiver = PretixReceiver()
    private val led = LED()

    override fun onListenerConnected() {
        super.onListenerConnected()
        notificationManager = application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val setColorReceiverIntent = IntentFilter()
        SetColorReceiver.intent_actions.map {
            setColorReceiverIntent.addAction(it)
        }
        registerReceiver(setColorReceiver, setColorReceiverIntent)

        val pretixReceiverIntent = IntentFilter()
        PretixReceiver.intent_actions.map {
            pretixReceiverIntent.addAction(it)
        }
        registerReceiver(pretixReceiver, pretixReceiverIntent)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterReceiver(setColorReceiver)
        unregisterReceiver(pretixReceiver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !rankingMap.orderedKeys.contains(sbn.key)) {
            // ranking map was worthless (or is not available), lets try to pull the color from the notification itself
            return onNotificationPosted(sbn)
        }

        val r = Ranking()
        rankingMap.getRanking(sbn.key, r)
        if (r.channel != null && r.channel.shouldShowLights()) {
            Log.i("NLS", "found active notification with channel color, pkg=${sbn.packageName}, channel=${r.channel.name}")
            val color = led.colorMapping(r.channel.lightColor)
            if (color != 0) {
                led.send(listOf(LED.TURN_ON, color))
                return
            }
        }

        // channel extraction didn't find a color, continue with the classic way
        return onNotificationPosted(sbn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (notificationManager == null) return
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
        Log.i("NLS", "got notification pkg=${sbn.packageName}, txt=${sbn.notification?.tickerText}")
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

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // lets try to pull the color from the remaining notification itself
            return onNotificationRemoved(sbn)
        }

        if (activeNotifications.isEmpty() || rankingMap.orderedKeys.isEmpty()) {
            Log.i("NLS", "found no other notifications, turning off")
            led.send(LED.TURN_OFF)
            return
        }

        val rankedColor = rankingMap.orderedKeys.map { key ->
            val r = Ranking()
            rankingMap.getRanking(key, r)
            if (r.channel != null && r.channel.shouldShowLights()) {
                Log.i("NLS", "found still active notification with channel color, channel=${r.channel.name}")
                return@map led.colorMapping(r.channel.lightColor)
            }
            null
        }.filterNotNull().firstOrNull()

        if (rankedColor != null && rankedColor != 0) {
            led.send(listOf(LED.TURN_ON, rankedColor))
            return
        }

        return onNotificationRemoved(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.i("NLS", "removed notification pkg=${sbn.packageName}, txt=${sbn.notification?.tickerText}")

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