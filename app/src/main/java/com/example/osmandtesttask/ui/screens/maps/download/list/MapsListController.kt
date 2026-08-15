package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.common.ProviderQualifiers
import com.example.osmandtesttask.domain.LocaleProvider
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.downloader.hasActive
import com.example.osmandtesttask.domain.downloader.hasFileForRegion
import com.example.osmandtesttask.domain.downloader.hasQueued
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionType
import com.example.osmandtesttask.domain.models.regionsComparator
import com.example.osmandtesttask.ui.common.components.MarginDividerItemDecoration
import com.example.osmandtesttask.ui.common.extensions.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.util.Locale

class MapsListController(
    private val adapter: MapsListAdapter,
) : KoinComponent {
    init {
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    private val localeProvider: LocaleProvider by inject(named(ProviderQualifiers.LOCALE))
    private val regionsComparator = regionsComparator(localeProvider)
    private var recyclerView: RecyclerView? = null

    fun attach(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        val context = recyclerView.context
        val divider = MarginDividerItemDecoration(
            1.dpToPx(context),
            ContextCompat.getColor(context, R.color.dividerColor),
            64.dpToPx(context), 0,
            alsoOmitItemIf = { adapter, position ->
                (adapter as? MapsListAdapter)?.shouldSkipDividerForItemAt(position) ?: false
            }
        )
        recyclerView.setHasFixedSize(true)
        recyclerView.addItemDecoration(divider)
        recyclerView.adapter = this.adapter
    }

    fun detach() {
        this.recyclerView?.adapter = null
        this.recyclerView = null
    }

    suspend fun setItems(
        indexPath: List<Int>,
        regions: List<Region>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>
    ) {
        withContext(Dispatchers.Default) {
            val items = buildItems(indexPath, regions, downloaderState, downloadedFiles)
            adapter.submitList(items)
        }
    }


    private fun buildItems(
        indexPath: List<Int>,
        regions: List<Region>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>
    ): List<MapListItem> {
        val list = mutableListOf<MapListItem>()
        val locale = localeProvider.invoke()
        regions.forEachIndexed { regionIndex, region ->
            val regionPath = indexPath + regionIndex
            if (region.type == RegionType.CONTINENT) {
                list.add(makeContinentHeaderItem(region, locale))
                val continentItems = region.regions.mapIndexed { childIndex, child ->
                    makeRegionItem(
                        child,
                        regionPath + childIndex,
                        downloaderState,
                        downloadedFiles,
                        locale
                    )
                }.sortedWith { item, item1 ->
                    regionsComparator.compare(item.region, item1.region)
                }
                list.addAll(continentItems)
                list.add(MapListItem.ContinentFooter(region))
            } else {
                val item =
                    makeRegionItem(region, regionPath, downloaderState, downloadedFiles, locale)
                list.add(item)
            }
        }
        return list.toList()
    }

    private fun makeContinentHeaderItem(
        region: Region,
        locale: Locale
    ): MapListItem.ContinentHeader {
        val displayedName = region.getLocalizedName(locale.language)
        return MapListItem.ContinentHeader(region, displayedName)
    }

    private fun makeRegionItem(
        region: Region,
        indexPath: List<Int>,
        downloaderState: DownloaderState,
        downloadedFiles: Set<DownloadedFileInfo>,
        locale: Locale
    ): MapListItem.RegionItem {
        val status: DownloadStatus
        val downloadProgress: Float
        if (downloaderState is DownloaderState.Processing) {
            if (downloaderState.hasActive(region)) {
                status = DownloadStatus.ACTIVE
                downloadProgress = when (val ds = downloaderState.downloadState) {
                    is DownloadState.Progress -> ds.progress
                    is DownloadState.Finished -> 1f
                    else -> 0f
                }
            } else {
                downloadProgress = 0f
                status = if (downloaderState.hasQueued(region)) {
                    DownloadStatus.ENQUEUED
                } else {
                    DownloadStatus.NONE
                }
            }
        } else {
            downloadProgress = 0f
            status = if (downloadedFiles.hasFileForRegion(region)) {
                DownloadStatus.DOWNLOADED
            } else {
                DownloadStatus.NONE
            }
        }

        val displayedName = region.getLocalizedName(locale.language)
        val hasMap = region.type == RegionType.MAP || region.map
        return MapListItem.RegionItem(
            region, indexPath, displayedName, status, downloadProgress, hasMap
        )
    }
}