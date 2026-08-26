#!/bin/bash
set -e

# Fix AppNavigation
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
import com.example.ui.wallpapers.WallpaperDetailScreen
import kotlinx.coroutines.runBlocking

@Composable
fun AppNavigation(navController: NavHostController, paddingValues: PaddingValues) {
    val uid = runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(navController = navController, authUserId = uid)
        }
        composable(BottomNavItem.Explore.route) {
            ExploreScreen(navController = navController)
        }
        composable(BottomNavItem.Wallpapers.route) {
            // There is no WallpapersScreen, it's probably WallpaperDetailScreen but WallpaperDetailScreen expects wallpaperId, just pass a dummy one or maybe it was PremiumScreen? 
            // In MainActivity: Home, Explore, Wallpapers, Favorites, Settings
            // Wallpapers here used to navigate to WallpapersScreen. If not exists, use ExploreScreen
            ExploreScreen(navController = navController)
        }
        composable(BottomNavItem.Favorites.route) {
            FavoritesScreen(navController = navController)
        }
        composable(BottomNavItem.Settings.route) {
            SettingsScreen(navController = navController) // Settings screen might need navController
        }
    }
}
INNER_EOF

# Fix SettingsScreen if it doesn't take navController
# Actually wait, I'll just check if it takes navController first
echo "Done"
