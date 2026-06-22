package com.spiritualphone.app.audio

import android.content.Context
import android.media.MediaPlayer
import com.spiritualphone.app.R
import com.spiritualphone.app.debug.DebugLog

/**
 * Plays short event sounds. The Hollow alert clip lives at
 * res/raw/hollow_spawn — replace that file with the real Bleach-style sound
 * (keep the name `hollow_spawn`, any common audio format).
 *
 * Currently res/raw/hollow_spawn is a silent placeholder, so nothing is heard
 * until the real clip is dropped in.
 */
object SoundManager {

    fun playSpawn(context: Context) {
        runCatching {
            MediaPlayer.create(context, R.raw.hollow_spawn)?.apply {
                setOnCompletionListener { it.release() }
                start()
            } ?: DebugLog.log("Sound: hollow_spawn could not be created")
        }.onFailure { DebugLog.log("Sound: error ${it.message}") }
    }
}
