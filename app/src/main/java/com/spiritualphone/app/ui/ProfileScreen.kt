package com.spiritualphone.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Deep link encoded in the profile QR. */
private fun profileUrl(userId: String?) = "https://spiritualphone.app/u/${userId ?: "anon"}"

/** Pushed sub-screens of the profile. */
private enum class ProfileSub { Username, About, Privacy, AppLock }

/**
 * Profile section, SpiritChat-style. One screen with two cross-faded modes
 * (Overview / Edit) plus pushed sub-screens. The avatar is rendered outside the
 * cross-faded content, so switching modes leaves it perfectly still.
 *
 * Shared visuals live in ProfileTheme.kt / ProfileComponents.kt; each
 * sub-screen has its own file (UsernameScreen, AboutScreen, PrivacyScreen,
 * AppLockScreen).
 */
@Composable
fun ProfileScreen(repo: ProfileRepository, onOpenLogs: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState(initial = UserProfile())

    var editing by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf<ProfileSub?>(null) }
    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }
    var age by remember(profile.age) { mutableStateOf(profile.age) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }

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
                        if (editing) {
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
                            Separator()
                            EditField("О себе", bio, { bio = it.take(200) }, "Несколько слов о себе")
                        }
                    } else {
                        Grouped(ROW) {
                            SettingRow(
                                Icons.Filled.AlternateEmail, "Имя пользователя",
                                onClick = { sub = ProfileSub.Username },
                            ) {
                                Text(
                                    if (profile.username.isBlank()) "Не задан" else "@${profile.username}",
                                    color = SUBTLE, fontSize = 15.sp,
                                )
                                Icon(Icons.Filled.ChevronRight, null, tint = SUBTLE, modifier = Modifier.size(18.dp))
                            }
                            Separator(startInset = 52.dp)
                            SettingRow(Icons.Filled.Notifications, "Уведомления") {
                                Switch(
                                    checked = profile.notificationsEnabled,
                                    onCheckedChange = { scope.launch { repo.setNotificationsEnabled(it) } },
                                )
                            }
                            Separator(startInset = 52.dp)
                            SettingRow(
                                Icons.Filled.Lock, "Конфиденциальность",
                                onClick = { sub = ProfileSub.Privacy },
                            ) {
                                Icon(Icons.Filled.ChevronRight, null, tint = SUBTLE, modifier = Modifier.size(18.dp))
                            }
                            Separator(startInset = 52.dp)
                            SettingRow(
                                Icons.Filled.Info, "О приложении",
                                onClick = { sub = ProfileSub.About },
                            ) {
                                Icon(Icons.Filled.ChevronRight, null, tint = SUBTLE, modifier = Modifier.size(18.dp))
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
                    CornerPill("Отмена") {
                        nickname = profile.nickname; age = profile.age; bio = profile.bio; editing = false
                    }
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
                        scope.launch { repo.setNickname(nickname); repo.setAge(age); repo.setBio(bio) }
                        editing = false
                    }
                } else {
                    CornerPill("Изм.") { editing = true }
                }
            }
        }

        // Pushed sub-screens (slide in from the right over the profile).
        AnimatedVisibility(
            visible = sub == ProfileSub.Username,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            UsernameScreen(
                initial = profile.username,
                onBack = { sub = null },
                onSave = { v -> scope.launch { repo.setUsername(v) }; sub = null },
            )
        }
        AnimatedVisibility(
            visible = sub == ProfileSub.About,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            AboutScreen(onBack = { sub = null })
        }
        AnimatedVisibility(
            visible = sub == ProfileSub.Privacy,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            PrivacyScreen(
                lockEnabled = profile.appLockHash != null,
                onBack = { sub = null },
                onOpenAppLock = { sub = ProfileSub.AppLock },
            )
        }
        AnimatedVisibility(
            visible = sub == ProfileSub.AppLock,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        ) {
            AppLockScreen(repo = repo, currentHash = profile.appLockHash, onBack = { sub = ProfileSub.Privacy })
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
