package com.example.osmandtesttask.ui.screens.maps.download.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.osmandtesttask.domain.MapsManager
import com.example.osmandtesttask.domain.errors.AppError
import com.example.osmandtesttask.domain.models.onFailure
import com.example.osmandtesttask.domain.models.onSuccess
import com.example.osmandtesttask.domain.storage.StorageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RegionsListState {
    object Loading: RegionsListState
    object Success: RegionsListState
    data class Failure(val error: AppError): RegionsListState
}
class MapsOverviewViewModel(
    private val mapsManager: MapsManager
): ViewModel() {

    private val _regionsListState = MutableStateFlow<RegionsListState>(RegionsListState.Loading)
    val regionsListState = _regionsListState.asStateFlow()
    val storageInfo = mapsManager.storageInfo

    fun loadRegionsList() {
        viewModelScope.launch {
            _regionsListState.value = RegionsListState.Loading
            val result = mapsManager.loadRegions()
            result.onFailure {
                _regionsListState.value = RegionsListState.Failure(it)
            }.onSuccess {
               _regionsListState.value = RegionsListState.Success
            }
        }
    }
}