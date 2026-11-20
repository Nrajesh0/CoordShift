package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.CooldownTier
import com.example.ui.theme.CooldownSemanticPalette
import com.example.ui.theme.LocalCooldownPalette
import com.example.ui.theme.MonoStyles

fun CooldownSemanticPalette.accentFor(tier: CooldownTier): Color = when (tier) {
    CooldownTier.READY, CooldownTier.FAST -> fast
    CooldownTier.MODERATE -> moderate
    CooldownTier.LONG -> long
    CooldownTier.MAX -> max
}

fun CooldownSemanticPalette.containerFor(tier: CooldownTier): Color = when (tier) {
    CooldownTier.READY, CooldownTier.FAST -> fastContainer
    CooldownTier.MODERATE -> moderateContainer
    CooldownTier.LONG -> longContainer
    CooldownTier.MAX -> maxContainer
}

@Composable
fun StatusPillBadge(
    text: String,
    accentColor: Color,
    containerColor: Color,
    showDot: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            style = MonoStyles.badge,
            color = accentColor,
            maxLines = 1
        )
    }
}

@Composable
fun CooldownSeverityBar(
    cooldownSeconds: Int,
    tier: CooldownTier,
    modifier: Modifier = Modifier
) {
    val palette = LocalCooldownPalette.current
    val fraction = (cooldownSeconds / 7200f).coerceIn(0f, 1f)
    val accent = palette.accentFor(tier)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.hairline.copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
                .size(width = (88 * fraction).coerceAtLeast(if (cooldownSeconds > 0) 6f else 0f).dp, height = 4.dp)
        )
    }
}
