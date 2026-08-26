package com.example.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

val categories = listOf("All", "Live", "Premium", "Anime", "Nature", "Abstract", "Cars")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
        )
    )
    val allWallpapers by viewModel.allWallpapers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AdBanner(isPremium = isPremium)
        
        Text(
            text = "Explore",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search wallpapers...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { selectedCategory = category }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }

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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    var filtered = allWallpapers
                    if (searchQuery.isNotBlank()) {
                        filtered = filtered.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    }
                    if (selectedCategory != "All") {
                        filtered = when (selectedCategory) {
                            "Live" -> filtered.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }
                            "Premium" -> filtered.filter { it.isPremium }
                            else -> filtered.filter { 
                                it.title.contains(selectedCategory, ignoreCase = true) || 
                                (it.description?.contains(selectedCategory, ignoreCase = true) == true)
                            }
                        }
                    }

                    items(filtered) { wallpaper ->
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
