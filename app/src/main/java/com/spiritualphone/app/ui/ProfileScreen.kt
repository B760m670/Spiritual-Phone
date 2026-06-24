package com.spiritualphone.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Deep link encoded in the profile QR. */
private fun profileUrl(userId: String?) = "https://spiritualphone.app/u/${userId ?: "anon"}"

// Monochrome palette (no blue, no red), tones taken from SpiritChat.
private val BG = Color(0xFF000000)
private val CARD = Color(0xFF111114)       // edit-field group
private val ROW = Color(0xFF1C1C1E)        // settings-row group
private val ICON_BG = Color(0xFF2C2C2E)
private val AVATAR_BG = Color(0xFF27272A)
private val TEXT = Color(0xFFEDEDED)
private val WHITE = Color(0xFFFFFFFF)
private val SUBTLE = Color(0xFF8E8E93)
private val LABEL = Color(0xFFA1A1AA)
private val PLACEHOLDER = Color(0xFF3F3F46)
private val SEP = Color(0xFF27272A)

private val AVATAR = 100.dp
private val BTN_H = 44.dp

/**
 * Profile section, SpiritChat-style. One screen with two cross-faded modes:
 *
 *  - Overview: avatar, name, settings rows; "Изм." pill (top-right), QR (top-left).
 *  - Edit:     same avatar IN THE SAME PLACE (so it doesn't move), editable
 *              fields, "Отмена"/"Готово" pills.
 *
 * The avatar is rendered outside the cross-faded content, so switching modes
 * leaves it perfectly still — the effect SpiritChat fakes with two same-layout
 * screens, done here natively with one.
 */
@Composable
fun ProfileScreen(repo: ProfileRepository, onOpenLogs: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState(initial = UserProfile())

    var editing by remember { mutableStateOf(false) }
    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }
    var age by remember(profile.age) { mutableStateOf(profile.age) }

    var userId by remember { mutableStateOf(profile.userId) }
    LaunchedEffect(Unit) { userId = repo.ensureUserId() }
    var showQr by remember { mutableStateOf(false) }

    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val file = File(context.filesDir, "avatar_${System.currentTimeMillis()}.jpg")
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { input.copyTo(it) }
                    }
                }
                repo.setAvatarPath(file.absolutePath)
            }
        }
    }

    val avatar = remember(profile.avatarPath) {
        profile.avatarPath?.let {
            runCatching { BitmapFactory.decodeFile(it) }.getOrNull()?.asImageBitmap()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Surface(color = BG, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(64.dp))  // clear the corner buttons

                // Avatar — fixed position across both modes.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(AVATAR)
                            .clip(CircleShape)
                            .background(AVATAR_BG)
                            .clickable(enabled = editing) { pickAvatar.launch("image/*") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatar != null) {
                            Image(
                                bitmap = avatar,
                                contentDescription = "Аватар",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(AVATAR).clip(CircleShape),
                            )
                        } else {
                            Icon(Icons.Filled.Person, null, tint = SUBTLE, modifier = Modifier.size(52.dp))
                        }
                        // Camera overlay only in edit mode.
                        AnimatedVisibility(visible = editing, enter = fadeIn(), exit = fadeOut()) {
                            Box(
                                Modifier.size(AVATAR).clip(CircleShape).background(Color(0x73000000)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PhotoCamera, null, tint = WHITE, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Name / "choose photo" — cross-faded, avatar above stays put.
                Crossfade(targetState = editing, label = "nameArea") { isEdit ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isEdit) {
                            Text(
                                "Выбрать фотографию",
                                color = WHITE,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { pickAvatar.launch("image/*") },
                            )
                        } else {
                            Text(
                                nickname.ifEmpty { "Мой профиль" },
                                color = WHITE,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            if (age.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("$age лет", color = SUBTLE, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Content — cross-faded between settings rows and edit fields.
                Crossfade(targetState = editing, label = "content") { isEdit ->
                    if (isEdit) {
                        Grouped(CARD) {
                            EditField("Имя", nickname, { nickname = it }, "Ваше имя")
                            Separator()
                            EditField(
                                "Возраст", age,
                                { input -> age = input.filter { it.isDigit() }.take(3) },
                                "Сколько вам лет",
                                numeric = true,
                            )
                        }
                    } else {
                        Grouped(ROW) {
                            SettingRow(Icons.Filled.Notifications, "Уведомления") {
                                Switch(
                                    checked = profile.notificationsEnabled,
                                    onCheckedChange = { scope.launch { repo.setNotificationsEnabled(it) } },
                                )
                            }
                            Separator(startInset = 52.dp)
                            SettingRow(Icons.Filled.BugReport, "Логи (отладка)", onClick = onOpenLogs) {
                                Icon(Icons.Filled.ChevronRight, null, tint = SUBTLE, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(110.dp))  // clear the floating tab bar
            }
        }

        // Top-left corner: QR (overview) / Отмена (edit).
        Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 16.dp, top = 10.dp)) {
            Crossfade(targetState = editing, label = "leftBtn") { isEdit ->
                if (isEdit) {
                    CornerPill("Отмена") { nickname = profile.nickname; age = profile.age; editing = false }
                } else {
                    QrThumb { showQr = true }
                }
            }
        }

        // Top-right corner: Изм. (overview) / Готово (edit).
        Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(end = 16.dp, top = 10.dp)) {
            Crossfade(targetState = editing, label = "rightBtn") { isEdit ->
                if (isEdit) {
                    CornerPill("Готово", bold = true) {
                        scope.launch { repo.setNickname(nickname); repo.setAge(age) }
                        editing = false
                    }
                } else {
                    CornerPill("Изм.") { editing = true }
                }
            }
        }
    }

    if (showQr) {
        QrDialog(
            content = profileUrl(userId),
            title = profile.nickname.ifEmpty { "Мой профиль" },
            onDismiss = { showQr = false },
        )
    }
}

/** Glass corner pill (Изм. / Отмена / Готово). */
@Composable
private fun CornerPill(label: String, bold: Boolean = false, onClick: () -> Unit) {
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
private fun QrThumb(onClick: () -> Unit) {
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
private fun Grouped(bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg),
        content = content,
    )
}

/** iOS/Telegram-style settings row: icon square + label + trailing control. */
@Composable
private fun SettingRow(
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

/** Labelled plain text field inside a group card. */
@Composable
private fun EditField(
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
private fun Separator(startInset: androidx.compose.ui.unit.Dp = 16.dp) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(start = startInset)
            .height(1.dp)
            .background(SEP)
    )
}

/** Enlarged QR for someone to scan, on a glass card. */
@Composable
private fun QrDialog(content: String, title: String, onDismiss: () -> Unit) {
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
