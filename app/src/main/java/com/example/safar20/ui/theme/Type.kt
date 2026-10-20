package com.example.safar20.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.safar20.R

// ============================================================================
// SAFAR 2.0 — ROYAL ENFIELD RETRO-MOTO TYPOGRAPHY & FONTS
// ============================================================================

val BebasNeue = FontFamily(
    Font(R.font.bebas_neue_regular, FontWeight.Normal)
)

val Rajdhani = FontFamily(
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium)
)

val Typography = Typography(
    // Speedometer Numerals & Odometer Counters (BebasNeue)
    displayLarge = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = BebasNeue,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),

    // Cockpit Section Headers (Rajdhani SemiBold, letterSpacing = 1.5.sp)
    titleLarge = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.5.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 1.5.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Rajdhani,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.5.sp
    ),

    // Body & Labels (Inter)
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)
