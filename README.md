NotificationLightBridge
=======================

This project adapts Notification Colors to the led light bar of a WA1053T Android Wall Mount Tablet.

Colors are usually defined [on a NotificationChannel](https://developer.android.com/reference/android/app/NotificationChannel#setLightColor(int)),
or, on api version < 26 on [the notifications ledARGB property](https://developer.android.com/reference/android/app/Notification#ledARGB).

Note: Flashing the leds is not supported.

Installation/Setup
------------------

After compiling and installing the apk, navigate to
`Settings → Apps & notifications → Special app access → Notification access` and enable the
_NotificationLightBridge_ entry.

