package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.domain.models.Wallpaper
import com.example.ui.components.ErrorState
import com.example.ui.components.WallpaperCard
import com.example.ui.components.AdBanner

@Composable
fun HomeScreen(navController: NavController, authUserId: String?) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = authUserId
        )
    )
    val featured by viewModel.featuredWallpapers.collectAsState()
    val trending by viewModel.trendingWallpapers.collectAsState()
    val new by viewModel.newWallpapers.collectAsState()
    val live by viewModel.liveWallpapers.collectAsState()
    val premium by viewModel.premiumWallpapers.collectAsState()
    
    val uiState by viewModel.uiState.collectAsState()
    val isPremiumUser by viewModel.isPremium.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AdBanner(isPremium = isPremiumUser)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Discover",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.5).sp
            )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 24.dp)
                ) {
                    if (featured.isNotEmpty()) {
                        SectionHeader("Featured Collections", "Curated for you")
                        WallpaperCarousel(wallpapers = featured, itemWidth = 260, itemHeight = 340, navController = navController)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    if (trending.isNotEmpty()) {
                        SectionHeader("Trending Now", "Popular this week")
                        WallpaperCarousel(wallpapers = trending, itemWidth = 150, itemHeight = 220, navController = navController)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    if (live.isNotEmpty()) {
                        SectionHeader("Live Wallpapers", "Bring your screen to life")
                        WallpaperCarousel(wallpapers = live, itemWidth = 150, itemHeight = 220, navController = navController)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    if (premium.isNotEmpty()) {
                        SectionHeader("Premium Exclusives", "High-fidelity artwork")
                        WallpaperCarousel(wallpapers = premium, itemWidth = 150, itemHeight = 220, navController = navController)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    if (new.isNotEmpty()) {
                        SectionHeader("Fresh Arrivals", "Just added")
                        WallpaperCarousel(wallpapers = new, itemWidth = 150, itemHeight = 220, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        TextButton(
            onClick = { /* TODO: See all */ },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(24.dp)
        ) {
            Text("See all", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
        }
    }
}

@Composable
fun WallpaperCarousel(wallpapers: List<Wallpaper>, itemWidth: Int, itemHeight: Int, navController: NavController) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(wallpapers) { wallpaper ->
            WallpaperCard(
                wallpaper = wallpaper,
                modifier = Modifier
                    .width(itemWidth.dp)
                    .height(itemHeight.dp),
                onClick = { navController.navigate("detail/${wallpaper.id}") }
            )
        }
    }
}
