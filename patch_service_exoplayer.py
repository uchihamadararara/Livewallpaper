import re

with open('app/src/main/java/com/example/service/AdvancedWallpaperService.kt', 'r') as f:
    content = f.read()

old_code = "ExoPlayer.Builder(applicationContext).build().apply {"
new_code = "ExoPlayer.Builder(applicationContext).setRenderersFactory(androidx.media3.exoplayer.DefaultRenderersFactory(applicationContext).setEnableDecoderFallback(true)).build().apply {"

if old_code in content:
    content = content.replace(old_code, new_code)
    with open('app/src/main/java/com/example/service/AdvancedWallpaperService.kt', 'w') as f:
        f.write(content)
    print("Patched ExoPlayer in AdvancedWallpaperService")
else:
    print("Could not find ExoPlayer builder code in AdvancedWallpaperService")
