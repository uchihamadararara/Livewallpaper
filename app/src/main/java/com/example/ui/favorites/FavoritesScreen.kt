package com.example.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.ui.components.ErrorState
import com.example.ui.components.WallpaperCard
import com.example.ui.home.HomeViewModel
import com.example.ui.home.UiState
import com.example.ui.components.AdBanner

@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
        )
    )
    val favorites by viewModel.favoriteWallpapers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AdBanner(isPremium = isPremium)
        
        Text(
            text = "Collection",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        when (val state = uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is UiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )
            }
            is UiState.Success -> {
                if (favorites.isEmpty()) {
                     Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                         Text(
                             text = "Your collection is empty.\nSave your favorite artworks here.",
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             fontSize = 16.sp,
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
                         )
                     }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(favorites) { wallpaper ->
                            WallpaperCard(
                                wallpaper = wallpaper,
                                modifier = Modifier.aspectRatio(0.6f),
                                onClick = { navController.navigate("detail/${wallpaper.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}
