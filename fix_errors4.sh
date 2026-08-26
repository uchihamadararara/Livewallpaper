#!/bin/bash
set -e

sed -i 's/val authRepository: AuthRepository get() = authRepositoryImpl/val authRepository: AuthRepository get() = authRepositoryImpl/g' app/src/main/java/com/example/di/AppContainer.kt

# The AppContainer syntax error
sed -i '/val authRepository: AuthRepository get() = authRepositoryImpl/d' app/src/main/java/com/example/di/AppContainer.kt
sed -i '/val authRepositoryImpl:/a\    val authRepository: AuthRepository get() = authRepositoryImpl' app/src/main/java/com/example/di/AppContainer.kt

echo "Fixing BottomNavItem usage..."
sed -i 's/androidx.compose.material.icons.Icons.Default.Home/if (isSelected) screen.selectedIcon else screen.unselectedIcon/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/screen.name/screen.title/g' app/src/main/java/com/example/MainActivity.kt

echo "Fixing unresolved AppContainer auth references"
sed -i 's/AppContainer.auth.currentUser?.uid/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/service/AdvancedWallpaperService.kt
sed -i 's/AppContainer.auth.currentUser?.uid/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/explore/ExploreScreen.kt
sed -i 's/AppContainer.auth.currentUser?.uid/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/favorites/FavoritesScreen.kt
sed -i 's/AppContainer.auth.currentUser?.uid/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/premium/PremiumScreen.kt
sed -i 's/AppContainer.auth.currentUser?.uid/kotlinx.coroutines.runBlocking { (AppContainer.authRepository as com.example.data.repository.AuthRepositoryImpl).getUserId() }/g' app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt

echo "Done"
