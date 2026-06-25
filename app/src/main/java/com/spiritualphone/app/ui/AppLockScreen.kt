package com.spiritualphone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.data.ProfileRepository
import kotlinx.coroutines.launch

/**
 * App lock setup. Stores a SHA-256 of a PIN locally; the launch gate in
 * MainActivity asks for it. The local equivalent of "облачный пароль" — no
 * server, fully functional (not a dead button).
 */
@Composable
internal fun AppLockScreen(repo: ProfileRepository, currentHash: String?, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    if (currentHash == null) {
        var pin by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        val canSave = pin.length >= 4 && confirm.isNotEmpty()

        SubScreenScaffold(
            "Блокировка",
            onBack = onBack,
            done = if (canSave) {
                {
                    if (pin != confirm) {
                        error = "PIN не совпадают"
                    } else {
                        scope.launch { repo.setAppLock(pin) }
                        onBack()
                    }
                }
            } else null,
        ) {
            LockHero("Защита приложения", "PIN из 4+ цифр будет нужен при каждом запуске.")
            Grouped(CARD) {
                PinField("Новый PIN", pin) { pin = it }
                Separator()
                PinField("Повтор", confirm) { confirm = it }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = ERR, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    } else {
        SubScreenScaffold("Блокировка", onBack = onBack) {
            LockHero("Блокировка включена", "PIN запрашивается при запуске приложения.")
            Grouped(ROW) {
                SettingRow(Icons.Filled.LockOpen, "Отключить пароль", onClick = {
                    scope.launch { repo.clearAppLock() }
                    onBack()
                }) {}
            }
        }
    }
}

/** Centred lock icon + title + description, shared by the app-lock screens. */
@Composable
private fun ColumnScope.LockHero(title: String, desc: String) {
    Spacer(Modifier.height(12.dp))
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(ICON_BG),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, null, tint = WHITE, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, color = WHITE, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(desc, color = SUBTLE, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(24.dp))
}

/** Numeric PIN field inside a group card. */
@Composable
private fun PinField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = LABEL, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = { v -> onValueChange(v.filter { it.isDigit() }.take(8)) },
            singleLine = true,
            textStyle = TextStyle(color = WHITE, fontSize = 16.sp),
            cursorBrush = SolidColor(WHITE),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("••••", color = PLACEHOLDER, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}
