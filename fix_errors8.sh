#!/bin/bash
set -e

# Fix AppNavigation syntax error and missing userId
cat << 'INNER_EOF' > app/src/main/java/com/example/ui/navigation/AppNavigation.kt
package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.explore.ExploreScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.wallpapers.WallpapersScreen

@Composable
fun AppNavigation(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(BottomNavItem.Explore.route) {
            ExploreScreen(navController = navController)
        }
        composable(BottomNavItem.Wallpapers.route) {
            WallpapersScreen(navController = navController)
        }
        composable(BottomNavItem.Favorites.route) {
            FavoritesScreen(navController = navController)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen()
        }
    }
}
INNER_EOF

echo "Done"
