package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.ui.charging.ChargingAnimationOverlay
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.BottomNavItem
import com.example.ui.theme.MyApplicationTheme
import com.example.util.rememberBatteryChargingState
import com.google.android.gms.ads.MobileAds
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val userPrefs = AppContainer.getUserPreferencesRepository(context)
    val isChargingAnimationEnabled by userPrefs.isChargingAnimationEnabled.collectAsState(initial = true)
    val batteryChargingState by rememberBatteryChargingState()

    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Explore,
        BottomNavItem.Wallpapers,
        BottomNavItem.Favorites,
        BottomNavItem.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.route
                                )
                            },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            AppNavigation(navController = navController, paddingValues = paddingValues)
        }

        // Automatic Luxury Charging Animation Overlay
        ChargingAnimationOverlay(
            chargingState = batteryChargingState,
            isEnabled = isChargingAnimationEnabled
        )
    }
}
