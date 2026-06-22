package com.spiritualphone.app.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spiritualphone.app.model.Hollow

/**
 * Posts a phone notification when a Hollow appears. The custom alert sound is
 * played separately (see SoundManager); this is the system notification that
 * also works when the app is in the background.
 */
class HollowNotifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обнаружение пустых",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Оповещения о появлении пустых поблизости" }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun notifySpawn(hollow: Hollow) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Обнаружен Пустой")
            .setContentText("Поблизости зафиксирована духовная активность")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(hollow.id.hashCode(), notification) }
    }

    companion object {
        private const val CHANNEL_ID = "hollow_detection"
    }
}
