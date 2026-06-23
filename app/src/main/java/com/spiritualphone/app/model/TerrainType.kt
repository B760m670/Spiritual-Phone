package com.spiritualphone.app.model

/**
 * The kind of physical environment at a Hollow's current coordinates.
 * Derived from OSM tags via Overpass API. Controls how the Hollow moves:
 * it avoids water, hunts souls in residential areas, wanders in forests.
 */
enum class TerrainType(val ruName: String) {
    OPEN("открытое пространство"),
    RESIDENTIAL("жилой квартал"),
    FOREST("лес"),
    WATER("вода"),
    INDUSTRIAL("промзона"),
}
