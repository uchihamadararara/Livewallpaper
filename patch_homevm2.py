with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'r') as f:
    content = f.read()

new_flows = """
    val allWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val liveWallpapers: StateFlow<List<Wallpaper>>"""

content = content.replace("    val liveWallpapers: StateFlow<List<Wallpaper>>", new_flows)

with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'w') as f:
    f.write(content)
