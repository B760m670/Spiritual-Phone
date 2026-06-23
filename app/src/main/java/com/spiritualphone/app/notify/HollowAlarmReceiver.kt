package com.spiritualphone.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.debug.DebugLog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Fires when a pre-scheduled Hollow approach alarm goes off (app may be closed).
 * Posts the proximity notification, honouring the user's notification setting.
 */
class HollowAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(HollowAlarmScheduler.EXTRA_ID) ?: return

        val enabled = runCatching {
            runBlocking { ProfileRepository(context).profile.first().notificationsEnabled }
        }.getOrDefault(true)
        if (!enabled) return

        DebugLog.log("Alarm fired: hollow $id within alert radius")
        HollowNotifier(context).notifyApproach(id)
    }
}
