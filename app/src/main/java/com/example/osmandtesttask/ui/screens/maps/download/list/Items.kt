package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.recyclerview.widget.DiffUtil
import com.example.osmandtesttask.domain.models.Region
import java.util.EnumSet

sealed interface MapListItem {
    val key: String

    data class RegionItem(
        val region: Region,
        val indexPath: List<Int>,
        val displayedName: String,
        val status: DownloadStatus,
        val downloadProgress: Float,
        val hasMap: Boolean,
    ) : MapListItem {
        override val key: String = "r_${region.downloadName}"
    }

    data class ContinentHeader(
        val region: Region,
        val displayedName: String,
    ) : MapListItem {
        override val key: String = "ch_${region.downloadName}"
    }

    data class ContinentFooter(val region: Region) : MapListItem {
        override val key: String = "ce_${region.downloadName}"
    }
}

enum class DownloadStatus {
    ENQUEUED, ACTIVE, DOWNLOADED, NONE
}

enum class PayloadFlags {
    STATUS, DOWNLOAD_PROGRESS
}

data class Payloads(val flags: Set<PayloadFlags>)

object MapListItemDiff : DiffUtil.ItemCallback<MapListItem>() {
    override fun areItemsTheSame(oldItem: MapListItem, newItem: MapListItem): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: MapListItem, newItem: MapListItem): Boolean {
        if (oldItem::class != newItem::class) return false
        return when(oldItem) {
            is MapListItem.ContinentFooter -> true
            is MapListItem.ContinentHeader -> {
                newItem as MapListItem.ContinentHeader
                oldItem.displayedName == newItem.displayedName
            }
            is MapListItem.RegionItem -> {
                newItem as MapListItem.RegionItem
                oldItem.displayedName == newItem.displayedName
                        && oldItem.status == newItem.status
                        && oldItem.downloadProgress == newItem.downloadProgress
                        && oldItem.hasMap == newItem.hasMap
            }
        }

    }

    override fun getChangePayload(oldItem: MapListItem, newItem: MapListItem): Any? {
        if (oldItem is MapListItem.RegionItem && newItem is MapListItem.RegionItem) {
            if (oldItem.key == newItem.key) {
                val flags = EnumSet.noneOf(PayloadFlags::class.java)
                if (oldItem.status != newItem.status) flags.add(PayloadFlags.STATUS)
                if (oldItem.downloadProgress != newItem.downloadProgress) flags.add(PayloadFlags.DOWNLOAD_PROGRESS)
                return if (flags.isEmpty()) null else Payloads(flags)
            }
        }
        return super.getChangePayload(oldItem, newItem)
    }
}