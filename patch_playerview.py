import re

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

old_code = """                AndroidView(
                    factory = {
                        PlayerView(context).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )"""

new_code = """                AndroidView(
                    factory = {
                        val view = android.view.LayoutInflater.from(context).inflate(com.example.R.layout.view_player, null) as PlayerView
                        view.player = exoPlayer
                        view
                    },
                    modifier = Modifier.fillMaxSize()
                )"""

content = content.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
    f.write(content)
