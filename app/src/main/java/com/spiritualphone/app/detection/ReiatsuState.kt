package com.spiritualphone.app.detection

/** Current state of reiatsu detection, surfaced to the UI. */
sealed interface ReiatsuState {

    /** Learning the calm baseline field at startup. */
    data object Calibrating : ReiatsuState

    /**
     * A live reading after calibration.
     *
     * @param deltaUt deviation from the calm baseline, in µT (the "reiatsu").
     * @param hollow  detected Hollow class, or null when the field is calm.
     */
    data class Reading(
        val deltaUt: Float,
        val hollow: HollowClass?,
    ) : ReiatsuState
}
