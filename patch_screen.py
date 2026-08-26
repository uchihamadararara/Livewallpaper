import re

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
"""        factory = ViewModelFactory(
            extraId = wallpaperId,
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
        )""",
"""        factory = ViewModelFactory(
            extraId = wallpaperId,
            wallpaperRepository = AppContainer.getWallpaperRepository(context),
            userRepository = AppContainer.userRepository,
            userPreferencesRepository = AppContainer.getUserPreferencesRepository(context),
            authUserId = kotlinx.coroutines.runBlocking { com.example.di.AppContainer.authRepositoryImpl.getUserId() }
        )"""
)

# Insert the AlertDialog right after Scaffold
alert_dialog_code = """    if (applyState is ApplyState.ShowSoundPrompt) {
        val wp = (applyState as ApplyState.ShowSoundPrompt).wallpaper
        AlertDialog(
            onDismissRequest = { viewModel.resetApplyState() },
            title = { Text("Live Wallpaper Sound", fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to enable audio playback for this live wallpaper when it's applied?") },
            confirmButton = {
                Button(onClick = { viewModel.onSoundPreferenceSelected(wp, true) }) {
                    Text("Turn Sound ON")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onSoundPreferenceSelected(wp, false) }) {
                    Text("Turn Sound OFF")
                }
            }
        )
    }

    Scaffold("""

content = content.replace("    Scaffold(", alert_dialog_code)

with open('app/src/main/java/com/example/ui/wallpapers/WallpaperDetailScreen.kt', 'w') as f:
    f.write(content)
