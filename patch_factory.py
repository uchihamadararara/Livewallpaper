with open('app/src/main/java/com/example/di/ViewModelFactory.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        if (modelClass.isAssignableFrom(WallpaperDetailViewModel::class.java)) {
            return WallpaperDetailViewModel(extraId!!, wallpaperRepository!!, userRepository!!, authUserId) as T
        }""",
"""        if (modelClass.isAssignableFrom(WallpaperDetailViewModel::class.java)) {
            return WallpaperDetailViewModel(extraId!!, wallpaperRepository!!, userRepository!!, userPreferencesRepository!!, authUserId) as T
        }"""
)

with open('app/src/main/java/com/example/di/ViewModelFactory.kt', 'w') as f:
    f.write(content)
