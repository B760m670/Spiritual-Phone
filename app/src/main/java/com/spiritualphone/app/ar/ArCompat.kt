package com.spiritualphone.app.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session

/**
 * GO/NO-GO probe for the ARCore Depth path: does this device support ARCore at
 * all, and the Depth API (occlusion) specifically? Used to decide whether to
 * build on ARCore before investing in the full GL render rewrite. Run off the
 * main thread — it may briefly create (and immediately close) a Session.
 */
object ArCompat {

    data class Report(
        val availability: String,
        val depthSupported: Boolean?,
        val note: String? = null,
    )

    fun check(context: Context): Report {
        val apk = ArCoreApk.getInstance()
        var availability = apk.checkAvailability(context)
        var tries = 0
        // checkAvailability can answer asynchronously the first time.
        while (availability == ArCoreApk.Availability.UNKNOWN_CHECKING && tries < 20) {
            Thread.sleep(100)
            availability = apk.checkAvailability(context)
            tries++
        }
        if (!availability.isSupported) {
            return Report(availability.name, null)
        }
        if (availability != ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            // Device is capable, but the ARCore runtime (Google Play Services for
            // AR) isn't installed yet — can't query Depth until it is.
            return Report(availability.name, null, "ARCore runtime not installed")
        }
        return try {
            val session = Session(context)
            val depth = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
            session.close()
            Report(availability.name, depth)
        } catch (t: Throwable) {
            Report(availability.name, null, t.javaClass.simpleName)
        }
    }
}
