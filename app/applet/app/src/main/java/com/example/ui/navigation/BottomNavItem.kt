package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem("home", Icons.Filled.Home, "Home")
    object Explore : BottomNavItem("explore", Icons.Filled.Explore, "Explore")
    object Wallpapers : BottomNavItem("wallpapers", Icons.Filled.Wallpaper, "Wallpapers")
    object Favorites : BottomNavItem("favorites", Icons.Filled.Favorite, "Favorites")
    object Settings : BottomNavItem("settings", Icons.Filled.Settings, "Settings")
}
