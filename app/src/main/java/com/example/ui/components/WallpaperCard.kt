package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.models.Wallpaper
import com.example.ui.theme.ChampagnePrimary

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    modifier: Modifier = Modifier,
    showTitleOverlay: Boolean = true,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isLive = wallpaper.type == "LIVE" || wallpaper.type == "ADVANCED_LIVE"
    val cornerRadius = 16.dp
    val shape = RoundedCornerShape(cornerRadius)

    // Extract primary tag and icon
    val (primaryTag, tagIcon, tagColor) = remember(wallpaper) {
        val cat = wallpaper.categoryIds.firstOrNull { it.isNotBlank() }?.trim()
        when {
            wallpaper.isTrending -> Triple("Trending", Icons.Default.LocalFireDepartment, Color(0xFFFF5252))
            cat?.equals("nature", ignoreCase = true) == true -> Triple("Nature", Icons.Default.Park, Color(0xFF4CAF50))
            cat?.equals("cars", ignoreCase = true) == true || cat?.equals("car", ignoreCase = true) == true -> Triple("Cars", Icons.Default.Speed, Color(0xFFFF9800))
            cat?.equals("anime", ignoreCase = true) == true -> Triple("Anime", Icons.Default.AutoAwesome, Color(0xFFE040FB))
            cat?.equals("abstract", ignoreCase = true) == true -> Triple("Abstract", Icons.Default.Layers, Color(0xFF00E5FF))
            isLive -> Triple("Live", Icons.Default.Videocam, Color(0xFFFF3366))
            !cat.isNullOrBlank() -> Triple(cat.replaceFirstChar { it.uppercase() }, Icons.Default.AutoAwesome, Color(0xFF9E7D48))
            wallpaper.hasHomeTransition || wallpaper.hasLockAnimation -> Triple("Double", Icons.Default.Smartphone, Color(0xFF3897F0))
            else -> Triple("HD", Icons.Default.AutoAwesome, Color(0xFF60A5FA))
        }
    }

    // Subtitle category text
    val subtitleText = remember(wallpaper) {
        val cat = wallpaper.categoryIds.firstOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
        val typeLabel = if (isLive) "Live" else "HD"
        if (!cat.isNullOrBlank()) "$typeLabel • $cat" else if (!wallpaper.description.isNullOrBlank()) "$typeLabel • ${wallpaper.description}" else "$typeLabel • Dynamic"
    }

    val context = LocalContext.current
    val imageRequest = remember(wallpaper.thumbnailUrl, wallpaper.imageUrl) {
        ImageRequest.Builder(context)
            .data(wallpaper.thumbnailUrl.ifEmpty { wallpaper.imageUrl })
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF16161E))
            .border(
                width = 0.6.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        // Wallpaper Image / Thumbnail
        AsyncImage(
            model = imageRequest,
            contentDescription = wallpaper.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top-left Category / Status pill badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = tagIcon,
                    contentDescription = null,
                    tint = tagColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = primaryTag,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Top-right indicator badges (Live / VIP / Sound)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (wallpaper.isPremium) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F2038), Color(0xFF0A1628))
                            )
                        )
                        .border(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIP",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Bottom Gradient & Metadata Overlay
        if (showTitleOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(start = 12.dp, end = 6.dp, top = 20.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = wallpaper.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Interactive Favorite Heart Button
                    val isFav = wallpaper.isFavorite
                    val heartColor by animateColorAsState(
                        targetValue = if (isFav) Color(0xFFFF2A55) else Color.White.copy(alpha = 0.85f),
                        label = "heartColor"
                    )

                    IconButton(
                        onClick = { onFavoriteToggle?.invoke() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                            tint = heartColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
