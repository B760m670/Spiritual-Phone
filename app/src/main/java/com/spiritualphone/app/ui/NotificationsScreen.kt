package com.spiritualphone.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import kotlinx.coroutines.launch

/** Notification settings: master toggle + Звук + Вибросигнал (all functional). */
@Composable
internal fun NotificationsScreen(repo: ProfileRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState(initial = UserProfile())
    val on = profile.notificationsEnabled

    SubScreenScaffold("Уведомления", onBack = onBack) {
        Grouped(ROW) {
            ToggleRow(Icons.Filled.Notifications, "Уведомления", on) {
                scope.launch { repo.setNotificationsEnabled(it) }
            }
            Separator(startInset = 52.dp)
            ToggleRow(Icons.Filled.VolumeUp, "Звук", profile.soundEnabled, enabled = on) {
                scope.launch { repo.setSoundEnabled(it) }
            }
            Separator(startInset = 52.dp)
            ToggleRow(Icons.Filled.Vibration, "Вибросигнал", profile.vibrationEnabled, enabled = on) {
                scope.launch { repo.setVibrationEnabled(it) }
            }
        }
    }
}
