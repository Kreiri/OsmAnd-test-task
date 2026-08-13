package com.example.osmandtesttask.ui.screens.maps.download.list

import com.example.osmandtesttask.domain.models.Region

sealed interface MapListItem {
    data class RegionItem(val region: Region, val indexPath: List<Int>) : MapListItem
    data class ContinentHeader(val region: Region) : MapListItem
    data class ContinentFooter(val region: Region) : MapListItem
}