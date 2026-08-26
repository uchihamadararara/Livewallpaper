package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.di.AppContainer
import com.example.di.ViewModelFactory
import com.example.util.OemHelper

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(
            userPreferencesRepository = AppContainer.getUserPreferencesRepository(context)
        )
    )
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val isChargingAnimationEnabled by viewModel.isChargingAnimationEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Account Section
        Text("ACCOUNT", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text("Guest User", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Sign in to sync your favorites across devices.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* Google Sign-In */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Sign in with Google", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Subscription Section
        Text("SUBSCRIPTION", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(16.dp)
        ) {
            Text("Free Plan", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text("Upgrade to Premium for exclusive Live Wallpapers and no ads.", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("premium") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Get Premium", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Wallpaper Preferences & OEM Guidance Section
        Text("WALLPAPER & PLAYBACK", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Sound",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Live Wallpaper Sound",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Enable audio for wallpapers supporting sound (default: off)",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = isSoundEnabled,
                    onCheckedChange = { viewModel.toggleSound(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = "Charging Animation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            "Charging Animation",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Show subtle luxury charging visual when plugged in",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp
                        )
                    }
                }
                Switch(
                    checked = isChargingAnimationEnabled,
                    onCheckedChange = { viewModel.toggleChargingAnimation(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Device Background Activity",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "On ${OemHelper.getManufacturerName()} devices, adjust battery management if live wallpapers pause unexpectedly.",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            }

            OutlinedButton(
                onClick = { OemHelper.openBatteryOptimizationSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Battery Optimization Settings")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
