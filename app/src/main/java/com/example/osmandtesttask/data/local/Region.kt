package com.example.osmandtesttask.data.local

data class Region(
    val name: String,
    val downloadName: String,
    val type: RegionType?,
    val map: Boolean,
    val translate: String?,
    val regions: List<Region>
) {
}

enum class RegionType {
    MAP, CONTINENT, SRTM, HILLSHADE, UNKNOWN
}