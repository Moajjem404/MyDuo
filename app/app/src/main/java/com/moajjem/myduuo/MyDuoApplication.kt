package com.moajjem.myduuo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MyDuoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Channel for the foreground service status
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "MyDuo Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors foreground applications changes and sends updates to your partner."
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            // Channel for partner activity notifications
            val partnerChannel = NotificationChannel(
                CHANNEL_PARTNER_ID,
                "Partner Activity Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when your partner changes the active application."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            // Channel for partner love ping notifications
            val lovePingChannel = NotificationChannel(
                CHANNEL_LOVE_PING_ID,
                "Love Pings & Instant Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you instantly with custom sound when your partner sends a Love Ping."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(lovePingChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "myduo_service_channel"
        const val CHANNEL_PARTNER_ID = "myduo_partner_channel"
        const val CHANNEL_LOVE_PING_ID = "myduo_love_ping_channel"
    }
}
