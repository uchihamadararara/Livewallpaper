package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.ui.components.AppLogoIcon
import com.example.ui.theme.ChampagnePrimary
import com.example.util.OemHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val authUserId = kotlinx.coroutines.runBlocking { AppContainer.authRepositoryImpl.getUserId() }
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(
            userRepository = AppContainer.userRepository,
            authRepository = AppContainer.authRepositoryImpl,
            billingRepository = AppContainer.getBillingRepository(context),
            authUserId = authUserId
        )
    )

    val userProfile by viewModel.userProfile.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(syncState) {
        syncState?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSyncMessage()
        }
    }

    val isVip = userProfile?.subscriptionStatus == "ACTIVE"
    val oemNotice = OemHelper.getLiveWallpaperLimitationNotice()

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
                    AppLogoIcon(size = 42.dp)
                    Column {
                        Text(
                            text = "Live Wallpaper",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isVip) "VIP Member" else "Free Explorer",
                            color = if (isVip) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 12.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Home Feed", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("home")
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Explore & Categories", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("explore")
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF2A55)) },
                    label = { Text("Favorites", color = Color.White) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("favorites")
                    },
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
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF60A5FA)) },
                    label = { Text("Account & Settings", color = Color.White) },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFF1E293B),
                        unselectedContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF080B10))
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.open() } },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF101622))
                            .border(1.dp, Color(0xFF1E2A3C), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer Menu",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Account & Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }

                // VIP Blue Pill Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                            )
                        )
                        .clickable { navController.navigate("premium") }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "VIP",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "VIP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. ACCOUNT SECTION
                Text(
                    text = "ACCOUNT",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F1B2D), Color(0xFF0A121E))
                            )
                        )
                        .border(1.dp, Color(0xFF1E2A3C), RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .border(2.dp, Color(0xFF60A5FA), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = userProfile?.displayName ?: "Explorer",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isVip) Color(0xFF1E3A8A) else Color(0xFF1E2A3C))
                                            .border(0.5.dp, if (isVip) Color(0xFF60A5FA) else Color(0xFF334155), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isVip) "VIP" else "FREE",
                                            color = if (isVip) Color(0xFF93C5FD) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = userProfile?.email ?: if (authUserId != null) "Cloud Account Connected" else "Guest Account",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Synced with Cloud Catalog",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.syncAccount() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2A3C)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sync Account & Purchases",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 2. SUBSCRIPTION SECTION
                Text(
                    text = "SUBSCRIPTION & VIP",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isVip) Brush.verticalGradient(listOf(Color(0xFF0F2347), Color(0xFF0B172E)))
                            else Brush.verticalGradient(listOf(Color(0xFF0F1B2D), Color(0xFF0A121E)))
                        )
                        .border(
                            width = 1.dp,
                            color = if (isVip) Color(0xFF1E3A8A) else Color(0xFF1E2A3C),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (isVip) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isVip) "VIP Studio Active" else "Free Explorer Plan",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    if (isVip && (userProfile?.subscriptionExpiry ?: 0L) > 0L) {
                                        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                            .format(Date(userProfile!!.subscriptionExpiry))
                                        Text(
                                            text = "Renews: $dateStr",
                                            color = Color(0xFF60A5FA),
                                            fontSize = 12.sp
                                        )
                                    } else {
                                        Text(
                                            text = if (isVip) "Lifetime VIP Access" else "Standard 1080p Access",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            if (!isVip) {
                                Button(
                                    onClick = { navController.navigate("premium") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Upgrade",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Already subscribed on Google Play?",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            TextButton(
                                onClick = {
                                    viewModel.syncAccount()
                                    Toast.makeText(context, "Checking Google Play purchases...", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Restore", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. SYSTEM & DEVICE SUPPORT
                Text(
                    text = "DEVICE & SYSTEM SUPPORT",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1522))
                        .border(1.dp, Color(0xFF1E2A3C), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (oemNotice != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = oemNotice,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { OemHelper.openBatteryOptimizationSettings(context) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Battery Optimization Guidance",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Keep Live Wallpapers and charging animations running smoothly in background",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 4. LEGAL & ABOUT
                Text(
                    text = "ABOUT & LEGAL",
                    color = Color(0xFF60A5FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E1522))
                        .border(1.dp, Color(0xFF1E2A3C), RoundedCornerShape(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("About Live Wallpaper", color = Color.White, fontSize = 14.sp)
                        Text("v1.0", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Privacy Policy", color = Color.White, fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTermsDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Terms of Service", color = Color.White, fontSize = 14.sp)
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF0F1522),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = {
                Text("Live Wallpaper", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0 (Production Release)")
                    Text("Crafted for high-refresh rate AMOLED Android displays with seamless offline playback and low battery consumption.")
                    Text("All artworks are curated and optimized with real-time charging animations.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = Color(0xFF0F1522),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = {
                Text("Privacy Policy", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("We respect your privacy. No personal photos, media files, or location data are collected by Live Wallpaper.")
                    Text("Applied wallpapers are stored locally on your device for offline rendering.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            containerColor = Color(0xFF0F1522),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = {
                Text("Terms of Service", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("All wallpapers and audio assets are provided for personal device personalization only.")
                    Text("VIP subscriptions unlock exclusive media and are managed securely via Google Play.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
