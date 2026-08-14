package com.example.osmandtesttask.ui.screens.maps.download.list

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.downloader.DownloadState
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionType
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale

class MapsListAdapterOld(
    private val onRegionItemTapped: (itemPath: List<Int>, item: Region) -> Unit,
    private val onDownloadTapped: (itemPath: List<Int>, item: Region) -> Unit,
) : RecyclerView.Adapter<MapsListAdapterOld.ViewHolder>() {

    private var items = listOf<MapListItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(indexPath: List<Int>, regions: List<Region>, downloaderState: DownloaderState) {
        items = buildItems(indexPath, regions, downloaderState)
        notifyDataSetChanged()
    }

    fun shouldSkipDividerForItemAt(position: Int): Boolean {
        val item = items[position]
        return (item !is MapListItem.RegionItem)
    }

    private fun buildItems(indexPath: List<Int>, regions: List<Region>, downloaderState: DownloaderState): List<MapListItem> {
        val list = mutableListOf<MapListItem>()
        regions.forEachIndexed { regionIndex, region ->
            val regionPath = indexPath + regionIndex
            if (region.type == RegionType.CONTINENT) {
                list.add(MapListItem.ContinentHeader(region))
                val continentItems = region.regions.mapIndexed { childIndex, child ->
//                    MapListItem.RegionItem(child, regionPath + childIndex)
                    makeRegionItem(child, regionPath+childIndex, downloaderState)
                }
                list.addAll(continentItems)
                list.add(MapListItem.ContinentFooter(region))
            } else {
                val item = makeRegionItem(region, regionPath, downloaderState)
                list.add(item)
            }
        }
        return list.toList()
    }

    private fun makeRegionItem(region: Region, indexPath: List<Int>, downloaderState: DownloaderState) : MapListItem.RegionItem{
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
        return MapListItem.RegionItem(region, indexPath, isDownloading, progress)
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        return when(item) {
            is MapListItem.ContinentFooter -> ViewType.ContinentFooter.ordinal
            is MapListItem.ContinentHeader -> ViewType.ContinentHeader.ordinal
            is MapListItem.RegionItem -> ViewType.RegionItem.ordinal
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val type = ViewType.entries[viewType]
        return when(type) {
            ViewType.RegionItem -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_maps_list_region, parent, false)
                RegionItemViewHolder(view)
            }
            ViewType.ContinentHeader -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_maps_list_continent_header, parent, false)
                ContinentHeaderViewHolder(view)
            }
            ViewType.ContinentFooter -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_maps_list_continent_footer, parent, false)
                ContinentFooterViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        when(holder) {
            is RegionItemViewHolder -> {
                val item = items[position] as? MapListItem.RegionItem ?: return
                holder.bind(item, onRegionItemTapped, onDownloadTapped)
            }
            is ContinentHeaderViewHolder -> {
                val item = items[position] as? MapListItem.ContinentHeader ?: return
                holder.bind(item)
            }
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    private enum class ViewType {
        RegionItem, ContinentHeader, ContinentFooter;
    }
    abstract class ViewHolder(view: View): RecyclerView.ViewHolder(view)
    class ContinentHeaderViewHolder(view: View): ViewHolder(view) {
        val regionName: TextView = view.findViewById(R.id.region_name)
        fun bind(item: MapListItem.ContinentHeader) {
            val context = itemView.context
            val locale = context.getCurrentLocale()
            val region = item.region
            regionName.text = region.getLocalizedName(locale.language)
        }
    }
    class ContinentFooterViewHolder(view: View): ViewHolder(view)
    class RegionItemViewHolder(view: View) : ViewHolder(view) {
        val regionName: TextView = view.findViewById(R.id.region_name)
        val downloadButton: Button = view.findViewById(R.id.download_button)
        val iconView : ImageView = view.findViewById(R.id.icon_view)

        fun bind(
            item: MapListItem.RegionItem,
            onItemTapped: (path: List<Int>, region: Region) -> Unit,
            onDownloadTapped: (path: List<Int>, region: Region) -> Unit
        ) {
            val context = itemView.context
            val locale = context.getCurrentLocale()
            val region = item.region
            val showDownloadButton = region.map
            val icon = if(region.type == RegionType.MAP || region.map) {
                R.drawable.ic_map
            } else {
                0
            }
            val showIconView = (icon != 0)

            iconView.visibility = if (showIconView) View.VISIBLE else View.INVISIBLE
            iconView.setImageResource(icon)


            downloadButton.visibility = if (showDownloadButton) View.VISIBLE else View.INVISIBLE
            downloadButton.setOnClickListener {
                onDownloadTapped(item.indexPath, region)
            }

            regionName.text = region.getLocalizedName(locale.language)
            itemView.setOnClickListener {
                onItemTapped(item.indexPath, region)
            }
        }
    }
}