package com.example.ui.explore

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.home.HomeViewModel
import com.example.ui.home.UiState
import com.example.ui.theme.ChampagnePrimary
import kotlinx.coroutines.launch

enum class ExploreFilterPill(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color
) {
    ALL("All", Icons.Default.GridView, Color(0xFF60A5FA)),
    LIVE("Live", Icons.Default.GraphicEq, Color(0xFF38BDF8)),
    STATIC("Static", Icons.Default.Image, Color(0xFF93C5FD)),
    PREMIUM("Premium", Icons.Default.WorkspacePremium, Color(0xFF60A5FA)),
    FREE("Free", Icons.Default.Sell, Color(0xFF34D399)),
    TRENDING("Trending", Icons.Default.TrendingUp, Color(0xFF60A5FA)),
    NEW("New", Icons.Default.AutoAwesome, Color(0xFF93C5FD))
}

data class CategoryItemData(
    val name: String,
    val icon: ImageVector,
    val neonColor: Color
)

@Composable
fun ExploreScreen(navController: NavController) {
    val context = LocalContext.current
    val authUid = remember {
        kotlinx.coroutines.runBlocking { AppContainer.authRepositoryImpl.getUserId() }
    }
    val viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = authUid
        )
    )

    val allWallpapers by viewModel.allWallpapers.collectAsState()
    val trendingWallpapers by viewModel.trendingWallpapers.collectAsState()
    val liveWallpapers by viewModel.liveWallpapers.collectAsState()
    val newWallpapers by viewModel.newWallpapers.collectAsState()
    val premiumWallpapers by viewModel.premiumWallpapers.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isPremiumUser by viewModel.isPremium.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterPill by remember { mutableStateOf(ExploreFilterPill.ALL) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    var showAllCategoriesDialog by remember { mutableStateOf(false) }
    var showAllTagsDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Advanced Filter Sheet options
    var currentSort by remember { mutableStateOf(WallpaperSortOrder.POPULAR) }
    var currentTypeFilter by remember { mutableStateOf(WallpaperTypeFilter.ALL) }
    var onlyVip by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Predefined Category catalog with neon icons
    val categoryList = remember {
        listOf(
            CategoryItemData("Anime", Icons.Default.AutoAwesome, Color(0xFFB388FF)),
            CategoryItemData("Nature", Icons.Default.Park, Color(0xFF4CAF50)),
            CategoryItemData("Cars", Icons.Default.Speed, Color(0xFFFF5252)),
            CategoryItemData("Gaming", Icons.Default.Gamepad, Color(0xFF00E5FF)),
            CategoryItemData("Abstract", Icons.Default.Layers, Color(0xFFE040FB)),
            CategoryItemData("AMOLED", Icons.Default.WbSunny, Color(0xFFFFD700)),
            CategoryItemData("Live", Icons.Default.GraphicEq, Color(0xFFFF3366))
        )
    }

    // Popular tags
    val popularTags = remember(allWallpapers) {
        listOf("anime", "naruto", "cars", "nature", "amoled", "minimal", "dark", "neon", "cyberpunk", "space")
    }

    // Is user actively filtering with a specific category, search, tag, or pill other than ALL?
    val isFilteredGridActive = searchQuery.isNotBlank() ||
            selectedCategoryFilter != null ||
            selectedTagFilter != null ||
            selectedFilterPill != ExploreFilterPill.ALL ||
            onlyVip ||
            currentTypeFilter != WallpaperTypeFilter.ALL

    // Compute Filtered Wallpapers
    val filteredWallpapers = remember(
        allWallpapers,
        trendingWallpapers,
        liveWallpapers,
        newWallpapers,
        premiumWallpapers,
        searchQuery,
        selectedFilterPill,
        selectedCategoryFilter,
        selectedTagFilter,
        currentSort,
        currentTypeFilter,
        onlyVip
    ) {
        var list = allWallpapers

        // Apply Pill Filter
        list = when (selectedFilterPill) {
            ExploreFilterPill.ALL -> list
            ExploreFilterPill.LIVE -> list.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }
            ExploreFilterPill.STATIC -> list.filter { it.type == "STATIC" }
            ExploreFilterPill.PREMIUM -> list.filter { it.isPremium }
            ExploreFilterPill.FREE -> list.filter { !it.isPremium }
            ExploreFilterPill.TRENDING -> list.filter { it.isTrending }
            ExploreFilterPill.NEW -> list.filter { it.isNew }
        }

        // Apply Category Filter
        if (selectedCategoryFilter != null) {
            val cat = selectedCategoryFilter!!.trim().lowercase()
            list = list.filter { wp ->
                wp.categoryIds.any { it.lowercase().contains(cat) } ||
                wp.title.lowercase().contains(cat) ||
                (wp.description?.lowercase()?.contains(cat) == true) ||
                (cat == "live" && (wp.type == "LIVE" || wp.type == "ADVANCED_LIVE")) ||
                (cat == "amoled" && wp.categoryIds.any { it.lowercase().contains("amoled") })
            }
        }

        // Apply Tag Filter
        if (selectedTagFilter != null) {
            val tag = selectedTagFilter!!.trim().lowercase()
            list = list.filter { wp ->
                wp.categoryIds.any { it.lowercase().contains(tag) } ||
                wp.title.lowercase().contains(tag) ||
                (wp.description?.lowercase()?.contains(tag) == true)
            }
        }

        // Apply Search Query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter { wp ->
                wp.title.lowercase().contains(q) ||
                (wp.description?.lowercase()?.contains(q) == true) ||
                wp.categoryIds.any { it.lowercase().contains(q) } ||
                wp.type.lowercase().contains(q)
            }
        }

        // Apply Advanced Sheet Filter (Type & VIP)
        list = when (currentTypeFilter) {
            WallpaperTypeFilter.ALL -> list
            WallpaperTypeFilter.LIVE_ONLY -> list.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }
            WallpaperTypeFilter.STATIC_ONLY -> list.filter { it.type == "STATIC" }
            WallpaperTypeFilter.DOUBLE_ONLY -> list.filter { it.hasHomeTransition || it.hasLockAnimation }
        }

        if (onlyVip) {
            list = list.filter { it.isPremium }
        }

        // Apply Sorting
        when (currentSort) {
            WallpaperSortOrder.POPULAR -> list.sortedByDescending { it.isTrending }
            WallpaperSortOrder.LATEST -> list.sortedByDescending { it.createdAt }
            WallpaperSortOrder.ALPHABETICAL -> list.sortedBy { it.title }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF090D14),
                drawerContentColor = Color.White
            ) {
                Spacer(Modifier.height(24.dp))
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
                            text = "Explore Gallery",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isPremiumUser) "VIP Lifetime Member" else "Free Edition",
                            fontSize = 12.sp,
                            color = if (isPremiumUser) Color(0xFF60A5FA) else Color(0xFF8E8EA0)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Home Gallery", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("home")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF2A55)) },
                    label = { Text("Favorites", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("favorites")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("VIP Studio", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("premium")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF3897F0)) },
                    label = { Text("Settings", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile")
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B10))
        ) {
            // 1. TOP BAR: Hamburger Menu | "W Explore" | VIP Blue Badge | Profile Avatar
            ExploreTopHeader(
                isPremium = isPremiumUser,
                onMenuClick = { scope.launch { drawerState.open() } },
                onVipClick = { navController.navigate("premium") },
                onProfileClick = { navController.navigate("profile") }
            )

            // 2. SEARCH BAR with Filter Setting Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search wallpapers, categories, tags...",
                    onFilterClick = { showFilterSheet = true }
                )
            }

            // 3. HORIZONTAL FILTER PILLS: All | Live | Static | Premium | Free | Trending | New
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ExploreFilterPill.values()) { pill ->
                    ExploreFilterPillChip(
                        pill = pill,
                        isSelected = selectedFilterPill == pill,
                        onClick = {
                            selectedFilterPill = pill
                            if (pill == ExploreFilterPill.ALL) {
                                selectedCategoryFilter = null
                                selectedTagFilter = null
                            }
                        }
                    )
                }
            }

            // Main Content Area
            when (val state = uiState) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WallpaperCarouselSkeleton(itemWidth = 160.dp, itemHeight = 220.dp, count = 2)
                        WallpaperCarouselSkeleton(itemWidth = 160.dp, itemHeight = 220.dp, count = 2)
                    }
                }

                is UiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is UiState.Success -> {
                    if (isFilteredGridActive) {
                        // FILTERED / SEARCH RESULTS GRID VIEW
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 4.dp)
                        ) {
                            // Active Filter Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val activeTitle = when {
                                    searchQuery.isNotBlank() -> "Search: \"$searchQuery\""
                                    selectedCategoryFilter != null -> "Category: ${selectedCategoryFilter}"
                                    selectedTagFilter != null -> "Tag: #${selectedTagFilter}"
                                    selectedFilterPill != ExploreFilterPill.ALL -> selectedFilterPill.label
                                    else -> "Filtered Wallpapers"
                                }

                                Text(
                                    text = "$activeTitle (${filteredWallpapers.size})",
                                    color = Color(0xFFA0A0B8),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategoryFilter = null
                                        selectedTagFilter = null
                                        selectedFilterPill = ExploreFilterPill.ALL
                                        onlyVip = false
                                        currentTypeFilter = WallpaperTypeFilter.ALL
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = ChampagnePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Clear Filter",
                                            color = ChampagnePrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            if (filteredWallpapers.isEmpty()) {
                                EmptyState(
                                    title = "No Wallpapers Found",
                                    message = "Try exploring different categories or reset active filters.",
                                    icon = Icons.Default.SearchOff,
                                    actionText = "Reset All Filters",
                                    onActionClick = {
                                        searchQuery = ""
                                        selectedCategoryFilter = null
                                        selectedTagFilter = null
                                        selectedFilterPill = ExploreFilterPill.ALL
                                        onlyVip = false
                                        currentTypeFilter = WallpaperTypeFilter.ALL
                                    }
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredWallpapers, key = { it.id }) { wp ->
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
                    } else {
                        // DEFAULT FULL EXPLORE SCREEN OVERVIEW (Matching Screenshot)
                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(bottom = 32.dp)
                        ) {
                            // SECTION 1: CATEGORIES
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Categories",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .clickable { showAllCategoriesDialog = true }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "View all",
                                        color = Color(0xFFB388FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "View all categories",
                                        tint = Color(0xFFB388FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(categoryList, key = { it.name }) { category ->
                                    CategorySquareCard(
                                        category = category,
                                        isSelected = selectedCategoryFilter.equals(category.name, ignoreCase = true),
                                        onClick = {
                                            selectedCategoryFilter = if (selectedCategoryFilter.equals(category.name, ignoreCase = true)) null else category.name
                                        }
                                    )
                                }
                            }

                            // SECTION 2: POPULAR TAGS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Popular Tags",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier
                                        .clickable { showAllTagsDialog = true }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "View all",
                                        color = Color(0xFFB388FF),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "View all tags",
                                        tint = Color(0xFFB388FF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(popularTags, key = { it }) { tag ->
                                    TagPill(
                                        tag = tag,
                                        isSelected = selectedTagFilter.equals(tag, ignoreCase = true),
                                        onClick = {
                                            selectedTagFilter = if (selectedTagFilter.equals(tag, ignoreCase = true)) null else tag
                                        }
                                    )
                                }
                            }

                            // SECTION 3: TRENDING
                            val trendingShowcase = remember(trendingWallpapers, allWallpapers) {
                                if (trendingWallpapers.isNotEmpty()) trendingWallpapers.take(2)
                                else allWallpapers.take(2)
                            }
                            if (trendingShowcase.isNotEmpty()) {
                                SectionWithViewAll(
                                    title = "Trending",
                                    onViewAllClick = { selectedFilterPill = ExploreFilterPill.TRENDING }
                                )
                                ShowcaseGridRow(
                                    wallpapers = trendingShowcase,
                                    navController = navController,
                                    onFavoriteToggle = { id, fav -> viewModel.toggleFavorite(id, fav) }
                                )
                            }

                            // SECTION 4: NEW WALLPAPERS
                            val newShowcase = remember(newWallpapers, allWallpapers) {
                                if (newWallpapers.isNotEmpty()) newWallpapers.take(2)
                                else allWallpapers.drop(2).take(2)
                            }
                            if (newShowcase.isNotEmpty()) {
                                SectionWithViewAll(
                                    title = "New Wallpapers",
                                    onViewAllClick = { selectedFilterPill = ExploreFilterPill.NEW }
                                )
                                ShowcaseGridRow(
                                    wallpapers = newShowcase,
                                    navController = navController,
                                    onFavoriteToggle = { id, fav -> viewModel.toggleFavorite(id, fav) }
                                )
                            }

                            // SECTION 5: LIVE WALLPAPERS
                            val liveShowcase = remember(liveWallpapers, allWallpapers) {
                                if (liveWallpapers.isNotEmpty()) liveWallpapers.take(2)
                                else allWallpapers.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" }.take(2)
                            }
                            if (liveShowcase.isNotEmpty()) {
                                SectionWithViewAll(
                                    title = "Live Wallpapers",
                                    onViewAllClick = { selectedFilterPill = ExploreFilterPill.LIVE }
                                )
                                ShowcaseGridRow(
                                    wallpapers = liveShowcase,
                                    navController = navController,
                                    onFavoriteToggle = { id, fav -> viewModel.toggleFavorite(id, fav) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Filter Sheet
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

        // All Categories Dialog
        if (showAllCategoriesDialog) {
            AlertDialog(
                onDismissRequest = { showAllCategoriesDialog = false },
                containerColor = Color(0xFF14121E),
                title = {
                    Text(
                        text = "All Categories",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryList.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selectedCategoryFilter == cat.name) Color(0xFF281C3E) else Color(0xFF1B192A))
                                    .clickable {
                                        selectedCategoryFilter = cat.name
                                        showAllCategoriesDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.neonColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = cat.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllCategoriesDialog = false }) {
                        Text("Close", color = ChampagnePrimary)
                    }
                }
            )
        }

        // All Tags Dialog
        if (showAllTagsDialog) {
            AlertDialog(
                onDismissRequest = { showAllTagsDialog = false },
                containerColor = Color(0xFF14121E),
                title = {
                    Text(
                        text = "All Popular Tags",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        popularTags.chunked(3).forEach { rowTags ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1F1D2E))
                                            .clickable {
                                                selectedTagFilter = tag
                                                showAllTagsDialog = false
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            color = Color(0xFFD0D0E0),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllTagsDialog = false }) {
                        Text("Close", color = ChampagnePrimary)
                    }
                }
            )
        }
    }
}

/**
 * Top Header: Hamburger | "W Explore" | VIP Pill Badge | Gradient User Avatar
 */
@Composable
fun ExploreTopHeader(
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
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Drawer Menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Circular App Logo Icon
            AppLogoIcon(size = 32.dp)

            Text(
                text = "Explore",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
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
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isPremium) "VIP PRO" else "VIP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Stylized Avatar with online blue dot indicator
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onProfileClick)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0xFF101622))
                        .border(1.2.dp, Color(0xFF2563EB).copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Small Blue Activity Dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                        .border(1.5.dp, Color(0xFF080B10), CircleShape)
                )
            }
        }
    }
}

/**
 * Filter Pill Chip (All | Live | Static | Premium | Free | Trending | New)
 */
@Composable
fun ExploreFilterPillChip(
    pill: ExploreFilterPill,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) Color(0xFF2563EB) else Color(0xFF101622)
            )
            .border(
                width = if (isSelected) 1.dp else 0.8.dp,
                color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF1E2A3C),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = pill.icon,
                contentDescription = pill.label,
                tint = if (isSelected) Color.White else pill.iconTint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = pill.label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Category Square Card with glowing blue/accent outline and icon
 */
@Composable
fun CategorySquareCard(
    category: CategoryItemData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(shape)
                .background(
                    if (isSelected) Color(0xFF1E293B) else Color(0xFF101622)
                )
                .border(
                    width = if (isSelected) 1.5.dp else 0.8.dp,
                    color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E2A3C),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = if (isSelected) Color(0xFF60A5FA) else category.neonColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Text(
            text = category.name,
            color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Tag Pill: simple and scannable
 */
@Composable
fun TagPill(
    tag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF2563EB) else Color(0xFF101622)
            )
            .border(
                0.8.dp,
                if (isSelected) Color(0xFF60A5FA) else Color(0xFF1E2A3C),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = tag,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Section Header with functional View All
 */
@Composable
fun SectionWithViewAll(
    title: String,
    onViewAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .clickable(onClick = onViewAllClick)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "View all",
                color = Color(0xFF60A5FA),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View all $title",
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 2-Card Showcase Row (for Trending, New, Live sections in default overview)
 */
@Composable
fun ShowcaseGridRow(
    wallpapers: List<Wallpaper>,
    navController: NavController,
    onFavoriteToggle: (String, Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        wallpapers.forEach { wp ->
            WallpaperCard(
                wallpaper = wp,
                showTitleOverlay = true,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.62f),
                onFavoriteToggle = {
                    onFavoriteToggle(wp.id, wp.isFavorite)
                },
                onClick = {
                    navController.navigate("detail/${wp.id}")
                }
            )
        }
    }
}
