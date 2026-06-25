package com.spiritualphone.app.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.spiritualphone.app.R
import com.spiritualphone.app.model.Hollow

/**
 * Posts a phone notification when a Hollow enters the alert radius.
 *
 * Sound and vibration are properties of a notification CHANNEL and can't be
 * changed after a channel is created, so we keep one channel per (sound,
 * vibration) combination and post on the one matching the user's current
 * settings — that's how the Звук / Вибросигнал toggles actually take effect,
 * even for background alarms. (minSdk 26, so channels always exist.)
 */
class HollowNotifier(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun notifySpawn(hollow: Hollow, sound: Boolean, vibrate: Boolean) =
        post(hollow.id.hashCode(), sound, vibrate)

    /**
     * Posts a proximity alert from a scheduled background alarm, where no live
     * [Hollow] object exists — only the precomputed id from the deterministic
     * world.
     */
    @SuppressLint("MissingPermission")
    fun notifyApproach(hollowId: String, sound: Boolean, vibrate: Boolean) =
        post(hollowId.hashCode(), sound, vibrate)

    private fun post(notificationId: Int, sound: Boolean, vibrate: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, ensureChannel(sound, vibrate))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠ Обнаружен Пустой")
            .setContentText("В пределах 1.2 км зафиксирована духовная активность")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        runCatching { manager.notify(notificationId, notification) }
    }

    /** Creates (idempotently) and returns the channel for a sound/vibrate combo. */
    private fun ensureChannel(sound: Boolean, vibrate: Boolean): String {
        val id = "hollow_detection_s${if (sound) 1 else 0}_v${if (vibrate) 1 else 0}"
        val channel = NotificationChannel(
            id,
            "Обнаружение пустых",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Оповещения о появлении пустых поблизости"
            if (sound) {
                val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.hollow_spawn}")
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, attributes)
            } else {
                setSound(null, null)
            }
            enableVibration(vibrate)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        return id
    }
}
