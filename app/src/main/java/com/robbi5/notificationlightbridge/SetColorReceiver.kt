package com.robbi5.notificationlightbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SetColorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val led = LED()
        when (intent.action) {
            "com.robbi5.notificationlightbridge.TURN_OFF" -> {
                led.send(LED.TURN_OFF)
            }
            "com.robbi5.notificationlightbridge.SET_COLOR" -> {
                if (intent.hasExtra("color")) {
                    led.send(
                        listOf(
                            LED.TURN_ON,
                            led.colorMapping(intent.getIntExtra("color", LED.TURN_OFF))
                        )
                    )
                }
            }
        }
    }
}