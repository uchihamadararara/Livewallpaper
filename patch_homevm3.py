with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'r') as f:
    content = f.read()

new_flows = """
    val favoriteWallpapers: StateFlow<List<Wallpaper>> = wallpaperRepository.getFavoriteWallpapers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {"""

content = content.replace("    init {", new_flows)

with open('app/src/main/java/com/example/ui/home/HomeViewModel.kt', 'w') as f:
    f.write(content)
