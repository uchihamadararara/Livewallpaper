package com.example.ui.charging

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BatteryChargingState
import kotlinx.coroutines.delay

/**
 * Luxury, calm, minimalist charging animation.
 * Features an obsidian dark canvas, refined champagne gold accents,
 * smooth breathing energy rings, and editorial typography.
 */
@Composable
fun ChargingAnimationOverlay(
    chargingState: BatteryChargingState,
    isEnabled: Boolean,
    onDismissRequest: () -> Unit = {}
) {
    // Show only if charging is active AND the user has charging animation enabled
    val shouldShow = isEnabled && chargingState.isCharging

    // Temporary user dismissal state for the current charging session
    var userDismissedThisSession by remember(chargingState.isCharging) { mutableStateOf(false) }

    val isVisible = shouldShow && !userDismissedThisSession

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 400, easing = FastOutLinearInEasing)) +
                scaleOut(targetScale = 0.98f, animationSpec = tween(durationMillis = 400, easing = FastOutLinearInEasing))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "charging_luxury_pulse")

        // Smooth breathing rotation
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring_rotation"
        )

        // Gentle breathing pulse
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.75f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        val ringScale by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_scale"
        )

        val goldAccent = Color(0xFFE5C07B)
        val goldSoft = Color(0xFFD4AF37)
        val deepBackground = Color(0xFF09090C).copy(alpha = 0.96f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(deepBackground)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    userDismissedThisSession = true
                    onDismissRequest()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                // Central Ring & Indicator
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer subtle energy aura
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.5.dp.toPx()
                        val sweep = 280f
                        val progressSweep = if (chargingState.batteryPercent in 0..100) {
                            (chargingState.batteryPercent / 100f) * 360f
                        } else {
                            0f
                        }

                        // Background Track
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = (size.minDimension / 2f) - (strokeWidth / 2f),
                            style = Stroke(width = strokeWidth)
                        )

                        // Ambient Soft Halo Arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    goldSoft.copy(alpha = 0.05f),
                                    goldAccent.copy(alpha = pulseAlpha * 0.4f),
                                    goldSoft.copy(alpha = 0.05f)
                                )
                            ),
                            startAngle = rotationAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth * 2.5f, cap = StrokeCap.Round),
                            size = Size(size.width - strokeWidth * 2, size.height - strokeWidth * 2),
                            topLeft = Offset(strokeWidth, strokeWidth)
                        )

                        // Precision Battery Arc
                        drawArc(
                            color = goldAccent,
                            startAngle = -90f,
                            sweepAngle = progressSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Inner Core
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Power Icon badge
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(goldAccent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Charging",
                                tint = goldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Percentage Number
                        val displayPercent = if (chargingState.batteryPercent in 0..100) {
                            "${chargingState.batteryPercent}%"
                        } else {
                            "--"
                        }

                        Text(
                            text = displayPercent,
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = (-1).sp
                        )

                        // Charging Status Label
                        Text(
                            text = "CHARGING",
                            color = goldAccent.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Source pill (e.g. "FAST AC • CONNECTED")
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${chargingState.chargingSource.uppercase()} POWER",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Tap anywhere to return",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
