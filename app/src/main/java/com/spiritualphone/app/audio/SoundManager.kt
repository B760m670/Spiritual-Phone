package com.spiritualphone.app.audio

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.spiritualphone.app.R
import com.spiritualphone.app.debug.DebugLog

/**
 * Plays short event sounds from res/raw. Replace the placeholder clips with the
 * real Bleach-style audio (keep the names):
 *  - hollow_spawn — appears within the alert radius
 *  - radar_alarm  — radar caught more than the danger threshold of objects
 */
object SoundManager {

    fun playSpawn(context: Context) = play(context, R.raw.hollow_spawn, "hollow_spawn")

    fun playAlarm(context: Context) = play(context, R.raw.radar_alarm, "radar_alarm")

    private fun play(context: Context, @RawRes res: Int, name: String) {
        runCatching {
            MediaPlayer.create(context, res)?.apply {
                setOnCompletionListener { it.release() }
                start()
            } ?: DebugLog.log("Sound: $name could not be created")
        }.onFailure { DebugLog.log("Sound: $name error ${it.message}") }
    }
}
