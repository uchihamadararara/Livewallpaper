with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import androidx.compose.material.icons.Icons',
    'import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.automirrored.filled.ArrowBack'
)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
    f.write(content)
