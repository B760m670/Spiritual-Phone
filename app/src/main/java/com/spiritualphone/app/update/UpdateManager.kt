package com.spiritualphone.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.spiritualphone.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Metadata about a newer build available on GitHub Releases. */
data class UpdateInfo(
    val versionCode: Int,
    val title: String,
    val apkUrl: String,
    val notes: String,
    /** Expected SHA-256 of the APK (hex), as reported by GitHub. May be null. */
    val sha256: String?,
)

/**
 * In-app updater. Reads the latest GitHub Release of this repo, compares its
 * build number (tag `build-<n>`) with the installed [BuildConfig.VERSION_CODE],
 * and — if newer — downloads the APK and launches the system installer.
 *
 * All builds are signed with the same key (see app/spiritual.keystore), so the
 * update installs cleanly over the current one.
 */
object UpdateManager {
    private const val RELEASES_LATEST =
        "https://api.github.com/repos/B760m670/Spiritual-Phone/releases/latest"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(RELEASES_LATEST).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 15_000
                readTimeout = 15_000
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name") // e.g. "build-7"
                val remoteCode = tag.substringAfter("build-", "").toIntOrNull()
                    ?: return@runCatching null
                if (remoteCode <= BuildConfig.VERSION_CODE) return@runCatching null

                val assets = json.optJSONArray("assets") ?: return@runCatching null
                var apkUrl: String? = null
                var sha256: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        // GitHub reports e.g. "sha256:abc123…"; keep the hex part.
                        sha256 = asset.optString("digest")
                            .substringAfter("sha256:", "")
                            .lowercase()
                            .ifBlank { null }
                        break
                    }
                }
                val url = apkUrl ?: return@runCatching null
                UpdateInfo(
                    versionCode = remoteCode,
                    title = json.optString("name", tag),
                    apkUrl = url,
                    notes = json.optString("body", ""),
                    sha256 = sha256,
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun download(context: Context, info: UpdateInfo): File = withContext(Dispatchers.IO) {
        val dir = context.getExternalFilesDir(null) ?: context.cacheDir
        val out = File(dir, "update.apk")
        if (out.exists()) out.delete()
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 60_000
        }
        try {
            conn.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
        // Integrity check: refuse to install a file whose SHA-256 doesn't match
        // what GitHub published (guards against corruption / tampering).
        val expected = info.sha256
        if (expected != null) {
            val actual = sha256Of(out)
            if (!actual.equals(expected, ignoreCase = true)) {
                out.delete()
                throw SecurityException("APK integrity check failed (SHA-256 mismatch)")
            }
        }
        out
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun install(context: Context, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
