package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Home : BottomNavItem("home", Icons.Filled.Home, Icons.Outlined.Home, "Home")
    object Explore : BottomNavItem("explore", Icons.Filled.GridView, Icons.Outlined.GridView, "Explore")
    object Favorites : BottomNavItem("favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "Favorites")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, Icons.Outlined.Person, "Profile")
}
