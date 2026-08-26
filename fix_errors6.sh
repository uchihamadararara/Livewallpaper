#!/bin/bash
set -e

sed -i 's/androidx.compose.material.icons.Icons.Default.Home/if (isSelected) screen.selectedIcon else screen.unselectedIcon/g' app/src/main/java/com/example/MainActivity.kt

sed -i '/import com.example.ui.navigation.AppNavigation/a\import com.example.di.AppContainer' app/src/main/java/com/example/ui/navigation/AppNavigation.kt

echo "Done"
