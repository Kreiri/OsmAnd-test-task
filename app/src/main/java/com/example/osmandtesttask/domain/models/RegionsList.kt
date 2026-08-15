package com.example.osmandtesttask.domain.models

data class RegionsList(val regions: List<Region>) {
    fun getRegionForIndexPath(indexPath: List<Int>): Region? {
        if (indexPath.isEmpty()) return null

        var current: Region = regions.getOrNull(indexPath[0]) ?: return null
        for (i in 1 until indexPath.size) {
            val crumb = indexPath[i]
            current = current.regions.getOrNull(crumb) ?: return null
        }
        return current
    }

    companion object {
        fun empty() = RegionsList(emptyList())
    }
}