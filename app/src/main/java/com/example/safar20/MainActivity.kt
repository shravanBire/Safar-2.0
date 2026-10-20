package com.example.safar20

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safar20.ui.components.IgnitionSplash
import com.example.safar20.ui.screens.home.HomeScreen
import com.example.safar20.ui.screens.settings.SettingScreen
import com.example.safar20.ui.screens.trips.TripsScreen
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.Rajdhani
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
                                .background(BronzeRim)
                        )
                        NavigationBar(
                            containerColor = GunmetalGauge,
                            contentColor = RoyalBrass
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
                                                        .background(RoyalBrass, shape = RoundedCornerShape(1.5.dp))
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
                                        selectedIconColor = RoyalBrass,
                                        selectedTextColor = RoyalBrass,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = MutedPewter,
                                        unselectedTextColor = MutedPewter
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
