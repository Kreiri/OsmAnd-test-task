package com.example.osmandtesttask.ui.screens.maps.download.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.domain.models.RegionType
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale
import com.google.android.material.progressindicator.LinearProgressIndicator

class MapsListAdapter(
    private val onRegionItemTapped: (itemPath: List<Int>, item: Region) -> Unit,
    private val onDownloadTapped: (itemPath: List<Int>, item: Region) -> Unit,
    private val onCancelDownloadTapped: (itemPath: List<Int>, item: Region) -> Unit,
) : ListAdapter<MapListItem, MapsListAdapter.ViewHolder>(MapListItemDiff) {


    fun shouldSkipDividerForItemAt(position: Int): Boolean {
        val item = getItem(position)
        return (item !is MapListItem.RegionItem)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when (item) {
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
        return when (type) {
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

    private fun bindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: List<Any?> = emptyList()
    ) {
        val actualPayloads: Set<PayloadFlags> =
            payloads.filterIsInstance<Payloads>().flatMap { it.flags }.toSet()
        when (holder) {
            is RegionItemViewHolder -> {
                val item = getItem(position) as? MapListItem.RegionItem ?: return
                holder.bind(
                    item,
                    actualPayloads,
                    onRegionItemTapped,
                    onDownloadTapped,
                    onCancelDownloadTapped
                )
            }

            is ContinentHeaderViewHolder -> {
                val item = getItem(position) as? MapListItem.ContinentHeader ?: return
                holder.bind(item)
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: List<Any?>
    ) {
        bindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        bindViewHolder(holder, position, emptyList())
    }

    private enum class ViewType {
        RegionItem, ContinentHeader, ContinentFooter;
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
    class ContinentHeaderViewHolder(view: View) : ViewHolder(view) {
        val regionName: TextView = view.findViewById(R.id.region_name)
        fun bind(item: MapListItem.ContinentHeader) {
            val context = itemView.context
            val locale = context.getCurrentLocale()
            val region = item.region
            regionName.text = region.getLocalizedName(locale.language)
        }
    }

    class ContinentFooterViewHolder(view: View) : ViewHolder(view)
    class RegionItemViewHolder(view: View) : ViewHolder(view) {
        val regionName: TextView = view.findViewById(R.id.region_name)
        val downloadButton: Button = view.findViewById(R.id.download_button)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val iconView: ImageView = view.findViewById(R.id.icon_view)
        val progressBar: LinearProgressIndicator = view.findViewById(R.id.progressIndicator)

        fun bind(
            item: MapListItem.RegionItem,
            payloads: Set<PayloadFlags>,
            onItemTapped: (path: List<Int>, region: Region) -> Unit,
            onDownloadTapped: (path: List<Int>, region: Region) -> Unit,
            onCancelDownloadTapped: (path: List<Int>, region: Region) -> Unit,
        ) {
            val region = item.region

            val shouldUpdateAll = payloads.isEmpty()
            val progressChanged =
                shouldUpdateAll || payloads.contains(PayloadFlags.DOWNLOAD_PROGRESS)
            val isDownloadingChanged =
                shouldUpdateAll || payloads.contains(PayloadFlags.IS_DOWNLOADING)
            val isDownloadedChanged =
                shouldUpdateAll || payloads.contains(PayloadFlags.IS_DOWNLOADED)


            if (shouldUpdateAll) {

                val context = itemView.context
                val locale = context.getCurrentLocale()
                regionName.text = region.getLocalizedName(locale.language)
                itemView.setOnClickListener {
                    onItemTapped(item.indexPath, region)
                }

                val icon = if (region.type == RegionType.MAP || region.map) {
                    R.drawable.ic_map
                } else {
                    0
                }
                val showIconView = (icon != 0)

                iconView.visibility = if (showIconView) View.VISIBLE else View.INVISIBLE
                iconView.setImageResource(icon)

                downloadButton.setOnClickListener {
                    onDownloadTapped(item.indexPath, region)
                }
                cancelButton.setOnClickListener {
                    onCancelDownloadTapped(item.indexPath, region)
                }
            }

            if (shouldUpdateAll || isDownloadingChanged) {
                val isDownloadable = region.map
                val isDownloading = item.isDownloading
                downloadButton.visibility =
                    if (isDownloadable && !isDownloading) View.VISIBLE else View.GONE
                cancelButton.visibility =
                    if (isDownloadable && isDownloading) View.VISIBLE else View.GONE
                progressBar.visibility = if (isDownloading) View.VISIBLE else View.GONE
            }

            if (shouldUpdateAll || progressChanged) {
                progressBar.progress = (item.downloadProgress * 100).toInt()
            }
            if (shouldUpdateAll || isDownloadedChanged) {
                val context = itemView.context
                val tintColor = if (item.isDownloaded) {
                    ContextCompat.getColor(context, R.color.downloadedIconColor)
                } else {
                    ContextCompat.getColor(context, R.color.listItemIconTintColor)
                }
                iconView.setColorFilter(tintColor)
            }
        }
    }
}