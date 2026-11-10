package com.example.safar20

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.example.safar20.ui.components.IgnitionSplash
import com.example.safar20.ui.screens.home.HomeScreen
import com.example.safar20.ui.screens.settings.SettingScreen
import com.example.safar20.ui.screens.trips.TripsScreen
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.SafarTheme
import com.example.safar20.viewModels.SafarViewModel

class MainActivity : ComponentActivity() {

    private var viewModel: SafarViewModel? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val vm = ViewModelProvider(this, SafarViewModel.Factory(applicationContext))[SafarViewModel::class.java]
        viewModel = vm

        registerNetworkCallback(vm)
        setupProcessLifecycleObserver(vm)

        setContent {
            SafarAppContent(safarViewModel = vm)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel?.setAppForeground(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel?.setAppForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
    }

    private fun setupProcessLifecycleObserver(vm: SafarViewModel) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                vm.setAppForeground(true)
            }

            override fun onStop(owner: LifecycleOwner) {
                vm.setAppForeground(false)
            }
        })
    }

    private fun registerNetworkCallback(vm: SafarViewModel) {
        try {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager = cm
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    vm.setNetworkAvailable(true)
                }

                override fun onLost(network: Network) {
                    vm.setNetworkAvailable(false)
                }
            }
            networkCallback = callback
            cm?.registerDefaultNetworkCallback(callback)

            @Suppress("DEPRECATION")
            val activeNet = cm?.activeNetworkInfo
            @Suppress("DEPRECATION")
            vm.setNetworkAvailable(activeNet?.isConnected == true)
        } catch (_: Exception) {
            vm.setNetworkAvailable(true)
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {
        }
    }
}

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Cockpit : NavItem("home", "Cockpit", Icons.Default.Speed)
    object Logbook : NavItem("trips", "Logbook", Icons.Default.Map)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun SafarAppContent(safarViewModel: SafarViewModel) {
    val isDarkTheme by safarViewModel.isDarkTheme.collectAsState()
    var selectedScreenIndex by remember { mutableIntStateOf(0) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val navItems = listOf(NavItem.Cockpit, NavItem.Logbook, NavItem.Settings)

    SafarTheme(darkTheme = isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedScreenIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedScreenIndex = index },
                                    icon = {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(18.dp)
                                                        .height(3.dp)
                                                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(1.5.dp))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                            } else {
                                                Spacer(modifier = Modifier.height(7.dp))
                                            }
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = item.title.uppercase(),
                                            fontFamily = Rajdhani,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 1.sp
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (selectedScreenIndex) {
                        0 -> HomeScreen(viewModel = safarViewModel)
                        1 -> TripsScreen(viewModel = safarViewModel)
                        2 -> SettingScreen(viewModel = safarViewModel)
                    }
                }
            }

            if (showSplash) {
                IgnitionSplash(onFinished = { showSplash = false })
            }
        }
    }
}
