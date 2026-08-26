package com.example.ui.wallpapers

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.util.OemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(applyState) {
        when (val state = applyState) {
            is ApplyState.RequiresAuth -> {
                Toast.makeText(context, "Please sign in to use this wallpaper.", Toast.LENGTH_SHORT).show()
                navController.navigate("settings")
                viewModel.resetApplyState(context)
            }

            is ApplyState.RequiresRewardAd -> {
                val activity = context as? android.app.Activity
                if (activity != null) {
                    Toast.makeText(context, "Loading rewarded ad to unlock wallpaper...", Toast.LENGTH_SHORT).show()
                    val adRequest = com.google.android.gms.ads.AdRequest.Builder().build()
                    com.google.android.gms.ads.rewarded.RewardedAd.load(
                        activity,
                        "ca-app-pub-3940256099942544/5224354917", // Test Ad Unit
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
                                Toast.makeText(context, "Ad unavailable. Please check your connection.", Toast.LENGTH_SHORT).show()
                                viewModel.resetApplyState(context)
                            }
                        }
                    )
                }
            }

            is ApplyState.RequiresSubscription -> {
                Toast.makeText(context, "Premium subscription required.", Toast.LENGTH_LONG).show()
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
                    videoUrl = state.videoUrl,
                    soundEnabled = state.soundEnabled
                )
            }

            is ApplyState.ReadyToApplyStatic -> {
                scope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(state.tempFile.absolutePath)
                        if (bitmap != null) {
                            val wallpaperManager = WallpaperManager.getInstance(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                wallpaperManager.setBitmap(bitmap, null, true, state.targetFlags)
                            } else {
                                wallpaperManager.setBitmap(bitmap)
                            }
                            withContext(Dispatchers.Main) {
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
                            Toast.makeText(context, "Failed to apply wallpaper: ${e.message}", Toast.LENGTH_SHORT).show()
                            viewModel.resetApplyState(context)
                        }
                    }
                }
            }

            is ApplyState.ReadyToLaunchLiveSystemIntent -> {
                val wp = state.wallpaper
                viewModel.onLiveWallpaperPickerLaunched(context, wp, state.tempFile, state.soundEnabled)
                try {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, AdvancedWallpaperService::class.java)
                        )
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                        context.startActivity(chooserIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "System live wallpaper picker not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                // Show OEM notice if applicable
                OemHelper.getLiveWallpaperLimitationNotice()?.let { notice ->
                    Toast.makeText(context, notice, Toast.LENGTH_LONG).show()
                }
            }

            is ApplyState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetApplyState()
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
            title = {
                Text(
                    text = "Apply Wallpaper",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose where you would like to apply this wallpaper:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ListItem(
                        headlineContent = { Text("Home Screen") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(
                                onClick = {
                                    showStaticTargetDialog = false
                                    viewModel.onStaticTargetSelected(
                                        context,
                                        wp,
                                        WallpaperManager.FLAG_SYSTEM
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Lock Screen") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(
                                onClick = {
                                    showStaticTargetDialog = false
                                    viewModel.onStaticTargetSelected(
                                        context,
                                        wp,
                                        WallpaperManager.FLAG_LOCK
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Home & Lock Screen") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingContent = {
                            Button(
                                onClick = {
                                    showStaticTargetDialog = false
                                    viewModel.onStaticTargetSelected(
                                        context,
                                        wp,
                                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Set Both", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    )
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
                    Text("Cancel")
                }
            }
        )
    }

    // Live Wallpaper Sound Dialog
    if (showSoundDialog && pendingLiveWallpaper != null) {
        val wp = pendingLiveWallpaper!!
        AlertDialog(
            onDismissRequest = {
                showSoundDialog = false
                viewModel.resetApplyState(context)
            },
            icon = {
                Icon(Icons.Default.MusicNote, contentDescription = "Audio", tint = MaterialTheme.colorScheme.primary)
            },
            title = {
                Text("Live Wallpaper Sound", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This live wallpaper includes sound. Would you like to enable audio when applied on your device? (Default is Muted)")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSoundDialog = false
                        viewModel.onSoundPreferenceSelected(wp, soundOn = true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Enable Sound")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSoundDialog = false
                        viewModel.onSoundPreferenceSelected(wp, soundOn = false)
                    }
                ) {
                    Text("Keep Muted (Default)")
                }
            }
        )
    }

    if (wallpaper == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val wp = wallpaper!!

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Preview Sound Toggle Button if sound is available
                    if (wp.soundAvailable && (wp.type == "LIVE" || wp.type == "ADVANCED_LIVE")) {
                        IconButton(
                            onClick = { viewModel.togglePreviewSound() },
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPreviewSoundOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = if (isPreviewSoundOn) "Mute Preview Audio" else "Unmute Preview Audio",
                                tint = if (isPreviewSoundOn) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (wp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (wp.isFavorite) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val isLive = wp.type == "LIVE" || wp.type == "ADVANCED_LIVE"
            if (isLive && !wp.videoUrl.isNullOrEmpty()) {
                val exoPlayer = remember {
                    val renderers = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
                    ExoPlayer.Builder(context, renderers).build().apply {
                        setMediaItem(MediaItem.fromUri(wp.videoUrl))
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        volume = if (isPreviewSoundOn) 1f else 0f
                        prepare()
                    }
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

            // Bottom Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                // Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isLiveType = wp.type == "LIVE" || wp.type == "ADVANCED_LIVE"
                    if (isLiveType) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                    if (wp.isPremium) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Star, contentDescription = "Premium", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("PREMIUM", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                    if (wp.soundAvailable) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.MusicNote, contentDescription = "Audio", tint = Color.White, modifier = Modifier.size(12.dp))
                                Text("AUDIO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = wp.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-1).sp
                )

                if (!wp.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = wp.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                val isApplying = applyState is ApplyState.Applying
                val progressText = (applyState as? ApplyState.Applying)?.message

                Button(
                    onClick = { viewModel.onApplyClicked() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (wp.isPremium) MaterialTheme.colorScheme.primary else Color.White
                    ),
                    enabled = !isApplying
                ) {
                    if (isApplying) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = progressText ?: "Preparing...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    } else {
                        Text(
                            text = if (wp.isPremium) "Apply Premium Wallpaper" else "Apply Wallpaper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (wp.isPremium) MaterialTheme.colorScheme.onPrimary else Color.Black
                        )
                    }
                }
            }
        }
    }
}
