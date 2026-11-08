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

package org.spotube.spotube.modules.shell

import com.composeunstyled.LocalTextStyle
import com.composeunstyled.Text
import org.spotube.millennium.composables.MillenniumBottomNavigationBar
import org.spotube.millennium.composables.MillenniumBottomNavigationItem
import org.spotube.millennium.composables.MillenniumIcon
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.ui.Modifier
import org.spotube.spotube.NavigationItem
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.bottombarTabs
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.modules.downloads.DownloadBadgeIndicator
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import org.koin.compose.koinInject
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.nav_create

@Composable
fun AppBottombar(
    navigator: Navigator,
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    libraryBadge: @Composable () -> Unit = { DownloadBadgeIndicator() }
) {
    val libraryState = koinInject<LibraryState>()
    // Full-width bar fading into the page (no pill, no selection fill): the active tab is drawn
    // in bright text, the others muted. "Create" opens the new-playlist form in the library.
    MillenniumBottomNavigationBar(modifier = modifier) {
        bottombarTabs.filterIsInstance<NavigationItem.Tab<*>>()
            .forEach { tab ->
                val selected = navigationState.topLevelRoute == tab.route
                val title = stringResource(tab.title)
                MillenniumBottomNavigationItem(
                    selected = selected,
                    onClick = { navigator.navigate(tab.route) },
                    contentDescription = title,
                    icon = {
                        NavIndicatorPill(selected) {
                            MillenniumIcon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (selected) AppTheme.colors.primary else LocalTextStyle.current.color
                            )
                        }
                    },
                    label = { Text(title) },
                    badge = if (tab.route == Routes.Library) {
                        libraryBadge
                    } else {
                        null
                    }
                )
            }
        val createLabel = stringResource(Res.string.nav_create)
        MillenniumBottomNavigationItem(
            selected = false,
            onClick = {
                navigator.navigate(Routes.Library)
                libraryState.onTabSelected(LibraryTab.Playlists)
                libraryState.showCreatePlaylist()
            },
            contentDescription = createLabel,
            icon = {
                NavIndicatorPill(false) {
                    MillenniumIcon(
                        imageVector = Iconsax.IconsaxAdd,
                        contentDescription = null,
                        tint = LocalTextStyle.current.color
                    )
                }
            },
            label = { Text(createLabel) }
        )
    }
}

/**
 * Material 3 Expressive active-tab indicator: a tonal pill behind the icon that grows in when the
 * tab becomes active and fades out when it is left.
 */
@Composable
private fun NavIndicatorPill(selected: Boolean, icon: @Composable () -> Unit) {
    val width by animateDpAsState(
        if (selected) 60.dp else 36.dp,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    )
    val background by animateColorAsState(
        if (selected) AppTheme.colors.primary.copy(alpha = 0.22f) else Color.Transparent
    )
    Box(
        Modifier.width(width).height(32.dp).clip(RoundedCornerShape(50)).background(background),
        contentAlignment = Alignment.Center
    ) { icon() }
}
