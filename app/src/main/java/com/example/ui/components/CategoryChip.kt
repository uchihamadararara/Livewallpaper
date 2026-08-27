package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricBlueHover
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(
            listOf(
                ElectricBluePrimary,
                ElectricBlueHover
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                DarkSurface,
                DarkSurface
            )
        )
    }

    val borderColor = if (isSelected) {
        Color(0xFF60A5FA).copy(alpha = 0.6f)
    } else {
        DarkBorder
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        TextSecondary
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(width = 0.8.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

