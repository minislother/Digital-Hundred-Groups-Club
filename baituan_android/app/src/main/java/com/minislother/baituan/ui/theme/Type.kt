package com.minislother.baituan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BaituanTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        ),
        titleLarge = titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        ),
        titleMedium = titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        ),
        bodyMedium = bodyMedium.copy(
            fontSize = 14.sp
        )
    )
}
