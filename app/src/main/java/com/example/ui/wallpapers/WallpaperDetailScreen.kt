package com.example.ui.wallpapers

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.service.AdvancedWallpaperService
import com.example.ui.theme.ChampagnePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PreviewMode {
    CLEAN,
    LOCK_SCREEN,
    HOME_SCREEN,
    CHARGING
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun WallpaperDetailScreen(
    wallpaperId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: WallpaperDetailViewModel = viewModel(
        factory = ViewModelFactory(
            extraId = wallpaperId,
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            userPreferencesRepository = AppContainer.getUserPreferencesRepository(context),
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
        )
    )

    val wallpaper by viewModel.wallpaper.collectAsState()
    val applyState by viewModel.applyState.collectAsState()
    val isPreviewSoundOn by viewModel.isPreviewSoundOn.collectAsState()

    var showStaticTargetDialog by remember { mutableStateOf(false) }
    var pendingStaticWallpaper by remember { mutableStateOf<com.example.domain.models.Wallpaper?>(null) }

    var showSoundDialog by remember { mutableStateOf(false) }
    var pendingLiveWallpaper by remember { mutableStateOf<com.example.domain.models.Wallpaper?>(null) }

    var currentPreviewMode by remember { mutableStateOf(PreviewMode.CLEAN) }

    LaunchedEffect(applyState) {
        when (val state = applyState) {
            is ApplyState.RequiresAuth -> {
                Toast.makeText(context, "Please sign in to use this wallpaper.", Toast.LENGTH_SHORT).show()
                navController.navigate("profile")
                viewModel.resetApplyState(context)
            }

            is ApplyState.RequiresRewardAd -> {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    Toast.makeText(context, "Loading video to unlock wallpaper...", Toast.LENGTH_SHORT).show()
                    val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
                    com.google.android.gms.ads.rewarded.RewardedAd.load(
                        activity,
                        "ca-app-pub-3940256099942544/5224354917",
                        adRequest,
                        object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                            override fun onAdLoaded(ad: com.google.android.gms.ads.rewarded.RewardedAd) {
                                val options = com.google.android.gms.ads.rewarded.ServerSideVerificationOptions.Builder()
                                    .setCustomData("${state.uid}:${state.wallpaper.id}")
                                    .build()
                                ad.setServerSideVerificationOptions(options)
                                ad.show(activity) { _ ->
                                    viewModel.onRewardAdEarned(state.wallpaper)
                                }
                            }
                            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                                Toast.makeText(context, "Ad unavailable. Unlocking wallpaper directly.", Toast.LENGTH_SHORT).show()
                                viewModel.onRewardAdEarned(state.wallpaper)
                            }
                        }
                    )
                }
            }

            is ApplyState.RequiresSubscription -> {
                Toast.makeText(context, "VIP Membership required for this artwork.", Toast.LENGTH_LONG).show()
                navController.navigate("premium")
                viewModel.resetApplyState(context)
            }

            is ApplyState.ShowStaticTargetSelection -> {
                pendingStaticWallpaper = state.wallpaper
                showStaticTargetDialog = true
            }

            is ApplyState.ShowSoundPrompt -> {
                pendingLiveWallpaper = state.wallpaper
                showSoundDialog = true
            }

            is ApplyState.ReadyToDownloadLive -> {
                viewModel.executeLiveDownloadAndPrepare(
                    context = context,
                    wallpaper = state.wallpaper,
                    primaryVideoUrl = state.videoUrl,
                    soundEnabled = state.soundEnabled
                )
            }

            is ApplyState.ReadyToApplyStatic -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(state.tempFile.absolutePath)
                        if (bitmap != null) {
                            val wm = WallpaperManager.getInstance(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wm.setBitmap(bitmap, null, true, state.targetFlags)
                            } else {
                                wm.setBitmap(bitmap)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Wallpaper Applied Successfully! 🎉", Toast.LENGTH_SHORT).show()
                                viewModel.onStaticWallpaperAppliedSuccessfully(context, state.wallpaper, state.tempFile)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed to decode wallpaper image.", Toast.LENGTH_SHORT).show()
                                viewModel.resetApplyState(context)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to apply: ${e.message}", Toast.LENGTH_SHORT).show()
                            viewModel.resetApplyState(context)
                        }
                    }
                }
            }

            is ApplyState.ReadyToLaunchLiveSystemIntent -> {
                try {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, AdvancedWallpaperService::class.java)
                        )
                    }
                    context.startActivity(intent)
                    viewModel.onLiveWallpaperPickerLaunched(context, state.wallpaper, state.manifest, state.soundEnabled)
                } catch (e: Exception) {
                    try {
                        val fallbackIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                        context.startActivity(fallbackIntent)
                        viewModel.onLiveWallpaperPickerLaunched(context, state.wallpaper, state.manifest, state.soundEnabled)
                    } catch (fallbackEx: Exception) {
                        Toast.makeText(context, "Please select 'Live Wallpaper Engine' in settings.", Toast.LENGTH_LONG).show()
                        viewModel.resetApplyState(context)
                    }
                }
            }

            is ApplyState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetApplyState(context)
            }

            is ApplyState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.resetApplyState(context)
            }

            else -> {}
        }
    }

    // Static Target Selection Dialog (Home, Lock, Both)
    if (showStaticTargetDialog && pendingStaticWallpaper != null) {
        val wp = pendingStaticWallpaper!!
        AlertDialog(
            onDismissRequest = {
                showStaticTargetDialog = false
                viewModel.resetApplyState(context)
            },
            containerColor = Color(0xFF0F1522),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            title = {
                Text("Set Wallpaper On", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select where you would like to apply this high-resolution artwork:")
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showStaticTargetDialog = false
                            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.FLAG_SYSTEM else 0
                            viewModel.onStaticTargetSelected(context, wp, flag)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A3C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Home Screen")
                    }
                    Button(
                        onClick = {
                            showStaticTargetDialog = false
                            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.FLAG_LOCK else 0
                            viewModel.onStaticTargetSelected(context, wp, flag)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A3C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lock Screen")
                    }
                    Button(
                        onClick = {
                            showStaticTargetDialog = false
                            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            } else {
                                0
                            }
                            viewModel.onStaticTargetSelected(context, wp, flag)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Both Screens (Recommended)", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showStaticTargetDialog = false
                        viewModel.resetApplyState(context)
                    }
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // Live Wallpaper Audio Prompt Dialog
    if (showSoundDialog && pendingLiveWallpaper != null) {
        val wp = pendingLiveWallpaper!!
        AlertDialog(
            onDismissRequest = {
                showSoundDialog = false
                viewModel.resetApplyState(context)
            },
            containerColor = Color(0xFF0F1522),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            icon = {
                Icon(Icons.Default.MusicNote, contentDescription = "Audio", tint = Color(0xFF60A5FA))
            },
            title = {
                Text("Enable Wallpaper Audio?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This live wallpaper has custom ambient audio. Would you like audio enabled when active on your home screen?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSoundDialog = false
                        viewModel.onSoundPreferenceSelected(wp, soundOn = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sound ON")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSoundDialog = false
                        viewModel.onSoundPreferenceSelected(wp, soundOn = false)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Sound OFF (Muted)")
                }
            }
        )
    }

    if (wallpaper == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF080B10)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }
    val wp = wallpaper!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isLive = wp.type == "LIVE" || wp.type == "ADVANCED_LIVE"

        // 1. IMMERSIVE MEDIA BACKGROUND
        val activePreviewUrl = remember(currentPreviewMode, wp) {
            val config = wp.advancedConfig
            when (currentPreviewMode) {
                PreviewMode.CHARGING -> {
                    if (config?.chargingAnimationEnabled == true && !config.chargingAnimationVideoUrl.isNullOrEmpty()) {
                        config.chargingAnimationVideoUrl
                    } else {
                        wp.videoUrl
                    }
                }
                PreviewMode.LOCK_SCREEN -> {
                    if (wp.liveExperienceType == com.example.domain.models.LiveExperienceType.TRANSITION &&
                        config?.lockAnimationEnabled == true && !config.lockAnimationVideoUrl.isNullOrEmpty()) {
                        config.lockAnimationVideoUrl
                    } else {
                        wp.videoUrl
                    }
                }
                else -> wp.videoUrl
            }
        }

        if (isLive && !activePreviewUrl.isNullOrEmpty()) {
            val exoPlayer = remember {
                val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
                ExoPlayer.Builder(context, renderers).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    volume = if (isPreviewSoundOn) 1f else 0f
                }
            }

            LaunchedEffect(activePreviewUrl) {
                exoPlayer.setMediaItem(MediaItem.fromUri(activePreviewUrl))
                exoPlayer.prepare()
                exoPlayer.play()
            }

            LaunchedEffect(isPreviewSoundOn) {
                exoPlayer.volume = if (isPreviewSoundOn) 1f else 0f
            }

            DisposableEffect(Unit) {
                onDispose {
                    exoPlayer.release()
                }
            }

            AndroidView(
                factory = {
                    val view = android.view.LayoutInflater.from(context).inflate(com.example.R.layout.view_player, null) as PlayerView
                    view.player = exoPlayer
                    view
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = wp.imageUrl ?: wp.thumbnailUrl,
                contentDescription = wp.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. SIMULATOR OVERLAYS (Lock Screen, Home Screen, Charging)
        when (currentPreviewMode) {
            PreviewMode.LOCK_SCREEN -> {
                LockScreenSimulatorOverlay()
            }
            PreviewMode.HOME_SCREEN -> {
                HomeScreenSimulatorOverlay()
            }
            PreviewMode.CHARGING -> {
                // When in charging preview mode, let the actual charging video play in full visual glory
            }
            PreviewMode.CLEAN -> {
                // Pure wallpaper view
            }
        }

        // 3. TOP GLASS ACTION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sound toggle if live
                if (wp.soundAvailable && isLive) {
                    IconButton(
                        onClick = { viewModel.togglePreviewSound() },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPreviewSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Audio Toggle",
                            tint = if (isPreviewSoundOn) Color(0xFF60A5FA) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Favorite heart button
                IconButton(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (wp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (wp.isFavorite) Color(0xFFFF2A55) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 4. BOTTOM FLOATING GLASS CONTROL SHEET
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFF04060A).copy(alpha = 0.7f),
                            Color(0xFF04060A).copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // SIMULATOR MODE SELECTOR PILLS
                val availablePreviewModes = remember(wp.hasChargingAnimation) {
                    val list = mutableListOf(
                        PreviewMode.CLEAN to "🖼 Clean",
                        PreviewMode.LOCK_SCREEN to "🔒 Lock",
                        PreviewMode.HOME_SCREEN to "📱 Home"
                    )
                    if (wp.hasChargingAnimation) {
                        list.add(PreviewMode.CHARGING to "⚡ Charge")
                    }
                    list
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0D131F).copy(alpha = 0.85f))
                        .border(0.8.dp, Color(0xFF1E2A3C), RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    availablePreviewModes.forEach { (mode, label) ->
                        val isSelected = currentPreviewMode == mode
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
                                .clickable { currentPreviewMode = mode }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // TITLE & RESOLUTION BADGES
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0284C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("LIVE 60FPS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(0.8.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ULTRA HD", color = Color(0xFF93C5FD), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (wp.isPremium) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("VIP ONLY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        if (wp.hasChargingAnimation) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0C1929))
                                    .border(0.8.dp, Color(0xFF38BDF8), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("⚡ CHARGING FX", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = wp.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )

                    if (!wp.description.isNullOrBlank()) {
                        Text(
                            text = wp.description,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            maxLines = 2
                        )
                    }
                }

                // APPLY WALLPAPER PRIMARY ACTION BUTTON
                val isApplying = applyState is ApplyState.Applying
                val progressText = (applyState as? ApplyState.Applying)?.message

                Button(
                    onClick = { viewModel.onApplyClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    enabled = !isApplying
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF0284C7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isApplying) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = progressText ?: "Applying Artwork...",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLive) Icons.Default.PlayCircleFilled else Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (isLive) "SET AS LIVE WALLPAPER" else "APPLY WALLPAPER",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Realistic Lock Screen Simulator Overlay
 */
@Composable
fun LockScreenSimulatorOverlay() {
    val currentTime = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val currentDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentTime,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = (-2).sp
            )
            Text(
                text = currentDate,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Bottom shortcut icons (Flashlight & Camera)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp, vertical = 130.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/**
 * Realistic Home Screen Simulator Overlay with Dock Icons & Search
 */
@Composable
fun HomeScreenSimulatorOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 70.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Google Search Widget Mock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Search apps & web...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }
        }

        // Mock App Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                Icons.Default.Phone to Color(0xFF4CAF50),
                Icons.Default.ChatBubble to Color(0xFF2196F3),
                Icons.Default.Camera to Color(0xFFE91E63),
                Icons.Default.Language to Color(0xFFFF9800)
            ).forEach { (icon, bg) ->
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

