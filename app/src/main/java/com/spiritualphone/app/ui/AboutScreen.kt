package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.BuildConfig

/** App info screen. */
@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    SubScreenScaffold("О приложении", onBack = onBack) {
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Spiritual Phone", color = WHITE, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Версия ${BuildConfig.VERSION_NAME}", color = SUBTLE, fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))
        Grouped(CARD) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Денрейсинки — детектор духовной активности. Пустые появляются как локальные аномалии; держите телефон под рукой.",
                    color = TEXT,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
