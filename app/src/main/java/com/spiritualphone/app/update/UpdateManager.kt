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

/** Metadata about a newer build available on GitHub Releases. */
data class UpdateInfo(
    val versionCode: Int,
    val title: String,
    val apkUrl: String,
    val notes: String,
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
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.optString("name").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
                val url = apkUrl ?: return@runCatching null
                UpdateInfo(
                    versionCode = remoteCode,
                    title = json.optString("name", tag),
                    apkUrl = url,
                    notes = json.optString("body", ""),
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
        out
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
