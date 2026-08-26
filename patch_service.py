with open('app/src/main/java/com/example/service/AdvancedWallpaperService.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        private fun loadActiveWallpaper() {
            serviceScope.launch {""",
"""        private fun loadActiveWallpaper() {
            serviceScope.launch {
                soundEnabledByUser = AppContainer.getUserPreferencesRepository(applicationContext).isSoundEnabled.firstOrNull() ?: false
                player?.volume = if (soundEnabledByUser) 1f else 0f
"""
)

with open('app/src/main/java/com/example/service/AdvancedWallpaperService.kt', 'w') as f:
    f.write(content)
