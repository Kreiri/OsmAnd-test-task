package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.recyclerview.widget.DiffUtil
import com.example.osmandtesttask.domain.models.Region
import java.util.EnumSet

sealed interface MapListItem {
    val key: String

    data class RegionItem(
        val region: Region,
        val indexPath: List<Int>,
        val isDownloading: Boolean,
        val downloadProgress: Float,
        val isDownloaded: Boolean
    ) : MapListItem {
        override val key: String = "r_${region.downloadName}"
    }

    data class ContinentHeader(val region: Region) : MapListItem {
        override val key: String = "ch_${region.downloadName}"
    }

    data class ContinentFooter(val region: Region) : MapListItem {
        override val key: String = "ce_${region.downloadName}"
    }
}

enum class PayloadFlags {
    IS_DOWNLOADING, DOWNLOAD_PROGRESS, IS_DOWNLOADED
}

data class Payloads(val flags: Set<PayloadFlags>)

object MapListItemDiff : DiffUtil.ItemCallback<MapListItem>() {
    override fun areItemsTheSame(oldItem: MapListItem, newItem: MapListItem): Boolean {
        return oldItem.key == newItem.key
    }

    override fun areContentsTheSame(oldItem: MapListItem, newItem: MapListItem): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: MapListItem, newItem: MapListItem): Any? {
        if (oldItem is MapListItem.RegionItem && newItem is MapListItem.RegionItem) {
            if (oldItem.key == newItem.key) {
                val flags = EnumSet.noneOf(PayloadFlags::class.java)
                if (oldItem.isDownloading != newItem.isDownloading) flags.add(PayloadFlags.IS_DOWNLOADING)
                if (oldItem.downloadProgress != newItem.downloadProgress) flags.add(PayloadFlags.DOWNLOAD_PROGRESS)
                if (oldItem.isDownloaded != newItem.isDownloaded) flags.add(PayloadFlags.IS_DOWNLOADED)
                return if (flags.isEmpty()) null else Payloads(flags)
            }
        }
        return super.getChangePayload(oldItem, newItem)
    }
}