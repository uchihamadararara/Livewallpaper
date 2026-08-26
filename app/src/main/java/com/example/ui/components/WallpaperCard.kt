package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.models.Wallpaper
import com.example.ui.theme.premiumClickable

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
            .premiumClickable(onClick = onClick)
    ) {
        AsyncImage(
            model = wallpaper.thumbnailUrl,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Dark gradient overlay at the bottom for better text contrast if we had text, but we keep it clean.
        
        // Badges overlay
        Row(
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isLive = wallpaper.type == "LIVE" || wallpaper.type == "ADVANCED_LIVE"
            if (isLive) {
                SmallIconBadge(Icons.Default.PlayArrow, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            }
            if (wallpaper.isPremium) {
                SmallIconBadge(Icons.Default.Star, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
fun SmallIconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
    }
}
