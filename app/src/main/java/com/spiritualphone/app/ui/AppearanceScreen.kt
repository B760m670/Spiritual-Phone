package com.spiritualphone.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import kotlinx.coroutines.launch

/** Appearance settings. */
@Composable
internal fun AppearanceScreen(repo: ProfileRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState(initial = UserProfile())

    SubScreenScaffold("Внешний вид", onBack = onBack) {
        Grouped(ROW) {
            ToggleRow(Icons.Filled.Label, "Подписи во вкладках", profile.tabLabels) {
                scope.launch { repo.setTabLabels(it) }
            }
        }
    }
}
