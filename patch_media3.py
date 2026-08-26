import re

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

# Add imports if missing
imports = """
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.remember
"""

content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\n" + imports)

# Find the AsyncImage block
async_image_block = """            AsyncImage(
                model = wp.imageUrl ?: wp.thumbnailUrl,
                contentDescription = wp.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )"""

exo_player_block = """            val isLive = wp.type == "LIVE" || wp.type == "ADVANCED_LIVE"
            if (isLive && !wp.videoUrl.isNullOrEmpty()) {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(wp.videoUrl))
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        volume = 0f // Muted preview by default
                        prepare()
                    }
                }
                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
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
            }"""

content = content.replace(async_image_block, exo_player_block)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
    f.write(content)
