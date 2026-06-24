package com.spiritualphone.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Local @handle editor (no server — format-validated only). */
@Composable
internal fun UsernameScreen(initial: String, onBack: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    val trimmed = value.trim()
    val valid = trimmed.isEmpty() ||
        (trimmed.length in 3..20 && trimmed.first() in 'a'..'z' &&
            trimmed.all { it in 'a'..'z' || it in '0'..'9' || it == '_' })

    SubScreenScaffold("Имя пользователя", onBack = onBack, done = { if (valid) onSave(trimmed) }) {
        Grouped(CARD) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Позывной", color = LABEL, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("@", color = SUBTLE, fontSize = 16.sp)
                    BasicTextField(
                        value = value,
                        onValueChange = { v ->
                            value = v.lowercase().filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }.take(20)
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = WHITE, fontSize = 16.sp),
                        cursorBrush = SolidColor(WHITE),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box {
                                if (value.isEmpty()) Text("kurosaki", color = PLACEHOLDER, fontSize = 16.sp)
                                inner()
                            }
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                value.isBlank() -> "Другие смогут найти вас по позывному."
                valid -> "Доступно."
                else -> "3–20 символов: латиница, цифры, _ (начинается с буквы)."
            },
            color = if (value.isBlank()) SUBTLE else if (valid) OK else ERR,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
