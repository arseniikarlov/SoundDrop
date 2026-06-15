package com.alfa.shakegroan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.alfa.shakegroan.R

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    Font(R.font.nunito_black, FontWeight.Black),
)

val ShakeGroanTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 54.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NunitoFontFamily,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
    ),
)
