package com.example.safar20.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.WarmIvory

/**
 * Retro-cockpit styled card with GunmetalGauge background, 1.dp BronzeRim border,
 * 16.dp rounded corners, and 16.dp inner padding.
 */
@Composable
fun BrassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.border(1.dp, BronzeRim, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = GunmetalGauge
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Uppercase section header label with Rajdhani font and RoyalBrass vertical bar accent.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .background(RoyalBrass, shape = RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text.uppercase(),
            fontFamily = Rajdhani,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            color = MutedPewter
        )
    }
}

/**
 * Compact stat display card for cockpit metrics with BebasNeue value and Rajdhani label.
 */
@Composable
fun BrassStatBadge(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    BrassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                fontFamily = BebasNeue,
                fontSize = 28.sp,
                lineHeight = 32.sp,
                color = WarmIvory
            )
            Text(
                text = label.uppercase(),
                fontFamily = Rajdhani,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = MutedPewter
            )
        }
    }
}

/**
 * Custom retro pill toggle selector.
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(1.dp, BronzeRim, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(CrankcaseBlack)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) RoyalBrass else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.uppercase(),
                        fontFamily = Rajdhani,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = if (isSelected) CrankcaseBlack else MutedPewter
                    )
                }
            }
        }
    }
}
