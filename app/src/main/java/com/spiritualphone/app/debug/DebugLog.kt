package com.spiritualphone.app.debug

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiny in-app log buffer surfaced by the debug panel, so issues (like location)
 * can be diagnosed on-device and copied out. Also mirrors to Logcat.
 */
object DebugLog {
    private const val MAX_LINES = 300
    private val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    val lines = mutableStateListOf<String>()

    fun log(message: String) {
        val line = "[${formatter.format(Date())}] $message"
        lines.add(line)
        while (lines.size > MAX_LINES) lines.removeAt(0)
        Log.d("SpiritualPhone", message)
    }

    fun dump(): String = lines.joinToString("\n")

    fun clear() = lines.clear()
}
