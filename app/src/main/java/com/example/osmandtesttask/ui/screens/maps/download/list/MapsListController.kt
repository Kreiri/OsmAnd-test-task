package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionType
import com.example.osmandtesttask.ui.common.components.MarginDividerItemDecoration
import com.example.osmandtesttask.ui.common.extensions.dpToPx

class MapsListController(
    val adapter: MapsListAdapter
) {
    init {
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    fun setupRecycler(recyclerView: RecyclerView) {
        val context = recyclerView.context
        val divider = MarginDividerItemDecoration(
            1.dpToPx(context),
            ContextCompat.getColor( context, R.color.dividerColor),
            64.dpToPx(context), 0,
            alsoOmitItemIf = { adapter, position ->
                (adapter as? MapsListAdapter)?.shouldSkipDividerForItemAt(position) ?: false
            }
        )
        recyclerView.addItemDecoration(divider)
        recyclerView.adapter = this.adapter
    }

    fun setItems(
        indexPath: List<Int>,
        regions: List<Region>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>
    ) {
        val items = buildItems(indexPath, regions, downloaderState, downloadedFiles)
        adapter.submitList(items)
    }

    private fun buildItems(
        indexPath: List<Int>,
        regions: List<Region>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>
    ): List<MapListItem> {
        val list = mutableListOf<MapListItem>()
        regions.forEachIndexed { regionIndex, region ->
            val regionPath = indexPath + regionIndex
            if (region.type == RegionType.CONTINENT) {
                list.add(MapListItem.ContinentHeader(region))
                val continentItems = region.regions.mapIndexed { childIndex, child ->
                    makeRegionItem(child, regionPath+childIndex, downloaderState, downloadedFiles)
                }
                list.addAll(continentItems)
                list.add(MapListItem.ContinentFooter(region))
            } else {
                val item = makeRegionItem(region, regionPath, downloaderState, downloadedFiles)
                list.add(item)
            }
        }
        return list.toList()
    }
    private fun makeRegionItem(
        region: Region,
        indexPath: List<Int>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>
    ) : MapListItem.RegionItem{
        var isDownloading = false
        var progress = 0f
        if (downloaderState is DownloaderState.Processing) {
            isDownloading = (downloaderState.activeDownload.downloadName == region.downloadName)
            if (isDownloading) {
                val downloadState = downloaderState.downloadState
                if (downloadState is DownloadState.Progress) {
                    progress = downloadState.progress
                } else if (downloadState is DownloadState.Finished) {
                    progress = 1f
                }
            }
        }

        val isDownloaded = downloadedFiles.any { it.downloadName == region.downloadName }

        return MapListItem.RegionItem(region, indexPath, isDownloading, progress, isDownloaded)
    }
}