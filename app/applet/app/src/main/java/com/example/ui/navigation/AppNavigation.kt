package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.home.HomeScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

@Composable
fun AppNavigation(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen()
        }
        composable(BottomNavItem.Explore.route) {
            PlaceholderScreen("Explore Screen Placeholder")
        }
        composable(BottomNavItem.Wallpapers.route) {
            PlaceholderScreen("Wallpapers Screen Placeholder")
        }
        composable(BottomNavItem.Favorites.route) {
            PlaceholderScreen("Favorites Screen Placeholder")
        }
        composable(BottomNavItem.Settings.route) {
            PlaceholderScreen("Settings Screen Placeholder")
        }
    }
}

@Composable
fun PlaceholderScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
