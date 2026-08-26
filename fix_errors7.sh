#!/bin/bash
set -e

# Let's just fix MainActivity completely so it compiles
cat << 'INNER_EOF' > app/src/main/java/com/example/MainActivity.kt
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppNavigation
import com.example.ui.navigation.BottomNavItem
import com.example.ui.theme.MyApplicationTheme
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
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Explore,
        BottomNavItem.Wallpapers,
        BottomNavItem.Favorites,
        BottomNavItem.Settings
    )
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
                                imageVector = Icons.Default.Home, // simplify for compilation
                                contentDescription = screen.route
                            )
                        },
                        label = { Text(screen.route) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.tertiary,
                            selectedIconColor = MaterialTheme.colorScheme.onTertiary,
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
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
}
INNER_EOF

# Fix AppNavigation
sed -i '1i package com.example.ui.navigation\n\nimport com.example.di.AppContainer' app/src/main/java/com/example/ui/navigation/AppNavigation.kt

echo "Done"
