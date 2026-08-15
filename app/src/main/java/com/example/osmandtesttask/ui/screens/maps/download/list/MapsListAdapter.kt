package com.example.osmandtesttask.ui.screens.maps.download.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale

class MapsListAdapter(
    private val onRegionItemTapped: (itemPath: List<Int>, region: Region) -> Unit,
    private val onDownloadTapped: (itemPath: List<Int>, region: Region) -> Unit,
    private val onCancelDownloadTapped: (itemPath: List<Int>, region: Region) -> Unit,
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
                RegionItemViewHolder(view).also { holder ->
                    holder.itemView.setOnClickListener { handleItemClick(holder) }
                    holder.downloadButton.setOnClickListener { handleDownloadClick(holder) }
                    holder.cancelButton.setOnClickListener { handleCancelDownloadClick(holder) }
                }
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
                holder.bind(item, actualPayloads)
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

    private fun withBindingAdapterPosition(holder: ViewHolder, action: (position: Int) -> Unit) {
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return
        action(position)
    }

    private fun handleDownloadClick(holder: RegionItemViewHolder) {
        withBindingAdapterPosition(holder) { position ->
            val item = getItem(position)
            if (item is MapListItem.RegionItem) {
                onDownloadTapped(item.indexPath, item.region)
            }
        }
    }

    private fun handleCancelDownloadClick(holder: RegionItemViewHolder) {
        withBindingAdapterPosition(holder) { position ->
            val item = getItem(position)
            if (item is MapListItem.RegionItem) {
                onCancelDownloadTapped(item.indexPath, item.region)
            }
        }
    }

    private fun handleItemClick(holder: RegionItemViewHolder) {
        withBindingAdapterPosition(holder) { position ->
            val item = getItem(position)
            if (item is MapListItem.RegionItem) {
                onRegionItemTapped(item.indexPath, item.region)
            }
        }
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
        val progressBar: ProgressBar = view.findViewById(R.id.progressIndicator)

        fun bind(
            item: MapListItem.RegionItem,
            payloads: Set<PayloadFlags>
        ) {
            val shouldUpdateAll = payloads.isEmpty()
            val progressChanged = payloads.contains(PayloadFlags.DOWNLOAD_PROGRESS)
            val statusChanged = payloads.contains(PayloadFlags.STATUS)

            if (shouldUpdateAll) {
                regionName.text = item.displayedName

                /*
                * NB: Doesn't seem right to show map icon for region that doesn't have a map,
                * but design mockup did show it with the map icon.
                * */
                iconView.setImageResource(R.drawable.ic_map)
            }

            if (shouldUpdateAll || statusChanged) {
                val context = itemView.context
                val isDownloadable = item.hasMap
                val isActiveOrEnqueued =
                    item.status == DownloadStatus.ACTIVE || item.status == DownloadStatus.ENQUEUED
                val isDownloaded = item.status == DownloadStatus.DOWNLOADED
                downloadButton.visibility =
                    if (isDownloadable && !isActiveOrEnqueued) View.VISIBLE else View.GONE
                cancelButton.visibility =
                    if (isDownloadable && isActiveOrEnqueued) View.VISIBLE else View.GONE
                progressBar.visibility = if (isActiveOrEnqueued) View.VISIBLE else View.GONE
                val tintColor = if (isDownloaded) {
                    ContextCompat.getColor(context, R.color.resourceDownloadedIconColor)
                } else {
                    ContextCompat.getColor(context, R.color.regionsListIconTintColor)
                }
                iconView.setColorFilter(tintColor)
            }

            if (shouldUpdateAll || progressChanged) {
                progressBar.progress = (item.downloadProgress * 100).toInt()
            }
        }
    }
}