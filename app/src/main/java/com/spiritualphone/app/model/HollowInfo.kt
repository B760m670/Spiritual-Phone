package com.spiritualphone.app.model

/**
 * Descriptive data shown in the Hollow details sheet. Stubs for now — real
 * Hollow races, artwork, spiritual power and numbers come in a later milestone.
 */
data class HollowInfo(
    val race: String = "Пустой",          // раса (заглушка)
    val count: Int = 1,                    // численность
    val spiritualPower: String = "Неизвестно", // духовная сила (заглушка)
)
