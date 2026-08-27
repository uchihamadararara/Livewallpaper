package com.example.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun StartupLoadingScreen(
    onLoadingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0.01f) }

    LaunchedEffect(Unit) {
        // Smooth, cinematic acceleration-deceleration curve from 1% to 100%
        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 2200,
                easing = CubicBezierEasing(0.2f, 0.0f, 0.2f, 1.0f)
            )
        )
        delay(120)
        onLoadingComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)),
        contentAlignment = Alignment.Center
    ) {
        // Main Artwork - Preserved exactly as provided
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Centered High-Fidelity Character & Domain Artwork
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Startup Screen Artwork",
                    modifier = Modifier
                        .size(310.dp)
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Cinematic Minimalist Loading Indicator at Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentPercent = (progress.value * 100).toInt().coerceIn(1, 100)

                // Sleek, glowing electric-cyan progress track
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF0F172A))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.value)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF0284C7),
                                        Color(0xFF00F0FF),
                                        Color(0xFF67E8F9)
                                    )
                                )
                            )
                            .shadow(
                                elevation = 4.dp,
                                spotColor = Color(0xFF00F0FF),
                                ambientColor = Color(0xFF00F0FF)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Numerical Progress Percentage (1% -> 100%)
                Text(
                    text = "$currentPercent%",
                    color = Color(0xFF67E8F9),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
