package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinematicBlueAccent
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DarkBackgroundAlt
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlueHover
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class WallpaperSortOrder {
    LATEST,
    POPULAR,
    ALPHABETICAL
}

enum class WallpaperTypeFilter {
    ALL,
    LIVE_ONLY,
    STATIC_ONLY,
    DOUBLE_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentSort: WallpaperSortOrder,
    currentTypeFilter: WallpaperTypeFilter,
    onlyPremium: Boolean,
    onSortChange: (WallpaperSortOrder) -> Unit,
    onTypeFilterChange: (WallpaperTypeFilter) -> Unit,
    onPremiumFilterChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackgroundAlt,
        contentColor = Color.White,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyanHighlight,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Filter & Sort",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        onSortChange(WallpaperSortOrder.POPULAR)
                        onTypeFilterChange(WallpaperTypeFilter.ALL)
                        onPremiumFilterChange(false)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // Sort Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SORT BY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptionChip(
                        label = "Trending",
                        isSelected = currentSort == WallpaperSortOrder.POPULAR,
                        onClick = { onSortChange(WallpaperSortOrder.POPULAR) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterOptionChip(
                        label = "Latest",
                        isSelected = currentSort == WallpaperSortOrder.LATEST,
                        onClick = { onSortChange(WallpaperSortOrder.LATEST) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterOptionChip(
                        label = "Name",
                        isSelected = currentSort == WallpaperSortOrder.ALPHABETICAL,
                        onClick = { onSortChange(WallpaperSortOrder.ALPHABETICAL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Type Filter Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WALLPAPER TYPE",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptionChip(
                        label = "All",
                        isSelected = currentTypeFilter == WallpaperTypeFilter.ALL,
                        onClick = { onTypeFilterChange(WallpaperTypeFilter.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterOptionChip(
                        label = "Live",
                        isSelected = currentTypeFilter == WallpaperTypeFilter.LIVE_ONLY,
                        onClick = { onTypeFilterChange(WallpaperTypeFilter.LIVE_ONLY) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterOptionChip(
                        label = "Double",
                        isSelected = currentTypeFilter == WallpaperTypeFilter.DOUBLE_ONLY,
                        onClick = { onTypeFilterChange(WallpaperTypeFilter.DOUBLE_ONLY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // VIP / Premium Filter Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIP Wallpapers Only",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Show exclusive high definition artworks",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = onlyPremium,
                    onCheckedChange = { onPremiumFilterChange(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricBluePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    )
                )
            }

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBluePrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Apply Filter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                color = if (isSelected) ElectricBluePrimary else DarkSurface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 0.8.dp,
                color = if (isSelected) Color(0xFF60A5FA).copy(alpha = 0.6f) else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

