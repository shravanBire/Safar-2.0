package com.example.safar20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safar20.ui.screens.home.HomeScreen
import com.example.safar20.ui.screens.settings.SettingScreen
import com.example.safar20.ui.screens.trips.TripsScreen
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SafarTheme
import com.example.safar20.viewModels.SafarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafarAppContent()
        }
    }
}

sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Cockpit : NavItem("home", "Cockpit", Icons.Default.Speed)
    object Logbook : NavItem("trips", "Logbook", Icons.Default.Map)
    object Settings : NavItem("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun SafarAppContent() {
    val context = LocalContext.current
    val safarViewModel: SafarViewModel = viewModel(factory = SafarViewModel.Factory(context))

    val isDarkTheme by safarViewModel.isDarkTheme.collectAsState()
    var selectedScreenIndex by remember { mutableIntStateOf(0) }

    val navItems = listOf(NavItem.Cockpit, NavItem.Logbook, NavItem.Settings)

    SafarTheme(darkTheme = isDarkTheme) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = GunmetalGauge,
                    contentColor = RoyalBrass
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedScreenIndex == index,
                            onClick = { selectedScreenIndex = index },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RoyalBrass,
                                selectedTextColor = RoyalBrass,
                                indicatorColor = BronzeRim,
                                unselectedIconColor = MutedPewter,
                                unselectedTextColor = MutedPewter
                            )
                        )
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
    }
}
