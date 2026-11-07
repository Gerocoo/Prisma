/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.spotube.millennium.composables.MillenniumTextTheme
import org.spotube.millennium.composables.MillenniumBottomNavigationBarTheme
import org.spotube.millennium.composables.MillenniumBottomNavigationItemThemeProperties
import org.spotube.millennium.composables.defaultMillenniumBottomNavigationBarTheme
import org.spotube.millennium.theme.resolve
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumControlStateProperty
import org.spotube.millennium.theme.MillenniumOrangeLightColors
import org.spotube.millennium.theme.MillenniumThemeComponents
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.figtree_bold
import spotube.composeapp.generated.resources.figtree_extrabold
import spotube.composeapp.generated.resources.figtree_medium
import spotube.composeapp.generated.resources.figtree_regular
import spotube.composeapp.generated.resources.figtree_semibold

import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.millennium.composables.MillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.MillenniumTooltipHost
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.MillenniumTopAppBarThemeProperties
import org.spotube.millennium.composables.defaultMillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.defaultMillenniumTopAppBarTheme
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumLightThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeConfig

/**
 * Semantic color roles used across the app, resolved from the active Millennium palette.
 *
 * This is not a second palette: every role is derived from [MillenniumColors], so the
 * whole app follows the selected Millennium accent family and light/dark appearance.
 */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color
)

/** Maps Millennium's semantic palette onto the app's role names. */
fun appColorsFrom(m: MillenniumColors): AppColors = AppColors(
    primary = m.accent,
    onPrimary = m.onAccent,
    primaryContainer = m.raised,
    onPrimaryContainer = m.text,
    secondary = m.accent,
    onSecondary = m.onAccent,
    secondaryContainer = m.raised,
    onSecondaryContainer = m.text,
    tertiaryContainer = m.raised,
    onTertiaryContainer = m.text,
    background = m.background,
    onBackground = m.text,
    surface = m.surface,
    onSurface = m.text,
    surfaceVariant = m.inset,
    onSurfaceVariant = m.muted,
    surfaceContainer = m.surface,
    surfaceContainerHigh = m.raised,
    surfaceContainerHighest = m.raised,
    inverseSurface = m.text,
    inverseOnSurface = m.background,
    outline = m.border,
    outlineVariant = m.border,
    error = m.danger,
    onError = m.background,
    errorContainer = m.danger.copy(alpha = 0.16f),
    onErrorContainer = m.danger,
    scrim = m.scrim
)

/** Named text styles used by the app, independent of Material typography. */
@Immutable
data class AppTypography(
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

private fun style(
    family: FontFamily,
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: Float = 0f
): TextStyle = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = letterSpacing.em
)

/**
 * Typography scale. Headings are heavy and slightly tightened, body text is regular: the
 * hierarchy comes from weight more than from size.
 */
fun appTypography(family: FontFamily) = AppTypography(
    displaySmall = style(family, 34, 40, FontWeight.ExtraBold, -0.02f),
    headlineLarge = style(family, 30, 36, FontWeight.ExtraBold, -0.02f),
    headlineMedium = style(family, 26, 32, FontWeight.ExtraBold, -0.02f),
    headlineSmall = style(family, 22, 28, FontWeight.ExtraBold, -0.01f),
    titleLarge = style(family, 20, 26, FontWeight.Bold, -0.01f),
    titleMedium = style(family, 16, 22, FontWeight.SemiBold),
    titleSmall = style(family, 14, 20, FontWeight.SemiBold),
    bodyLarge = style(family, 16, 24),
    bodyMedium = style(family, 14, 20),
    bodySmall = style(family, 12, 16),
    labelLarge = style(family, 14, 20, FontWeight.SemiBold),
    labelMedium = style(family, 12, 16, FontWeight.Medium),
    labelSmall = style(family, 11, 16, FontWeight.Medium)
)

val AppDefaultTypography = appTypography(FontFamily.SansSerif)

/** Figtree (SIL Open Font License, see assets/licenses/Figtree-OFL.txt), bundled as static weights. */
@Composable
fun rememberAppFontFamily(): FontFamily {
    val regular = Font(Res.font.figtree_regular, FontWeight.Normal)
    val medium = Font(Res.font.figtree_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.figtree_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.figtree_bold, FontWeight.Bold)
    val extraBold = Font(Res.font.figtree_extrabold, FontWeight.ExtraBold)
    return remember(regular, medium, semiBold, bold, extraBold) {
        FontFamily(regular, medium, semiBold, bold, extraBold)
    }
}

/** Millennium's text roles on the app font, so library components match the rest of the UI. */
private fun MillenniumThemeComponents.withFont(family: FontFamily): MillenniumThemeComponents {
    val base = textTheme.states.default
    val themed = base.copy(
        body = base.body.copy(fontFamily = family),
        label = base.label.copy(fontFamily = family),
        button = base.button.copy(fontFamily = family, fontWeight = FontWeight.Bold),
        title = base.title.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        headline = base.headline.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        caption = base.caption.copy(fontFamily = family),
        avatar = base.avatar.copy(fontFamily = family)
    )
    return copy(textTheme = MillenniumTextTheme(MillenniumControlStateProperty.all(themed)))
}

/**
 * Flat bottom navigation: solid background, no gel on the selected item; the selected tab is
 * bright text, the others muted. A focus ring is still drawn for keyboard users.
 */
private fun flatBottomNavigationTheme(colors: MillenniumColors): MillenniumBottomNavigationBarTheme {
    val base = defaultMillenniumBottomNavigationBarTheme()
    val clear = SolidColor(Color.Transparent)
    val bar = base.states.default.copy(
        background = SolidColor(colors.background),
        shape = RectangleShape,
        borderColor = Color.Transparent,
        borderWidth = 0.dp,
        shadowElevation = 0.dp,
        innerGlowColor = Color.Transparent
    )
    fun MillenniumBottomNavigationItemThemeProperties.flat(selected: Boolean, focused: Boolean = false) = copy(
        background = clear,
        foreground = if (selected) colors.text else colors.muted,
        borderColor = if (focused) null else Color.Transparent,
        shadowElevation = 0.dp,
        innerGlowColor = Color.Transparent
    )
    val item = base.item
    return base.copy(
        states = MillenniumControlStateProperty.all(bar),
        item = item.copy(
            default = item.default.flat(selected = false),
            hovered = item.hovered.flat(selected = false).copy(foreground = colors.text),
            focused = item.focused.flat(selected = false, focused = true),
            pressed = item.pressed.flat(selected = false),
            disabled = item.disabled.flat(selected = false),
            selected = item.selected.flat(selected = true)
        )
    )
}

/**
 * Dark surfaces shared by every accent family: near-black ground, softly lifted cards, no
 * tinted chrome. Accents are flat (top = bottom) instead of Millennium's glossy gel.
 */
private fun darkSurface(accent: Color, onAccent: Color) = MillenniumColors(
    background = Color(0xFF0F0F10), surface = Color(0xFF18181B), raised = Color(0xFF26262A),
    inset = Color(0xFF141416), text = Color(0xFFF4F4F5), muted = Color(0xFFA1A1AA),
    border = Color(0xFF2E2E33), highlight = Color(0xFF3A3A40), accent = accent,
    accentTop = accent, accentBottom = accent, onAccent = onAccent,
    success = Color(0xFF86EFAC), danger = Color(0xFFFF8A80), scrim = Color(0xCC050505)
)

/** Amber is the default (the "Orange" family slot); blue and lime keep their own accents. */
val AppColorSchemes = MillenniumColorSchemes(
    dark = darkSurface(accent = Color(0xFF7C8CFF), onAccent = Color(0xFF0B0B1A)),
    orangeDark = darkSurface(accent = Color(0xFFF5A524), onAccent = Color(0xFF111111)),
    orangeLight = MillenniumOrangeLightColors.copy(
        accent = Color(0xFF9A5B00), accentTop = Color(0xFFF5A524), accentBottom = Color(0xFFF5A524),
        onAccent = Color(0xFF111111)
    ),
    limeDark = darkSurface(accent = Color(0xFF2DD4BF), onAccent = Color(0xFF04201C))
)

/** Corner shapes used by the app. */
@Immutable
data class AppShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape
)

val AppDefaultShapes = AppShapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

val LocalAppColors =
    staticCompositionLocalOf { appColorsFrom(org.spotube.millennium.theme.MillenniumLightColors) }
val LocalAppTypography = staticCompositionLocalOf { AppDefaultTypography }
val LocalAppShapes = staticCompositionLocalOf { AppDefaultShapes }

/** Reads the active app theme tokens, all backed by Millennium. */
object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val typography: AppTypography @Composable get() = LocalAppTypography.current
    val shapes: AppShapes @Composable get() = LocalAppShapes.current
}

/**
 * The app's single source of truth for colour.
 *
 * Installs [MillenniumTheme] with the user's selected [UserSettings.palette] and light/dark
 * [UserSettings.theme], then derives the app's semantic [AppColors], [AppTypography] and
 * [AppShapes] tokens from the active Millennium theme.
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemDarkTheme()
    }
    val palette: MillenniumPalette = settings.palette
    // Material You: when enabled and available, the system palette replaces every accent family.
    val systemDark = if (settings.useSystemColors) systemDynamicColors(dark = true) else null
    val systemLight = if (settings.useSystemColors) systemDynamicColors(dark = false) else null
    val colorSchemes = remember(systemDark, systemLight) {
        if (systemDark != null && systemLight != null) {
            MillenniumColorSchemes(
                light = systemLight, dark = systemDark,
                orangeLight = systemLight, orangeDark = systemDark,
                limeLight = systemLight, limeDark = systemDark
            )
        } else AppColorSchemes
    }
    val fontFamily = rememberAppFontFamily()
    val typography = remember(fontFamily) { appTypography(fontFamily) }

    val darkTheme = remember(fontFamily, palette, colorSchemes) {
        MillenniumDarkThemeComponents.withFont(fontFamily).copy(
            bottomNavigationBarTheme = flatBottomNavigationTheme(colorSchemes.resolve(palette, dark = true))
        )
    }
    val lightTheme = remember(fontFamily, palette, colorSchemes) {
        MillenniumLightThemeComponents.withFont(fontFamily).copy(
            bottomNavigationBarTheme = flatBottomNavigationTheme(colorSchemes.resolve(palette, dark = false))
        )
    }

    MillenniumTheme(
        dark = isDarkTheme,
        config = MillenniumThemeConfig(
            colorSchemes = colorSchemes,
            palette = palette,
            dark = darkTheme.copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = darkTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                ),
                modalBottomSheetTheme = compactModalBottomSheetTheme()
            ),
            light = lightTheme.copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = lightTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                ),
                modalBottomSheetTheme = compactModalBottomSheetTheme()
            )
        )
    ) {
        val colors = appColorsFrom(MillenniumTheme.colors)
        MillenniumTooltipHost {
            CompositionLocalProvider(
                LocalAppColors provides colors,
                LocalAppTypography provides typography,
                LocalAppShapes provides AppDefaultShapes,
                content = content
            )
        }
    }
}

/** Library default is 24dp padding + 16dp spacing, which is far too roomy on phones. */
private fun compactModalBottomSheetTheme(): MillenniumModalBottomSheetTheme {
    val padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
    fun org.spotube.millennium.composables.MillenniumModalBottomSheetThemeProperties.compact() =
        copy(contentPadding = padding, contentSpacing = 8.dp)
    val states = defaultMillenniumModalBottomSheetTheme().states
    return MillenniumModalBottomSheetTheme(
        states = org.spotube.millennium.theme.MillenniumControlStateProperty(
            default = states.default.compact(),
            hovered = states.hovered.compact(),
            focused = states.focused.compact(),
            pressed = states.pressed.compact(),
            disabled = states.disabled.compact(),
            selected = states.selected.compact(),
            error = states.error.compact()))
}

/** Whether the active Millennium appearance is light. */
@Composable
fun isLightAppearance(): Boolean = MillenniumTheme.colors.background.luminance() > 0.5f
