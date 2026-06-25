package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Privacy section — hosts the local app lock (analog of SpiritChat's cloud password). */
@Composable
internal fun PrivacyScreen(lockEnabled: Boolean, onBack: () -> Unit, onOpenAppLock: () -> Unit) {
    SubScreenScaffold("Конфиденциальность", onBack = onBack) {
        Grouped(ROW) {
            SettingRow(Icons.Filled.Lock, "Блокировка приложения", onClick = onOpenAppLock) {
                Text(if (lockEnabled) "Вкл" else "Выкл", color = SUBTLE, fontSize = 15.sp)
                Icon(Icons.Filled.ChevronRight, null, tint = SUBTLE, modifier = Modifier.size(18.dp))
            }
        }
    }
}
