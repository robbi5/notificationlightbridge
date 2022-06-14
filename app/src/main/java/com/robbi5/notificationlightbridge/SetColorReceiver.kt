package com.robbi5.notificationlightbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SetColorReceiver : BroadcastReceiver() {
    companion object {
        const val INTENT_TURN_OFF = "com.robbi5.notificationlightbridge.TURN_OFF"
        const val INTENT_SET_COLOR = "com.robbi5.notificationlightbridge.SET_COLOR"
        const val INTENT_SET_RAW = "com.robbi5.notificationlightbridge.SET_RAW"
        val intent_actions = listOf(
            INTENT_TURN_OFF,
            INTENT_SET_COLOR,
            INTENT_SET_RAW
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val led = LED()
        when (intent.action) {
            INTENT_TURN_OFF -> {
                led.send(LED.TURN_OFF)
            }
            INTENT_SET_COLOR -> {
                if (intent.hasExtra("color")) {
                    led.send(
                        listOf(
                            LED.TURN_ON,
                            led.colorMapping(intent.getIntExtra("color", LED.TURN_OFF))
                        )
                    )
                }
            }
            INTENT_SET_RAW -> {
                if (intent.hasExtra("cmd")) {
                    led.send(
                        intent.getIntArrayExtra("values").toList()
                    )
                }
            }
        }
    }
}