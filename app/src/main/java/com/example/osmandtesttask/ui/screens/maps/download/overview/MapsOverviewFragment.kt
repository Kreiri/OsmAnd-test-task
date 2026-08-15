package com.example.osmandtesttask.ui.screens.maps.download.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.storage.StorageInfo
import com.example.osmandtesttask.ui.common.components.StorageInfoView
import com.example.osmandtesttask.ui.screens.maps.download.list.MapsListFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MapsOverviewFragment : Fragment() {
    val vm by viewModel<MapsOverviewViewModel>()

    private lateinit var storageView: StorageInfoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            vm.loadRegionsList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps_overview, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        storageView = view.findViewById(R.id.storage_info)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.regionsListState.collect { state ->
                    updateRegionsList(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.storageInfo.collect { state ->
                    updateStorageView(state)
                }
            }
        }
    }

    private fun updateStorageView(state: StorageInfo) {
        storageView.updateStorageInfo(state.totalBytes, state.availableBytes)
    }

    private fun updateRegionsList(state: RegionsListState) {
        when (state) {
            is RegionsListState.Failure -> {
                Toast.makeText(
                    requireContext(),
                    R.string.error_msg_maps_list_download_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }

            RegionsListState.Loading -> {

            }

            is RegionsListState.Success -> {
                val fragment = MapsListFragment.createForTopLevel()
                childFragmentManager.beginTransaction().replace(
                    R.id.child_fragment_container, fragment
                ).commit()
            }
        }
    }
}