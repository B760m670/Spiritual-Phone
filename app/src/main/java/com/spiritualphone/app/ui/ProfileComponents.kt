package com.spiritualphone.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/** Glass corner pill (Изм. / Отмена / Готово). */
@Composable
internal fun CornerPill(label: String, bold: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(BTN_H)
            .glass(shape = RoundedCornerShape(BTN_H / 2), tint = Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = WHITE,
            fontSize = 17.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Circular glass QR button (44 dp). */
@Composable
internal fun QrThumb(onClick: () -> Unit) {
    Box(
        Modifier
            .size(BTN_H)
            .glass(shape = CircleShape, tint = Color.Black.copy(alpha = 0.42f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.QrCode2, contentDescription = "QR профиля", tint = WHITE, modifier = Modifier.size(24.dp))
    }
}

/** A rounded dark group surface (rows/fields supply their own padding). */
@Composable
internal fun Grouped(bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg),
        content = content,
    )
}

/** iOS/Telegram-style settings row: icon square + label + trailing control. */
@Composable
internal fun SettingRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(ICON_BG),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = WHITE, modifier = Modifier.size(16.dp))
        }
        Text(label, color = TEXT, fontSize = 17.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Settings row with a trailing Switch (greyed out when [enabled] is false). */
@Composable
internal fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingRow(icon, label) {
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else null,
            enabled = enabled,
        )
    }
}

/** Labelled plain text field inside a group card. */
@Composable
internal fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    numeric: Boolean = false,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = LABEL, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = WHITE, fontSize = 16.sp),
            cursorBrush = SolidColor(WHITE),
            keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = PLACEHOLDER, fontSize = 16.sp)
                    inner()
                }
            },
        )
    }
}

/** Hairline separator, inset from the start like iOS lists. */
@Composable
internal fun Separator(startInset: Dp = 16.dp) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(1.dp)
            .background(SEP)
    )
}

/**
 * Full-screen sub-screen shell: a nav bar (glass back button + centred title +
 * optional Готово pill) over the standard black background, with scrollable
 * content. Mirrors SpiritChat's privacy/cloud-password layout.
 */
@Composable
internal fun SubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    done: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(color = BG, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(42.dp)
                        .glass(shape = CircleShape, tint = Color.Black.copy(alpha = 0.42f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ChevronLeft, "Назад", tint = WHITE, modifier = Modifier.size(26.dp))
                }
                Text(
                    title,
                    color = WHITE,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (done != null) {
                    CornerPill("Готово", bold = true, onClick = done)
                } else {
                    Spacer(Modifier.size(42.dp))
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

/** Enlarged QR for someone to scan, on a glass card. */
@Composable
internal fun QrDialog(content: String, title: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(Color(0xF210141E), RoundedCornerShape(24.dp))
                .glass(shape = RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            val big = rememberQrBitmap(content, 720)
            if (big != null) {
                Box(
                    Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                ) {
                    Image(bitmap = big, contentDescription = "QR профиля", modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Покажите код, чтобы поделиться профилем", color = SUBTLE, fontSize = 12.sp)
        }
    }
}
