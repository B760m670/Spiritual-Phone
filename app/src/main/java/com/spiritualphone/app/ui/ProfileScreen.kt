package com.spiritualphone.app.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/** Deep link encoded in the profile QR; resolves to the web profile once the
 *  backend exists, and works as an app deep link in the meantime. */
private fun profileUrl(userId: String?) = "https://spiritualphone.app/u/${userId ?: "anon"}"

private val BG = Color(0xFF0B0E14)
private val ACCENT = Color(0xFFE53935)
private val TEXT = Color(0xFFEDEDED)
private val MUTED = Color(0xFF8A90A0)

/** Dark profile + settings screen: nickname, age, avatar, notifications. */
@Composable
fun ProfileScreen(
    repo: ProfileRepository,
    onClose: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by repo.profile.collectAsState(initial = UserProfile())

    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }
    var age by remember(profile.age) { mutableStateOf(profile.age) }

    // Stable local id for the QR; generated on first open.
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

    Surface(color = BG, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // QR of the user's profile link, in place of the old title.
                QrThumbButton(content = profileUrl(userId), onClick = { showQr = true })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Закрыть", tint = TEXT)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Avatar
            val avatar = remember(profile.avatarPath) {
                profile.avatarPath?.let {
                    runCatching { BitmapFactory.decodeFile(it) }.getOrNull()?.asImageBitmap()
                }
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (avatar != null) {
                    Image(
                        bitmap = avatar,
                        contentDescription = "Аватар",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MUTED,
                        modifier = Modifier.size(110.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(onClick = { pickAvatar.launch("image/*") }) {
                    Text(if (profile.avatarPath == null) "Поставить аватар" else "Изменить")
                }
                if (profile.avatarPath != null) {
                    Spacer(Modifier.size(8.dp))
                    TextButton(onClick = { scope.launch { repo.setAvatarPath(null) } }) {
                        Text("Удалить", color = ACCENT)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Никнейм") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = age,
                onValueChange = { input -> age = input.filter { it.isDigit() }.take(3) },
                label = { Text("Возраст") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { scope.launch { repo.setNickname(nickname); repo.setAge(age) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сохранить") }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFF22283A))
            Spacer(Modifier.height(16.dp))

            Text("Настройки", color = TEXT, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Уведомления", color = TEXT, modifier = Modifier.weight(1f))
                Switch(
                    checked = profile.notificationsEnabled,
                    onCheckedChange = { scope.launch { repo.setNotificationsEnabled(it) } },
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenLogs) { Text("Логи (отладка)", color = MUTED) }
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

/** Small glass tile showing the profile QR; tap to enlarge. */
@Composable
private fun QrThumbButton(content: String, onClick: () -> Unit) {
    val qr = rememberQrBitmap(content, 256)
    Box(
        Modifier
            .size(52.dp)
            .glass(shape = RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (qr != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(3.dp),
            ) {
                Image(bitmap = qr, contentDescription = "QR профиля", modifier = Modifier.fillMaxSize())
            }
        } else {
            Icon(Icons.Filled.QrCode2, contentDescription = "QR профиля", tint = TEXT, modifier = Modifier.size(28.dp))
        }
    }
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
            Text("Покажите код, чтобы поделиться профилем", color = MUTED, fontSize = 12.sp)
        }
    }
}
