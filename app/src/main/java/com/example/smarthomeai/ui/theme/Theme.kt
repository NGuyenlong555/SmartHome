package com.example.smarthomeai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Blue700 = Color(0xFF0B5ED7)
val Blue600 = Color(0xFF1565D8)
val Blue500 = Color(0xFF2F80ED)
val Blue300 = Color(0xFF5FA2F5)
val Blue50 = Color(0xFFEAF2FF)
val Bg = Color(0xFFF4F7FC)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1C2433)
val TextGray = Color(0xFF7B8794)
val OffGray = Color(0xFFEEF1F6)
val GreenOk = Color(0xFF22B573)
val GreenSoft = Color(0xFFE6F8EE)
val RedAlert = Color(0xFFE53935)
val RedSoft = Color(0xFFFDE7E7)
val OrangeWarn = Color(0xFFF59E0B)
val YellowSoft = Color(0xFFFFF4D6)
val BlueSoft = Color(0xFFE3EEFF)
val PurpleSoft = Color(0xFFEDE9FE)
val Purple = Color(0xFF6C5CE7)
val TempSoft = Color(0xFFFDE5E0)
val HumSoft = Color(0xFFDCEAFE)

val HeaderBrush = Brush.verticalGradient(listOf(Blue700, Blue600))
val LoginBrush = Brush.verticalGradient(listOf(Blue600, Blue300))

private val Scheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    background = Bg,
    surface = CardWhite,
    onSurface = TextDark,
    secondary = Blue500
)

@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
