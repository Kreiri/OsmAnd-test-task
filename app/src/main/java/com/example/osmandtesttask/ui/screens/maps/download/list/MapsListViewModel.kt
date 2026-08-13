package com.example.osmandtesttask.ui.screens.maps.download.list

import androidx.lifecycle.ViewModel
import com.example.osmandtesttask.domain.MapsManager
import com.example.osmandtesttask.domain.models.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


sealed interface UIState {
    object Loading: UIState
    data class Success(val parentRegion: Region?, val regions: List<Region>) : UIState
}

class MapsListViewModel(
    private val mapsManager: MapsManager
) : ViewModel() {

    var indexPath: List<Int> = emptyList()
    private set

    val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    fun setIndexPath(path: List<Int>) {
        this.indexPath = path
        val data = mapsManager.getRegions()
        if (path.isEmpty()) {
            _uiState.value = UIState.Success(null, data?.regions ?: emptyList())
        } else {
            val parentRegion = data?.getRegionForIndexPath(path)
            val regions = parentRegion?.regions ?: emptyList()
            _uiState.value = UIState.Success(parentRegion, regions)
        }
    }
}