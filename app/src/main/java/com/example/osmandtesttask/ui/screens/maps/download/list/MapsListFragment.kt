package com.example.osmandtesttask.ui.screens.maps.download.list

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.osmandtesttask.R
import com.example.osmandtesttask.domain.models.Region
import com.example.osmandtesttask.ui.common.components.MarginDividerItemDecoration
import com.example.osmandtesttask.ui.common.extensions.dpToPx
import com.example.osmandtesttask.ui.common.extensions.getCurrentLocale
import com.example.osmandtesttask.ui.common.navigation.NavDestination
import com.example.osmandtesttask.ui.common.navigation.NavigationViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class MapsListFragment: Fragment() {
    val vm by viewModel<MapsListViewModel>()
    val navigationVm by activityViewModel<NavigationViewModel>()

    private lateinit var regionsRecycler: RecyclerView

    private val mapsListAdapter = MapsListAdapter(
        onRegionItemTapped = ::onRegionItemTapped,
        onDownloadTapped = ::onDownloadTapped
    )

    private var permissionContinuation: Continuation<Boolean>? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionContinuation?.resume(isGranted)
        permissionContinuation = null
    }

    suspend fun requestPermission(permission: String): Boolean =
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.uiState.collect { state ->
                    when (state) {
                        UIState.Loading -> {}
                        is UIState.Success -> {
                            val regions = state.regions
                            mapsListAdapter.setItems(vm.indexPath, regions)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecycler(view: View) {
        val context = view.context
        regionsRecycler = view.findViewById(R.id.regions_recycler)
        val divider = MarginDividerItemDecoration(
            1.dpToPx(context),
            ContextCompat.getColor( context, R.color.listDividerColor),
            64.dpToPx(context), 0,
            alsoOmitItemIf = { adapter, position ->
                (adapter as? MapsListAdapter)?.shouldSkipDividerForItemAt(position) ?: false
            }
        )
        regionsRecycler.addItemDecoration(divider)
        regionsRecycler.adapter = mapsListAdapter
    }

    private fun onDownloadTapped(indexPath: List<Int>, item: Region) {
        lifecycleScope.launch {
            if (requestPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                vm.requestDownload(item)
            }
        }
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
        fun createForPath(indexPath: List<Int>) : MapsListFragment {
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