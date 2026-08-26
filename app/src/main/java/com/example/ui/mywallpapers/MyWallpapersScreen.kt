package com.example.ui.mywallpapers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domain.models.Wallpaper
import com.example.domain.repository.WallpaperRepository
import com.example.ui.components.WallpaperCard
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.example.di.AppContainer
import androidx.compose.ui.platform.LocalContext

class MyWallpapersViewModel(private val repository: WallpaperRepository) : ViewModel() {
    val favoriteWallpapers = repository.getFavoriteWallpapers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

@Composable
fun MyWallpapersScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = AppContainer.getWallpaperRepository(context)
    val viewModel = remember { MyWallpapersViewModel(repository) }
    val favorites by viewModel.favoriteWallpapers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "My Wallpapers",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Favorites",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        if (favorites.isEmpty()) {
            Text("No favorites yet.", color = MaterialTheme.colorScheme.secondary)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites) { wp ->
                    WallpaperCard(
                        wallpaper = wp,
                        onClick = { navController.navigate("detail/${wp.id}") }
                    )
                }
            }
        }
    }
}
