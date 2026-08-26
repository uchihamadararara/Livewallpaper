with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'r') as f:
    content = f.read()

new_flows = """
    val liveWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.type == "LIVE" || it.type == "ADVANCED_LIVE" } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val premiumWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getWallpapers()
        .map { list -> list.filter { it.isPremium } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {"""

content = content.replace("    init {", new_flows)

with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'w') as f:
    f.write(content)
