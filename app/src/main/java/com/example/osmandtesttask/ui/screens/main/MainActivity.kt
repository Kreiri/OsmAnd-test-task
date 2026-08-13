package com.example.osmandtesttask.ui.screens.main

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.osmandtesttask.R
import com.example.osmandtesttask.ui.common.navigation.NavDestination
import com.example.osmandtesttask.ui.common.navigation.NavigationViewModel
import com.example.osmandtesttask.ui.screens.maps.download.list.MapsListFragment
import com.example.osmandtesttask.ui.screens.maps.download.overview.MapsOverviewFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val navViewModel: NavigationViewModel by viewModel()

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            navViewModel.setRootDestination(NavDestination.MapList)
            routeTo(NavDestination.MapList, addToBackStack = false)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val canGoBack = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.setDisplayHomeAsUpEnabled(canGoBack)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    navViewModel.navigateToScreen.collect { destination ->
                        routeTo(destination, addToBackStack = true)
                    }
                }
                launch {
                    navViewModel.backEvent.collect {
                        supportFragmentManager.popBackStack()
                        val screen = navViewModel.getTopDestination()
                        updateToolbarTitle(screen)
                    }
                }
            }
        }


    }

    override fun onSupportNavigateUp(): Boolean {
        navViewModel.navigateBack()
        return true
    }

    private fun routeTo(destination: NavDestination, addToBackStack: Boolean) {
        updateToolbarTitle(destination)

        val fragment = when(destination) {
            NavDestination.MapList -> MapsOverviewFragment()
            is NavDestination.MapListRegion -> MapsListFragment.createForPath(destination.path)
        }
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun updateToolbarTitle(destination: NavDestination) {
        val toolbarTitle = when (destination) {
            NavDestination.MapList -> getString(R.string.screen_title_downlaod_maps)
            is NavDestination.MapListRegion -> destination.title
        }
        supportActionBar?.title = toolbarTitle
    }
}