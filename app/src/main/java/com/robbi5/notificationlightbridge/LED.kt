package com.robbi5.notificationlightbridge

import android.graphics.Color
import android.util.Log
import java.io.DataOutputStream

class LED {

    /*
     * If a BRIGHTNESS_UP or BRIGHTNESS_DOWN is sent right after a COLOR_ command, the corresponding
     * color is displayed brighter or dimmer.
     *
     * However, if SPEED_UP or SPEED_DOWN is sent right a special mode (CYCLE_RAINBOW, CYCLE_RGB,
     * BREATH_WHITE or FADE_RAINBOW), the speed of the effect is in- or decreased.
     *
     * Since BRIGHTNESS_UP/SPEED_UP and BRIGHTNESS_DOWN/SPEED_DOWN are sharing the same code, an
     * effect cannot be dimmed. However, since most effects run always at full brightness, this
     * should be negligible.
     *
     * The names of the colors are taken from the documentation of the IR RGB remote control that
     * inspired the manufacturer to do this craziness this way.
     *
     * The comments next to the special modes are the official names of the modes on the IR remote
     *
     * Brightness and speed are a set on a 75 step range.
     */
    companion object {
        const val BRIGHTNESS_UP = 0x00; const val SPEED_UP = 0x00
        const val BRIGHTNESS_DOWN = 0x01; const val SPEED_DOWN = 0x01
        const val TURN_OFF = 0x02
        const val TURN_ON = 0x03

        const val COLOR_RED = 0x04
        const val COLOR_GREEN = 0x05
        const val COLOR_BLUE = 0x06
        const val COLOR_WHITE = 0x07

        const val COLOR_ORANGE = 0x08
        const val COLOR_MINT = 0x09
        const val COLOR_DARKBLUE = 0x0A
        const val CYCLE_RAINBOW = 0x0B // "Flash"

        const val COLOR_BROWN = 0x0C
        const val COLOR_AQUA = 0x0D
        const val COLOR_PURPLE = 0x0E
        const val BREATH_WHITE = 0x0F // "Strobe"

        const val COLOR_BEIGE = 0x10
        const val COLOR_TOPAZ = 0x11
        const val COLOR_FUCHSIA = 0x12
        const val FADE_RAINBOW = 0x13

        const val COLOR_YELLOW = 0x14
        const val COLOR_SKY_BLUE = 0x15
        const val COLOR_PINK = 0x16
        const val CYLCE_RGB = 0x17 // "Smooth"
    }

    fun colorMapping(notificationColor: Int): Int {
        var hsv: FloatArray = floatArrayOf(0F, 0F, 0F)
        Color.colorToHSV(notificationColor, hsv)

        if (hsv[1] == 0F && hsv[2] == 100F) return COLOR_WHITE
        if (hsv[2] == 0F) return TURN_OFF // aka: black

        return when (hsv[0]) {
            in 0F..30F -> COLOR_RED
            in 30F..60F -> COLOR_BROWN // orange
            in 60F..90F -> COLOR_YELLOW
            in 90F..120F -> COLOR_GREEN
            in 120F..150F -> COLOR_MINT
            in 150F..180F -> COLOR_AQUA
            in 210F..240F -> COLOR_BLUE
            in 240F..270F -> COLOR_PURPLE
            in 270F..300F -> COLOR_PINK // magenta
            in 300F..330F -> COLOR_PINK
            in 330F..360F -> COLOR_RED
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