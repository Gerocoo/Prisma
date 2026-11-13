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

import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import org.spotube.millennium.composables.MillenniumButtonThemeProperties
import org.spotube.millennium.composables.MillenniumIconButtonThemeProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
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
        // Transparent at the top, solid page color at the bottom: content fades under the bar.
        background = Brush.verticalGradient(
            0f to colors.background.copy(alpha = 0f),
            0.55f to colors.background.copy(alpha = 0.92f),
            1f to colors.background
        ),
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

private fun <T> MillenniumControlStateProperty<T>.mapAll(f: (T) -> T) = MillenniumControlStateProperty(
    default = f(default), hovered = f(hovered), focused = f(focused), pressed = f(pressed),
    disabled = f(disabled), selected = f(selected), error = f(error)
)

/**
 * Like [mapAll], but pressed, hovered and focused look exactly like the resting state: no bevel,
 * darkening, gel flash or leftover glass highlight after a tap or long press.
 */
private fun <T> MillenniumControlStateProperty<T>.mapNoPress(f: (T) -> T): MillenniumControlStateProperty<T> {
    val rest = f(default)
    return MillenniumControlStateProperty(
        default = rest, hovered = rest, focused = rest, pressed = rest,
        disabled = f(disabled), selected = f(selected), error = f(error)
    )
}

/**
 * Material 3 Expressive look for every shared component, so all menus, dialogs, sheets and
 * settings match the player: pill buttons, large rounded surfaces, flat fills, no gloss,
 * no hairline borders and no inner glows.
 */
private fun MillenniumThemeComponents.expressive(colors: MillenniumColors): MillenniumThemeComponents {
    val clear = Color.Transparent
    val pill = RoundedCornerShape(50)
    val flatButton = { p: MillenniumButtonThemeProperties ->
        p.copy(shape = pill, borderWidth = 0.dp, innerGlowColor = clear, shadows = emptyList(),
            shadowElevation = 0.dp, bottomRimColor = clear)
    }
    val flatIconButton = { p: MillenniumIconButtonThemeProperties ->
        p.copy(shape = CircleShape, borderWidth = 0.dp, innerGlowColor = clear, shadows = emptyList(),
            shadowElevation = 0.dp, bottomRimColor = clear, orb = false)
    }
    val tonal = SolidColor(colors.raised)
    return copy(
        buttonTheme = buttonTheme.copy(
            primary = buttonTheme.primary.mapNoPress(flatButton),
            secondary = buttonTheme.secondary.mapNoPress { flatButton(it).copy(background = tonal) },
            quiet = buttonTheme.quiet.mapNoPress(flatButton),
            danger = buttonTheme.danger.mapNoPress(flatButton)
        ),
        iconButtonTheme = iconButtonTheme.copy(
            primary = iconButtonTheme.primary.mapNoPress(flatIconButton),
            secondary = iconButtonTheme.secondary.mapNoPress { flatIconButton(it).copy(background = tonal) },
            quiet = iconButtonTheme.quiet.mapNoPress(flatIconButton),
            danger = iconButtonTheme.danger.mapNoPress(flatIconButton)
        ),
        cardTheme = cardTheme.copy(states = cardTheme.states.mapNoPress {
            it.copy(shape = RoundedCornerShape(24.dp), background = SolidColor(colors.surface),
                borderWidth = 0.dp, innerGlowColor = clear, shadowElevation = 0.dp, bottomRimColor = clear)
        }),
        listTileTheme = listTileTheme.copy(states = listTileTheme.states.mapNoPress {
            it.copy(shape = RoundedCornerShape(20.dp), selectedBackground = SolidColor(colors.accent.copy(alpha = 0.16f)),
                selectedGelStrength = 0f, borderWidth = 0.dp, innerGlowColor = clear)
        }),
        dialogTheme = dialogTheme.copy(states = dialogTheme.states.mapAll {
            it.copy(shape = RoundedCornerShape(28.dp), panelBackground = SolidColor(colors.surface),
                panelBorderWidth = 0.dp, panelGlowColor = clear, titleBarBorderColor = clear,
                footerBorderColor = clear)
        }),
        dropdownMenuTheme = dropdownMenuTheme.copy(states = dropdownMenuTheme.states.mapAll {
            it.copy(panelShape = RoundedCornerShape(20.dp), panelBackground = SolidColor(colors.raised),
                panelBorderWidth = 0.dp, panelGlowColor = clear, itemShape = RoundedCornerShape(12.dp),
                itemBorderWidth = 0.dp, itemGlowColor = clear)
        }),
        textFieldTheme = textFieldTheme.copy(states = textFieldTheme.states.mapAll {
            it.copy(shape = RoundedCornerShape(16.dp), background = SolidColor(colors.raised),
                innerGlowColor = clear, insetShadeColor = clear, bottomRimColor = clear)
        }),
        // Material 3 switch: pill track filled with the accent when on, flat thumb, no gloss.
        toggleSwitchTheme = toggleSwitchTheme.copy(states = toggleSwitchTheme.states.mapAll {
            it.copy(trackBackground = SolidColor(colors.inset), selectedTrackBackground = SolidColor(colors.accent),
                trackSize = DpSize(52.dp, 32.dp), trackBorderColor = colors.border, trackBorderWidth = 2.dp,
                trackGlowColor = clear, thumbBackground = SolidColor(colors.text), thumbSize = 22.dp,
                thumbInset = 5.dp, thumbElevation = 0.dp, thumbBorderWidth = 0.dp, thumbGlowColor = clear)
        }),
        modalBottomSheetTheme = modalBottomSheetTheme.copy(states = modalBottomSheetTheme.states.mapAll {
            // Compact padding: the library default (24dp + 16dp spacing) is far too roomy on phones.
            it.copy(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), background = SolidColor(colors.surface),
                borderWidth = 0.dp, innerGlowColor = clear, shadowElevation = 0.dp,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                contentSpacing = 8.dp)
        })
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

/**
 * One flat Material look for every Millennium component, applied last. Millennium falls back to
 * its glossy effects whenever an effect color is null, so every glow, shade, rim and border is
 * set to transparent explicitly, every elevation to zero, and hover / press / focus all look like
 * the resting state. No bevels, gel, shine or outlines anywhere in the app.
 */
private fun MillenniumThemeComponents.uniform(colors: MillenniumColors): MillenniumThemeComponents {
    val clear = Color.Transparent
    val tonal = SolidColor(colors.raised)
    return copy(
        buttonTheme = buttonTheme.copy(
            primary = buttonTheme.primary.mapNoPress { it.copy(background = SolidColor(colors.accent), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp) },
            secondary = buttonTheme.secondary.mapNoPress { it.copy(background = tonal, borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp) },
            quiet = buttonTheme.quiet.mapNoPress { it.copy(background = SolidColor(clear), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp) },
            danger = buttonTheme.danger.mapNoPress { it.copy(background = SolidColor(colors.danger), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp) }
        ),
        iconButtonTheme = iconButtonTheme.copy(
            primary = iconButtonTheme.primary.mapNoPress { it.copy(background = SolidColor(colors.accent), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp, orb = false, orbColor = clear) },
            secondary = iconButtonTheme.secondary.mapNoPress { it.copy(background = tonal, borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp, orb = false, orbColor = clear) },
            quiet = iconButtonTheme.quiet.mapNoPress { it.copy(background = SolidColor(clear), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp, orb = false, orbColor = clear) },
            danger = iconButtonTheme.danger.mapNoPress { it.copy(background = SolidColor(colors.danger), borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear, shadows = emptyList(), shadowElevation = 0.dp, orb = false, orbColor = clear) }
        ),
        cardTheme = cardTheme.copy(states = cardTheme.states.mapNoPress {
            it.copy(shadowElevation = 0.dp, shadowColor = clear, borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear, bottomRimColor = clear)
        }),
        listTileTheme = listTileTheme.copy(states = listTileTheme.states.mapNoPress {
            it.copy(selectedGelStrength = 0f, borderWidth = 0.dp, borderColor = clear, innerGlowColor = clear)
        }),
        textFieldTheme = textFieldTheme.copy(states = textFieldTheme.states.mapNoPress {
            it.copy(background = it.background ?: tonal, borderColor = clear, borderWidth = 0.dp,
                insetShadeColor = clear, innerGlowColor = clear, bottomRimColor = clear)
        }),
        autocompleteTheme = autocompleteTheme.copy(states = autocompleteTheme.states.mapNoPress {
            it.copy(panelBorderColor = clear, panelBorderWidth = 0.dp, panelShadowElevation = 0.dp, panelGlowColor = clear)
        }),
        avatarTheme = avatarTheme.copy(states = avatarTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, shadowColor = clear, innerGlowColor = clear)
        }),
        bottomSheetTheme = bottomSheetTheme.copy(states = bottomSheetTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear)
        }),
        modalBottomSheetTheme = modalBottomSheetTheme.copy(states = modalBottomSheetTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear)
        }),
        checkboxTheme = checkboxTheme.copy(states = checkboxTheme.states.mapNoPress {
            it.copy(faceGlowColor = clear, faceElevation = 0.dp)
        }),
        triStateCheckboxTheme = triStateCheckboxTheme.copy(states = triStateCheckboxTheme.states.mapNoPress {
            it.copy(faceGlowColor = clear, faceElevation = 0.dp)
        }),
        dialogTheme = dialogTheme.copy(states = dialogTheme.states.mapNoPress {
            // No red close cap: dialogs close with their own buttons, back, or a tap outside.
            it.copy(panelBorderColor = clear, panelBorderWidth = 0.dp, panelShadowElevation = 0.dp, panelGlowColor = clear,
                titleBarBorderColor = clear, footerBorderColor = clear, closeCapSize = 0.dp, closeTargetSize = 0.dp)
        }),
        disclosureTheme = disclosureTheme.copy(states = disclosureTheme.states.mapNoPress {
            it.copy(background = it.background ?: tonal, borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear)
        }),
        drawerTheme = drawerTheme.copy(states = drawerTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear)
        }),
        dropdownMenuTheme = dropdownMenuTheme.copy(states = dropdownMenuTheme.states.mapNoPress {
            it.copy(panelBorderColor = clear, panelBorderWidth = 0.dp, panelShadowElevation = 0.dp, panelGlowColor = clear,
                itemBorderColor = clear, itemBorderWidth = 0.dp, itemGlowColor = clear)
        }),
        progressTheme = progressTheme.copy(states = progressTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, innerGlowColor = clear)
        }),
        radioGroupTheme = radioGroupTheme.copy(states = radioGroupTheme.states.mapNoPress {
            it.copy(indicatorGlowColor = clear, indicatorElevation = 0.dp)
        }),
        segmentedButtonTheme = segmentedButtonTheme.copy(states = segmentedButtonTheme.states.mapNoPress {
            it.copy(borderWidth = 0.dp, borderColor = clear, innerGlowColor = clear)
        }),
        separatorTheme = separatorTheme.copy(states = separatorTheme.states.mapNoPress {
            it.copy(highlightColor = clear, highlightWidth = 0.dp)
        }),
        sliderTheme = sliderTheme.copy(states = sliderTheme.states.mapNoPress {
            it.copy(trackBorderColor = clear, trackBorderWidth = 0.dp, thumbBorderColor = clear, thumbBorderWidth = 0.dp,
                thumbElevation = 0.dp, thumbGlowColor = clear, thumbBottomRimColor = clear, trackInsetShadeColor = clear)
        }),
        tabGroupTheme = tabGroupTheme.copy(states = tabGroupTheme.states.mapNoPress {
            it.copy(listBackground = it.listBackground ?: SolidColor(colors.surface), listBorderColor = clear, listBorderWidth = 0.dp, tabBorderWidth = 0.dp, tabElevation = 0.dp, tabGlowColor = clear)
        }),
        toggleSwitchTheme = toggleSwitchTheme.copy(states = toggleSwitchTheme.states.mapNoPress {
            it.copy(trackGlowColor = clear, thumbElevation = 0.dp, thumbBorderColor = clear, thumbBorderWidth = 0.dp, thumbGlowColor = clear)
        }),
        tooltipTheme = tooltipTheme.copy(states = tooltipTheme.states.mapNoPress {
            it.copy(borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear)
        }),
        snackbarTheme = snackbarTheme.copy(properties = snackbarTheme.properties.copy(
            borderColor = clear, borderWidth = 0.dp, shadowElevation = 0.dp, innerGlowColor = clear
        )),
        pullToRefreshTheme = pullToRefreshTheme.copy(properties = pullToRefreshTheme.properties.copy(
            indicatorBorderColor = clear, indicatorBorderWidth = 0.dp, indicatorShadowElevation = 0.dp, indicatorInnerGlowColor = clear
        ))
    )
}

/**
 * Light surfaces: soft off-white ground with clearly darker tonal cards, near-black text and a
 * muted grey that still passes contrast on every surface.
 */
private fun lightSurface(accent: Color, onAccent: Color) = MillenniumColors(
    // Tinted like Pixel apps: page in a soft accent tint, cards lighter, chips almost white.
    background = lerp(Color.White, accent, 0.17f), surface = lerp(Color.White, accent, 0.08f),
    raised = lerp(Color.White, accent, 0.03f), inset = lerp(Color.White, accent, 0.12f),
    text = lerp(Color(0xFF1A1414), accent, 0.08f), muted = lerp(Color(0xFF4A3F3F), accent, 0.15f),
    border = lerp(Color.White, accent, 0.3f), highlight = lerp(Color.White, accent, 0.3f), accent = accent,
    accentTop = accent, accentBottom = accent, onAccent = onAccent,
    success = Color(0xFF15803D), danger = Color(0xFFC62828), scrim = Color(0x99000000)
)

/** Amber is the default (the "Orange" family slot); blue and lime keep their own accents. */
val AppColorSchemes = MillenniumColorSchemes(
    dark = darkSurface(accent = Color(0xFF7C8CFF), onAccent = Color(0xFF0B0B1A)),
    orangeDark = darkSurface(accent = Color(0xFFF5A524), onAccent = Color(0xFF111111)),
    light = lightSurface(accent = Color(0xFF3F51E0), onAccent = Color.White),
    orangeLight = lightSurface(accent = Color(0xFFB45F00), onAccent = Color.White),
    limeLight = lightSurface(accent = Color(0xFF00796B), onAccent = Color.White),
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
    val customAccent = settings.accentColor
    val colorSchemes = remember(systemDark, systemLight, customAccent) {
        if (systemDark != null && systemLight != null) {
            MillenniumColorSchemes(
                light = systemLight, dark = systemDark,
                orangeLight = systemLight, orangeDark = systemDark,
                limeLight = systemLight, limeDark = systemDark
            )
        } else if (customAccent != null) {
            val accent = Color(customAccent.toInt())
            // Dark surfaces need a lighter accent to stay readable on near-black.
            val darkAccent = if (accent.luminance() < 0.3f) lerp(accent, Color.White, 0.35f) else accent
            val light = lightSurface(accent = accent, onAccent = readableOn(accent))
            val dark = darkSurface(accent = darkAccent, onAccent = readableOn(darkAccent))
            MillenniumColorSchemes(
                light = light, dark = dark,
                orangeLight = light, orangeDark = dark,
                limeLight = light, limeDark = dark
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

    SystemBarsAppearance(darkTheme = isDarkTheme)
    SplashThemeSync(settings.theme)
    MillenniumTheme(
        dark = isDarkTheme,
        config = MillenniumThemeConfig(
            colorSchemes = colorSchemes,
            palette = palette,
            dark = darkTheme.expressive(colorSchemes.resolve(palette, dark = true)).uniform(colorSchemes.resolve(palette, dark = true)).copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = darkTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                )
            ),
            light = lightTheme.expressive(colorSchemes.resolve(palette, dark = false)).uniform(colorSchemes.resolve(palette, dark = false)).copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = lightTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                )
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

/** Black or white, whichever reads better on [background]. */
private fun readableOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xFF111111) else Color.White

/** Whether the active Millennium appearance is light. */
@Composable
fun isLightAppearance(): Boolean = MillenniumTheme.colors.background.luminance() > 0.5f
