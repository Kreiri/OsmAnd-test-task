package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.osmandtesttask.domain.MapsManager
import com.example.osmandtesttask.domain.downloader.DownloadedFileInfo
import com.example.osmandtesttask.domain.downloader.DownloaderState
import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch


sealed interface UIState {
    object Loading : UIState
    data class Success(
        val parentRegion: Region?,
        val regions: List<Region>,
        val downloaderState: DownloaderState,
        val downloadedFiles: Set<DownloadedFileInfo>
    ) : UIState
}

class MapsListViewModel(
    private val mapsManager: MapsManager
) : ViewModel() {

    var indexPath: List<Int> = emptyList()
        private set

    val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    val downloadErrors = mapsManager.downloadErrors.shareIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        replay = 0,
    ).buffer(capacity = Channel.BUFFERED)

    private val downloadsIndex = mapsManager.downloadsIndex

    init {
        subscribeToDownloadStateUpdates()
    }

    private fun subscribeToDownloadStateUpdates() {
        viewModelScope.launch {
            mapsManager.downloadsState
                .combine(downloadsIndex) { downloaderState, downloadsIndex ->
                    downloaderState to downloadsIndex
                }
                .collect { (downloaderState, downloadsIndex) ->
                    val currentState = _uiState.value
                    val newState = if (currentState is UIState.Success) {
                        currentState.copy(
                            downloaderState = downloaderState,
                            downloadedFiles = downloadsIndex.downloadedFiles
                        )
                    } else currentState
                    _uiState.value = newState
                }
        }
    }

    fun setIndexPath(path: List<Int>) {
        this.indexPath = path
        val data = mapsManager.getRegions()
        if (path.isEmpty()) {
            _uiState.value =
                UIState.Success(
                    parentRegion = null,
                    regions = data?.regions ?: emptyList(),
                    downloaderState = mapsManager.downloadsState.value,
                    downloadedFiles = downloadsIndex.value.downloadedFiles
                )
        } else {
            val parentRegion = data?.getRegionForIndexPath(path)
            val regions = parentRegion?.regions ?: emptyList()
            _uiState.value = UIState.Success(
                parentRegion = parentRegion,
                regions = regions,
                downloaderState = mapsManager.downloadsState.value,
                downloadedFiles = downloadsIndex.value.downloadedFiles
            )
        }
    }

    fun requestDownload(region: Region) {
        mapsManager.requestDownloadMap(region)
    }

    fun cancelDownload(region: Region) {
        mapsManager.cancelDownload(region)
    }
}