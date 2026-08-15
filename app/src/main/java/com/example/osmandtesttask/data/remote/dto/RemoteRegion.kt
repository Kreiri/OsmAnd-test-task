package com.example.osmandtesttask.data.remote.dto

import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionType


class RemoteRegion(
    val name: String,
    val translate: String?,
    val downloadSuffix: String?,
    val innerDownloadSuffix: String?,
    val downloadPrefix: String?,
    val innerDownloadPrefix: String?,
    val type: String?,
    val map: Boolean?,
    val regions: List<RemoteRegion>

) {

    private fun resolveString(value: String?): String? {
        if (value == "\$name") return this.name
        return value
    }

    fun getAffixes(): DownloadAffixes {
        val downloadSuffix = resolveString(downloadSuffix)
        val innerDownloadSuffix = resolveString(innerDownloadSuffix)
        val downloadPrefix = resolveString(downloadPrefix)
        val innerDownloadPrefix = resolveString(innerDownloadPrefix)
        return DownloadAffixes(
            downloadSuffix, innerDownloadSuffix, downloadPrefix, innerDownloadPrefix
        )
    }

    fun toLocal(parentAffixes: DownloadAffixes? = null, childRegions: List<Region>): Region {
        val mergedAffixes =
            if (parentAffixes != null) mergeDownloadAffixes(parentAffixes) else getAffixes()
        val downloadName = composeDownloadName(mergedAffixes)
        val type = this.type?.parseAsType()
        val isMap = if (type != null && type != RegionType.MAP) {
            false
        } else {
            this.map ?: true
        }
        return Region(
            name = this.name,
            downloadName = downloadName,
            type = type,
            map = isMap,
            translate = this.translate,
            regions = childRegions
        )
    }


    fun mergeDownloadAffixes(parentAffixes: DownloadAffixes?): DownloadAffixes {
        val affixes = getAffixes()
        val downloadSuffix = parentAffixes?.innerDownloadSuffix
            ?: affixes.downloadSuffix ?: parentAffixes?.downloadSuffix
        val downloadPrefix = parentAffixes?.innerDownloadPrefix
            ?: affixes.downloadPrefix ?: parentAffixes?.downloadPrefix
        val innerDownloadSuffix = affixes.innerDownloadSuffix
        val innerDownloadPrefix = affixes.innerDownloadPrefix
        return DownloadAffixes(
            downloadSuffix,
            innerDownloadSuffix,
            downloadPrefix,
            innerDownloadPrefix
        )
    }

    fun composeDownloadName(mergedAffixes: DownloadAffixes): String {
        return listOfNotNull(
            mergedAffixes.downloadPrefix,
            this.name,
            mergedAffixes.downloadSuffix
        ).joinToString("_")
    }


}

data class DownloadAffixes(
    val downloadSuffix: String?,
    val innerDownloadSuffix: String?,
    val downloadPrefix: String?,
    val innerDownloadPrefix: String?,
) {

}

private fun String.parseAsType(): RegionType {
    return when (this) {
        "continent" -> RegionType.CONTINENT
        "map" -> RegionType.MAP
        "srtm" -> RegionType.SRTM
        "hillshade" -> RegionType.HILLSHADE
        else -> RegionType.UNKNOWN
    }
}
