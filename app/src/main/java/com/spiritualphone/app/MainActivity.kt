package com.spiritualphone.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.spiritualphone.app.data.ProfileRepository
import com.spiritualphone.app.data.UserProfile
import com.spiritualphone.app.debug.DebugLog
import com.spiritualphone.app.map.SpiritRadarMap
import com.spiritualphone.app.ui.AppTab
import com.spiritualphone.app.ui.ArScreen
import com.spiritualphone.app.ui.BottomTabBar
import com.spiritualphone.app.ui.DebugPanel
import com.spiritualphone.app.ui.LockScreen
import com.spiritualphone.app.ui.ProfileScreen
import com.spiritualphone.app.update.UpdateInfo
import com.spiritualphone.app.update.UpdateManager
import kotlinx.coroutines.launch

/**
 * Single entry point of the Spiritual Phone.
 *
 * Milestone 1: schematic street map + the player's own GPS position.
 * In-app updates (GitHub Releases) are wired in here too, so new builds
 * install without re-downloading the APK by hand.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = SpiritColors) {
                SpiritualPhoneApp()
            }
        }
    }
}

/** Monochrome dark palette (no blue, no red) — SpiritChat-inspired neutral tones. */
private val SpiritColors = darkColorScheme(
    primary = Color(0xFFEDEDED),      // near-white accent
    onPrimary = Color(0xFF0A0C10),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
)

@Composable
private fun SpiritualPhoneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun hasLocation() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasLocation()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // --- In-app update check on launch ---
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        update = UpdateManager.checkForUpdate()
    }

    // Ask for notification permission (Android 13+) so Hollow alerts can show.
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result ignored: notifications are optional */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val profileRepo = remember { ProfileRepository(context) }
    val profile by profileRepo.profile.collectAsState(initial = UserProfile())
    var tab by remember { mutableStateOf(AppTab.Map) }
    var showDebug by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }
    LaunchedEffect(locationGranted) {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        DebugLog.log("Permissions: fine=$fine coarse=$coarse")
    }

    Box(Modifier.fillMaxSize()) {
        if (locationGranted) {
            SpiritRadarMap(Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        )
                    )
                }) {
                    Text(context.getString(R.string.grant_permission))
                }
            }
        }

        update?.let { info ->
            AlertDialog(
                onDismissRequest = { if (!downloading) update = null },
                title = { Text(context.getString(R.string.update_title)) },
                text = {
                    Text(
                        if (downloading) context.getString(R.string.update_downloading)
                        else "${info.title}\n\n${info.notes}".trim()
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !downloading,
                        onClick = {
                            downloading = true
                            scope.launch {
                                val result = runCatching {
                                    val apk = UpdateManager.download(context, info)
                                    UpdateManager.install(context, apk)
                                }
                                downloading = false
                                update = null
                                if (result.isFailure) {
                                    Toast.makeText(
                                        context, R.string.update_failed, Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) { Text(context.getString(R.string.update_now)) }
                },
                dismissButton = {
                    TextButton(
                        enabled = !downloading,
                        onClick = { update = null }
                    ) { Text(context.getString(R.string.update_later)) }
                }
            )
        }

        // Profile section (covers the map when active), cross-faded.
        AnimatedVisibility(visible = tab == AppTab.Profile, enter = fadeIn(), exit = fadeOut()) {
            ProfileScreen(
                repo = profileRepo,
                onOpenLogs = { showDebug = true },
            )
        }

        // AR section, cross-faded.
        AnimatedVisibility(visible = tab == AppTab.Ar, enter = fadeIn(), exit = fadeOut()) {
            ArScreen(Modifier.fillMaxSize())
        }

        BottomTabBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (showDebug) {
            DebugPanel(onClose = { showDebug = false }, modifier = Modifier.fillMaxSize())
        }

        // App-lock gate: covers everything until the PIN is entered.
        val lockHash = profile.appLockHash
        if (lockHash != null && !unlocked) {
            LockScreen(repo = profileRepo, expectedHash = lockHash, onUnlock = { unlocked = true })
        }
    }
}
