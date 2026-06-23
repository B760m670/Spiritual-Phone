package com.spiritualphone.app.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spiritualphone.app.R
import com.spiritualphone.app.model.Hollow

/**
 * Posts a phone notification when a Hollow enters the alert radius.
 *
 * The alert sound (res/raw/hollow_spawn) is attached to the notification
 * CHANNEL, so it plays even when the app is in the background — a foreground-
 * only MediaPlayer cannot do that. The channel id is versioned because a
 * channel's sound is fixed once created.
 */
class HollowNotifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = Uri.parse(
                "android.resource://${context.packageName}/${R.raw.hollow_spawn}"
            )
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Обнаружение пустых",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Оповещения о появлении пустых поблизости"
                setSound(soundUri, attributes)
            }
            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    fun notifySpawn(hollow: Hollow) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Обнаружен Пустой")
            .setContentText("В пределах 1.2 км зафиксирована духовная активность")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(hollow.id.hashCode(), notification) }
    }

    /**
     * Posts a proximity alert from a scheduled background alarm, where no live
     * [Hollow] object exists — only the precomputed id from the deterministic
     * world. Same channel (and sound) as [notifySpawn].
     */
    @SuppressLint("MissingPermission")
    fun notifyApproach(hollowId: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Обнаружен Пустой")
            .setContentText("В пределах 1.2 км зафиксирована духовная активность")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(hollowId.hashCode(), notification) }
    }

    companion object {
        // Bump suffix if the channel's sound/behaviour changes.
        private const val CHANNEL_ID = "hollow_detection_v2"
    }
}
