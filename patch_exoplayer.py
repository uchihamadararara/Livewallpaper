import re

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

old_code = "ExoPlayer.Builder(context).build().apply {"
new_code = "ExoPlayer.Builder(context).setRenderersFactory(androidx.media3.exoplayer.DefaultRenderersFactory(context).setEnableDecoderFallback(true)).build().apply {"

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
        f.write(content)
    print("Patched ExoPlayer")
else:
    print("Could not find ExoPlayer builder code")
