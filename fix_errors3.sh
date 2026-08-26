#!/bin/bash
set -e

sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/ui/navigation/AppNavigation.kt
sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/ui/explore/ExploreScreen.kt
sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/ui/favorites/FavoritesScreen.kt
sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/ui/premium/PremiumScreen.kt
sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt
sed -i '/import com.google.firebase.auth.FirebaseAuth/d' app/src/main/java/com/example/service/AdvancedWallpaperService.kt

sed -i 's/val auth: FirebaseAuth = AppContainer.auth//g' app/src/main/java/com/example/service/AdvancedWallpaperService.kt
sed -i 's/val auth = AppContainer.auth//g' app/src/main/java/com/example/ui/explore/ExploreScreen.kt
sed -i 's/val auth = AppContainer.auth//g' app/src/main/java/com/example/ui/favorites/FavoritesScreen.kt
sed -i 's/val auth = AppContainer.auth//g' app/src/main/java/com/example/ui/premium/PremiumScreen.kt
sed -i 's/val auth = AppContainer.auth//g' app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt

sed -i 's/val authRepository = AppContainer.authRepository//g' app/src/main/java/com/example/ui/navigation/AppNavigation.kt

echo "Fixing AppContainer.kt syntax..."
sed -i 's/val authRepository: AuthRepository get() = authRepositoryImpl/val authRepository: AuthRepository get() = authRepositoryImpl/g' app/src/main/java/com/example/di/AppContainer.kt

echo "Fixing MainActivity.kt unresolved references for bottom nav..."
sed -i 's/screen.selectedIcon/androidx.compose.material.icons.Icons.Default.Home/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/screen.unselectedIcon/androidx.compose.material.icons.Icons.Default.Home/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/screen.title/screen.name/g' app/src/main/java/com/example/MainActivity.kt

