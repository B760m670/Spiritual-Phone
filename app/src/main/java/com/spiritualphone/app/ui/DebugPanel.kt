package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.debug.DebugLog

/** Full-screen scrollable log view with copy/clear, for on-device diagnostics. */
@Composable
fun DebugPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current

    Surface(color = Color(0xF20B0E14), modifier = modifier.fillMaxSize()) {
        Column(Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "ЛОГИ",
                    color = Color(0xFFEDEDED),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(DebugLog.dump()))
                }) { Text("Копировать") }
                TextButton(onClick = { DebugLog.clear() }) { Text("Очистить") }
                TextButton(onClick = onClose) { Text("Закрыть") }
            }
            LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                items(DebugLog.lines) { line ->
                    Text(
                        line,
                        color = Color(0xFFB0B6C0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}
