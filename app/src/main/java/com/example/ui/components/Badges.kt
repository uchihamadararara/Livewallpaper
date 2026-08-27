package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinematicBlueAccent
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.IceBlueText

@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        Box(
            modifier = modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .border(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Live",
                tint = CyanHighlight,
                modifier = Modifier.size(12.dp)
            )
        }
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF09111E).copy(alpha = 0.85f))
                .border(0.5.dp, Color(0xFF2563EB).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Live",
                tint = CyanHighlight,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "LIVE",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun PremiumBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        Box(
            modifier = modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(ElectricBluePrimary, Color(0xFF1D4ED8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "Premium",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    } else {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0F2038), Color(0xFF0A1628)))
                )
                .border(0.5.dp, Color(0xFF3B82F6).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "VIP",
                tint = CyanHighlight,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "VIP",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun AudioBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "Audio Included",
            tint = CyanHighlight,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "AUDIO",
            color = IceBlueText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ChargingBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D1B2E).copy(alpha = 0.85f))
            .border(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "Charging Effect",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = "CHARGING",
            color = Color(0xFFBAE6FD),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
    }
}

