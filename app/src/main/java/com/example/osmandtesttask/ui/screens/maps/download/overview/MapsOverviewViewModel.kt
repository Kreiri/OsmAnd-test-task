package com.example.osmandtesttask.ui.screens.maps.download.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.osmandtesttask.domain.MapsManager
import com.example.osmandtesttask.domain.models.RegionsList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UIState {
    object Loading: UIState
    data class Success(val regionsList: RegionsList): UIState
    data class Failure(val e: Throwable): UIState
}
class MapsOverviewViewModel(
    private val mapsManager: MapsManager
): ViewModel() {

    val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    fun loadRegionsList() {
        viewModelScope.launch {
            _uiState.value = UIState.Loading
            val result = mapsManager.loadRegions()
            result.onFailure {
                _uiState.value = UIState.Failure(it)
            }.onSuccess {
               _uiState.value = UIState.Success(it)
            }
        }
    }
}