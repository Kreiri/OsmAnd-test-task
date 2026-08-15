package com.example.osmandtesttask.ui.common.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface NavDestination {
    data class MapListRegion(val path: List<Int>, val title: String) : NavDestination
    object MapList : NavDestination
}

class NavigationViewModel : ViewModel() {

    private val _navigateToScreen = MutableSharedFlow<NavDestination>()
    val navigateToScreen = _navigateToScreen.asSharedFlow()

    private val _backEvent = MutableSharedFlow<Unit>()
    val backEvent = _backEvent.asSharedFlow()

    private val screenStack = mutableListOf<NavDestination>()

    fun setRootDestination(destination: NavDestination) {
        screenStack.add(0, destination)
    }

    fun getTopDestination(): NavDestination {
        return screenStack.last()
    }

    fun navigateTo(destination: NavDestination) {
        viewModelScope.launch {
            screenStack.add(destination)
            _navigateToScreen.emit(destination)
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            viewModelScope.launch {
                _backEvent.emit(Unit)
            }
            return true
        }
        return false
    }
}