package com.example.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.explore.ExploreScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.premium.PremiumScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.wallpapers.WallpaperDetailScreen
import kotlinx.coroutines.runBlocking

@Composable
fun AppNavigation(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
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
        composable("categories") {
            ExploreScreen(navController = navController)
        }
        composable("explore") {
            ExploreScreen(navController = navController)
        }
        composable("live") {
            ExploreScreen(navController = navController)
        }
        composable(BottomNavItem.Favorites.route) {
            FavoritesScreen(navController = navController)
        }
        composable("favorites") {
            FavoritesScreen(navController = navController)
        }
        composable(BottomNavItem.Profile.route) {
            SettingsScreen(navController = navController)
        }
        composable("profile") {
            SettingsScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("premium") {
            PremiumScreen(navController = navController)
        }
        composable(
            route = "detail/{wallpaperId}",
            arguments = listOf(
                navArgument("wallpaperId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val wallpaperId = backStackEntry.arguments?.getString("wallpaperId") ?: ""
            WallpaperDetailScreen(wallpaperId = wallpaperId, navController = navController)
        }
    }
}
