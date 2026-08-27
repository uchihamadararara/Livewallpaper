package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class BatteryChargingState(
    val isCharging: Boolean = false,
    val batteryPercent: Int = 0,
    val chargingSource: String = "Standard"
)

/**
 * Lifecycle-aware Compose hook to monitor Android hardware charging state and battery percentage.
 * Uses standard AOSP BatteryManager & system BroadcastReceivers.
 */
@Composable
fun rememberBatteryChargingState(): State<BatteryChargingState> {
    val context = LocalContext.current
    val state = remember {
        mutableStateOf(getCurrentBatteryChargingState(context))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        // Immediately update charging state while preserving the valid battery percentage
                        val previousPercent = state.value.batteryPercent
                        val current = getCurrentBatteryChargingState(context)
                        val finalPercent = if (current.batteryPercent in 0..100) {
                            current.batteryPercent
                        } else {
                            previousPercent
                        }
                        state.value = current.copy(
                            isCharging = true,
                            batteryPercent = finalPercent
                        )
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        val previousPercent = state.value.batteryPercent
                        val current = getCurrentBatteryChargingState(context)
                        val finalPercent = if (current.batteryPercent in 0..100) {
                            current.batteryPercent
                        } else {
                            previousPercent
                        }
                        state.value = current.copy(
                            isCharging = false,
                            batteryPercent = finalPercent
                        )
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        state.value = parseBatteryIntent(intent)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    return state
}

fun getCurrentBatteryChargingState(context: Context): BatteryChargingState {
    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    // 1. Synchronously query hardware capacity if supported (API 21+)
    val hardwareCapacity = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    val isHardwareValid = hardwareCapacity in 0..100

    // 2. Query sticky ACTION_BATTERY_CHANGED broadcast
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    if (intent != null) {
        val parsed = parseBatteryIntent(intent)
        // If parsed has valid percent, use it; otherwise use hardware capacity if valid
        val finalPercent = if (parsed.batteryPercent in 0..100) {
            parsed.batteryPercent
        } else if (isHardwareValid) {
            hardwareCapacity
        } else {
            -1
        }
        return parsed.copy(batteryPercent = finalPercent)
    }

    // 3. Fallback when sticky broadcast is not yet ready: check BatteryManager charging state & capacity
    val isCharging = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        batteryManager?.isCharging == true
    } else {
        false
    }

    return BatteryChargingState(
        isCharging = isCharging,
        batteryPercent = if (isHardwareValid) hardwareCapacity else -1,
        chargingSource = if (isCharging) "Power Adapter" else "Battery"
    )
}

fun parseBatteryIntent(intent: Intent): BatteryChargingState {
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val percent = if (level >= 0 && scale > 0) {
        (level * 100) / scale
    } else {
        -1
    }

    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
    val source = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> if (isCharging) "Power Adapter" else "Battery"
    }

    return BatteryChargingState(
        isCharging = isCharging,
        batteryPercent = percent,
        chargingSource = source
    )
}
