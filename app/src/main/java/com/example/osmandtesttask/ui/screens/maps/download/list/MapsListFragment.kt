package com.example.osmandtesttask.ui.screens.maps.download.list

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.downloader.MapDownloadError
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale
import com.example.osmandtesttask.ui.common.extensions.toReadableFileSize
import com.example.osmandtesttask.ui.common.navigation.NavDestination
import com.example.osmandtesttask.ui.common.navigation.NavigationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MapsListFragment : Fragment() {
    val vm by viewModel<MapsListViewModel>()
    val navigationVm by activityViewModel<NavigationViewModel>()

    private val mapsListController = MapsListController(
        MapsListAdapter(
            onRegionItemTapped = { indexPath, region -> onRegionItemTapped(indexPath, region) },
            onDownloadTapped = { _, region -> onDownloadTapped(region) },
            onCancelDownloadTapped = { _, region -> onCancelDownloadTapped(region) }
        )
    )

    private var permissionContinuation: Continuation<Boolean>? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionContinuation?.resume(isGranted)
        permissionContinuation = null
    }

    private suspend fun requestPermission(permission: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            permissionContinuation = continuation
            continuation.invokeOnCancellation {
                permissionContinuation = null
            }
            requestPermissionLauncher.launch(permission)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val indexPath = arguments?.getIntArray(ARG_INDEX_PATH)?.toList() ?: emptyList()
            vm.setIndexPath(indexPath)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps_list, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler(view)

        subscribeToDownloadErrors()
        subscribeToUiState()
    }

    override fun onDestroyView() {
        mapsListController.detach()
        super.onDestroyView()
    }

    private fun subscribeToDownloadErrors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.downloadErrors.collect { error ->
                    val downloadName = error.download.displayName
                    val message = when (error) {
                        is MapDownloadError.CancelledByUser -> getString(
                            R.string.error_msg_map_download_cancelled,
                            downloadName
                        )
                        is MapDownloadError.InsufficientStorage -> {
                            val context = requireContext()
                            val requiredBytes = error.requiredBytes.toReadableFileSize(context)
                            val availableBytes = error.availableBytes.toReadableFileSize(context)
                            getString(
                                R.string.error_msg_map_download_insufficient_storage,
                                downloadName, requiredBytes, availableBytes
                            )
                        }

                        else -> getString(R.string.error_msg_map_download_failed, downloadName)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun subscribeToUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state ->
                    when (state) {
                        UIState.Loading -> {}

                        is UIState.Success -> {
                            mapsListController.setItems(
                                vm.indexPath,
                                state.regions,
                                state.downloaderState,
                                state.downloadedFiles
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupRecycler(view: View) {
        val regionsRecycler: RecyclerView = view.findViewById(R.id.regions_recycler)
        mapsListController.attach(regionsRecycler)
    }

    private fun onDownloadTapped(item: Region) {
        lifecycleScope.launch {
            if (requestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                vm.requestDownload(item)
            }
        }
    }


    private fun onCancelDownloadTapped(item: Region) {
        vm.cancelDownload(item)
    }


    private fun onRegionItemTapped(indexPath: List<Int>, item: Region) {
        if (item.regions.isEmpty()) return
        val context = context ?: return
        val title = item.getLocalizedName(context.getCurrentLocale().language)
        val destination = NavDestination.MapListRegion(indexPath, title)
        navigationVm.navigateTo(destination)
    }

    companion object {
        private const val ARG_INDEX_PATH = "index_path"
        fun createForPath(indexPath: List<Int>): MapsListFragment {
            val args = Bundle().apply {
                putIntArray(ARG_INDEX_PATH, indexPath.toIntArray())
            }
            return MapsListFragment().also {
                it.arguments = args
            }
        }

        fun createForTopLevel() = MapsListFragment()
    }
}