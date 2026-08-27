package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.domain.models.Wallpaper
import com.example.ui.components.AppLogoIcon
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.SearchField
import com.example.ui.components.WallpaperCard
import com.example.ui.components.WallpaperCarouselSkeleton
import com.example.ui.components.WallpaperSortOrder
import com.example.ui.components.WallpaperTypeFilter
import com.example.ui.theme.ChampagnePrimary
import kotlinx.coroutines.launch

enum class HomeTab {
    TRENDING,
    LIVE,
    DOUBLE,
    NEW
}

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

    val allWallpapers by viewModel.allWallpapers.collectAsState()
    val trendingWallpapers by viewModel.trendingWallpapers.collectAsState()
    val liveWallpapers by viewModel.liveWallpapers.collectAsState()
    val doubleWallpapers by viewModel.doubleWallpapers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPremiumUser by viewModel.isPremium.collectAsState()

    var selectedTab by remember { mutableStateOf(HomeTab.TRENDING) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Filter & Sort States
    var currentSort by remember { mutableStateOf(WallpaperSortOrder.POPULAR) }
    var currentTypeFilter by remember { mutableStateOf(WallpaperTypeFilter.ALL) }
    var onlyVip by remember { mutableStateOf(false) }

    // Drawer state for hamburger menu
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF090D14),
                drawerContentColor = Color.White
            ) {
                Spacer(Modifier.height(24.dp))
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppLogoIcon(size = 44.dp)
                    Column {
                        Text(
                            text = "Live Wallpaper",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPremiumUser) "VIP Member" else "Free Explorer",
                            fontSize = 12.sp,
                            color = if (isPremiumUser) Color(0xFF60A5FA) else Color(0xFF94A3B8)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Home Feed", color = Color.White) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF121B2B),
                        unselectedContainerColor = Color.Transparent
                    )
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Explore & Categories", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("explore")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEC4899)) },
                    label = { Text("Favorites", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("favorites")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("VIP Studio", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("premium")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray) },
                    label = { Text("Account & Settings", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B10))
        ) {
            // 1. TOP HEADER: Hamburger Menu | "W Live Wallpaper" | VIP Pill Badge | User Avatar
            TopLiveWallpaperHeader(
                isPremium = isPremiumUser,
                onMenuClick = { scope.launch { drawerState.open() } },
                onVipClick = { navController.navigate("premium") },
                onProfileClick = { navController.navigate("profile") }
            )

            // 2. SEARCH BAR with Filter / Tune Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search anime, cars, nature, live...",
                    onFilterClick = { showFilterSheet = true }
                )
            }

            // 3. HORIZONTAL TAB SELECTOR: Trending | Live | Double | New
            HomeTopTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Quick Category Shortcuts Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shortcuts = listOf(
                    "🌸 Anime" to "anime",
                    "🌿 Nature" to "nature",
                    "🚗 Supercars" to "cars",
                    "🎮 Gaming" to "gaming",
                    "✨ AMOLED" to "amoled",
                    "🌀 Abstract" to "abstract",
                    "🌌 Space" to "space"
                )
                items(shortcuts) { (label, tag) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF101622))
                            .border(1.dp, Color(0xFF1E2A3C), RoundedCornerShape(14.dp))
                            .clickable {
                                navController.navigate("explore")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Dynamic Data Computation based on Tab, Search, Filters & Sort
            val displayedWallpapers = remember(
                allWallpapers,
                trendingWallpapers,
                liveWallpapers,
                doubleWallpapers,
                selectedTab,
                searchQuery,
                currentSort,
                currentTypeFilter,
                onlyVip
            ) {
                // Step 1: Base list by active tab
                val baseList = when (selectedTab) {
                    HomeTab.TRENDING -> if (trendingWallpapers.isNotEmpty()) trendingWallpapers else allWallpapers
                    HomeTab.LIVE -> if (liveWallpapers.isNotEmpty()) liveWallpapers else allWallpapers.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }
                    HomeTab.DOUBLE -> if (doubleWallpapers.isNotEmpty()) doubleWallpapers else allWallpapers.filter { it.hasHomeTransition || it.hasLockAnimation || it.hasChargingAnimation }
                    HomeTab.NEW -> allWallpapers.sortedByDescending { it.createdAt }
                }

                // Step 2: Search Query filtering
                val searchFiltered = if (searchQuery.isBlank()) {
                    baseList
                } else {
                    val q = searchQuery.trim().lowercase()
                    allWallpapers.filter { wp ->
                        wp.title.lowercase().contains(q) ||
                        (wp.description?.lowercase()?.contains(q) == true) ||
                        wp.type.lowercase().contains(q) ||
                        wp.categoryIds.any { it.lowercase().contains(q) }
                    }
                }

                // Step 3: Type Filter Sheet
                val typeFiltered = when (currentTypeFilter) {
                    WallpaperTypeFilter.ALL -> searchFiltered
                    WallpaperTypeFilter.LIVE_ONLY -> searchFiltered.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }
                    WallpaperTypeFilter.STATIC_ONLY -> searchFiltered.filter { it.type == "STATIC" }
                    WallpaperTypeFilter.DOUBLE_ONLY -> searchFiltered.filter { it.hasHomeTransition || it.hasLockAnimation }
                }

                // Step 4: VIP Filter
                val vipFiltered = if (onlyVip) typeFiltered.filter { it.isPremium } else typeFiltered

                // Step 5: Sorting
                when (currentSort) {
                    WallpaperSortOrder.POPULAR -> vipFiltered.sortedByDescending { it.isTrending }
                    WallpaperSortOrder.LATEST -> vipFiltered.sortedByDescending { it.createdAt }
                    WallpaperSortOrder.ALPHABETICAL -> vipFiltered.sortedBy { it.title }
                }
            }

            // 4. MAIN CONTENT AREA (2-Column Grid Layout)
            when (val state = uiState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WallpaperCarouselSkeleton(itemWidth = 160.dp, itemHeight = 240.dp, count = 2)
                        WallpaperCarouselSkeleton(itemWidth = 160.dp, itemHeight = 240.dp, count = 2)
                    }
                }

                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is UiState.Success -> {
                    if (displayedWallpapers.isEmpty()) {
                        EmptyState(
                            title = if (searchQuery.isNotBlank()) "No Matching Wallpapers" else "No Wallpapers Available",
                            message = if (searchQuery.isNotBlank()) "Try searching for a different keyword or reset filters." else "No wallpapers found in this category right now.",
                            icon = Icons.Default.SearchOff,
                            actionText = if (searchQuery.isNotBlank()) "Clear Search" else "Refresh",
                            onActionClick = {
                                if (searchQuery.isNotBlank()) searchQuery = "" else viewModel.refresh()
                            }
                        )
                    } else {
                        val gridState = rememberLazyGridState()

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedWallpapers, key = { it.id }) { wp ->
                                WallpaperCard(
                                    wallpaper = wp,
                                    showTitleOverlay = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.62f),
                                    onFavoriteToggle = {
                                        viewModel.toggleFavorite(wp.id, wp.isFavorite)
                                    },
                                    onClick = {
                                        navController.navigate("detail/${wp.id}")
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter & Sort Bottom Sheet
        if (showFilterSheet) {
            FilterBottomSheet(
                currentSort = currentSort,
                currentTypeFilter = currentTypeFilter,
                onlyPremium = onlyVip,
                onSortChange = { currentSort = it },
                onTypeFilterChange = { currentTypeFilter = it },
                onPremiumFilterChange = { onlyVip = it },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

/**
 * 1. Top Header: Hamburger | "W Live Wallpaper" | VIP Pill Badge | Gradient User Avatar
 */
@Composable
fun TopLiveWallpaperHeader(
    isPremium: Boolean,
    onMenuClick: () -> Unit,
    onVipClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Side: Hamburger & Brand Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF101622))
                    .border(0.5.dp, Color(0xFF1E2A3C), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer Menu",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Circular App Logo Icon
            AppLogoIcon(size = 32.dp)

            Text(
                text = "Live Wallpaper",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        // Right Side: VIP Button & Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // VIP Pill Badge Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        )
                    )
                    .clickable(onClick = onVipClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isPremium) "VIP PRO" else "VIP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stylized Avatar
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF101622))
                    .border(1.2.dp, Color(0xFF2563EB).copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 3. Horizontal Tab Selector: Trending | Live | Double | New
 */
@Composable
fun HomeTopTabSelector(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101622))
            .border(0.8.dp, Color(0xFF1E2A3C), RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            HomeTab.TRENDING to ("🔥 Trending"),
            HomeTab.LIVE to ("🔴 Live"),
            HomeTab.DOUBLE to ("📱 Double"),
            HomeTab.NEW to ("✨ New")
        ).forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                        ) else Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
