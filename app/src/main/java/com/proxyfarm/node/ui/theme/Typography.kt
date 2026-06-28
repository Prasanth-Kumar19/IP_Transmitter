package com.proxyfarm.node.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    titleLarge  = TextStyle(FontFamily.Default, FontWeight.Bold,     22.sp, 28.sp),
    titleMedium = TextStyle(FontFamily.Default, FontWeight.SemiBold, 16.sp, 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(FontFamily.Default, FontWeight.Medium,   14.sp, 20.sp, letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(FontFamily.Default, FontWeight.Normal,   16.sp, 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(FontFamily.Default, FontWeight.Normal,   14.sp, 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(FontFamily.Default, FontWeight.Normal,   12.sp, 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(FontFamily.Default,   FontWeight.Medium, 14.sp, 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(FontFamily.Default,   FontWeight.Medium, 12.sp, 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(FontFamily.Monospace, FontWeight.Medium, 11.sp, 16.sp, letterSpacing = 0.5.sp)
)
