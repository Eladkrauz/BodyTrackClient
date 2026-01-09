////////////////////////////////////////////////////////////
////////////// BODY TRACK // CLIENT // THEME ///////////////
////////////////////////////////////////////////////////////
/////////////////////// FILE: Theme ////////////////////////
////////////////////////////////////////////////////////////
package com.bodytrack.client.theme

///////////////
/// IMPORTS ///
///////////////
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Color schemes.
private val LightColorScheme = lightColorScheme(
    primary = BodyTrackBlue,
    onPrimary = Color.White,

    primaryContainer = BodyTrackBlueContainer,
    onPrimaryContainer = BodyTrackBlueDark,

    secondary = BodyTrackBlueLight,
    onSecondary = Color.White,

    background = BackgroundLight,
    onBackground = Color.Black,

    surface = CardWhite,
    onSurface = Color.Black,

    surfaceTint = BodyTrackBlue,
    outline = OutlineGray,
    inversePrimary = BodyTrackBlueDark
)

private val DarkColorScheme = darkColorScheme(
    primary = BodyTrackBlueLight,
    onPrimary = Color.Black,

    primaryContainer = BodyTrackBlueContainerDark,
    onPrimaryContainer = Color.White,

    secondary = BodyTrackBlue,
    onSecondary = Color.White,

    background = Color.Black,
    onBackground = Color.White,

    surface = Color(0xFF121212),
    onSurface = Color.White,

    surfaceTint = BodyTrackBlueLight,
    outline = OutlineGray,
    inversePrimary = BodyTrackBlue
)

////////////////////////
/// BODY TRACK THEME ///
////////////////////////
/**
 * The main theme for the BodyTrack application.
 *
 * This Composable function applies the MaterialTheme to the entire app, providing a consistent
 * look and feel. It handles switching between light and dark color schemes based on the system
 * settings or a provided parameter.
 *
 * @param darkTheme Whether to use the dark theme. Defaults to the system's dark theme setting.
 * @param content The Composable content to be themed.
 */
@Composable
fun BodyTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme =
        if (darkTheme) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}