package com.spiritualphone.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.world.AlertConfig
import com.spiritualphone.app.world.DeterministicWorld

/**
 * Because the world is deterministic, its future is computable. When the app
 * goes to the background we look ahead along the user's last known location and
 * pre-schedule a local alarm for each Hollow that will enter the alert radius.
 * Android fires those alarms (waking the device) even while the app is closed,
 * so proximity alerts arrive without any server or push service.
 *
 * Uses inexact `setAndAllowWhileIdle` — fires through Doze without needing the
 * restricted SCHEDULE_EXACT_ALARM permission.
 */
object HollowAlarmScheduler {

    private const val HORIZON_MS = 60 * 60_000L     // look 60 min ahead
    private const val STEP_MS = 60_000L             // sample every minute
    private const val MAX_ALARMS = 8

    const val EXTRA_ID = "hollow_id"

    fun schedule(context: Context, lat: Double, lon: Double) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()

        // Hollows already in range now shouldn't trigger a future alarm.
        val seen = HashSet<String>()
        DeterministicWorld.hollowsNear(lat, lon, AlertConfig.ALERT_RADIUS_M, now)
            .forEach { seen.add(it.id) }

        var scheduled = 0
        var t = now + STEP_MS
        while (t <= now + HORIZON_MS && scheduled < MAX_ALARMS) {
            for (h in DeterministicWorld.hollowsNear(lat, lon, AlertConfig.ALERT_RADIUS_M, t)) {
                if (!seen.add(h.id)) continue
                scheduleOne(context, am, h.id, t)
                if (++scheduled >= MAX_ALARMS) break
            }
            t += STEP_MS
        }
        DebugLog.log("Scheduled $scheduled hollow alarm(s) ahead")
    }

    private fun scheduleOne(context: Context, am: AlarmManager, id: String, triggerAtMs: Long) {
        val intent = Intent(context, HollowAlarmReceiver::class.java).putExtra(EXTRA_ID, id)
        val pi = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
    }
}
