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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.NavigationItem
import org.spotube.spotube.bottombarTabs
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.downloads.DownloadBadgeIndicator
import org.spotube.spotube.modules.library.LibraryState
import org.spotube.spotube.modules.library.LibraryTab
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxAdd
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.nav_create

private val IndicatorWidth = 64.dp
private val IndicatorHeight = 32.dp

/**
 * Material 3 Expressive navigation bar. A single Monet-tinted pill sits behind the active icon
 * and slides to the new tab when the destination changes: its two edges move on different
 * springs, so it stretches towards the target and settles with a small bounce.
 */
@Composable
fun AppBottombar(
    navigator: Navigator,
    navigationState: NavigationState,
    modifier: Modifier = Modifier,
    libraryBadge: @Composable () -> Unit = { DownloadBadgeIndicator() }
) {
    val libraryState = koinInject<LibraryState>()
    val tabs = remember { bottombarTabs.filterIsInstance<NavigationItem.Tab<*>>() }
    val selectedIndex = tabs.indexOfFirst { navigationState.topLevelRoute == it.route }
    val createLabel = stringResource(Res.string.nav_create)
    val itemCount = tabs.size + 1
    val background = AppTheme.colors.background

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to background.copy(alpha = 0f),
                    0.12f to background.copy(alpha = 0.97f),
                    1f to background
                )
            )
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        val itemWidth = maxWidth / itemCount
        val target = selectedIndex.coerceAtLeast(0)
        val restLeft = itemWidth * target + (itemWidth - IndicatorWidth) / 2
        // Direction of the last tab change (plain holder: no state write during composition).
        val last = remember { intArrayOf(target, 1) }
        if (target != last[0]) { last[1] = if (target > last[0]) 1 else -1; last[0] = target }
        val goingRight = last[1] > 0
        // Leading edge is quick, trailing edge lags: the pill stretches like a drop of paint.
        val fast = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.72f, stiffness = 520f)
        val slow = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.62f, stiffness = 220f)
        val left by animateDpAsState(restLeft, if (goingRight) slow else fast, label = "navLeft")
        val right by animateDpAsState(restLeft + IndicatorWidth, if (goingRight) fast else slow, label = "navRight")
        val indicatorAlpha by animateFloatAsState(if (selectedIndex >= 0) 1f else 0f, label = "navAlpha")

        Box(
            Modifier
                .offset(x = left)
                .width((right - left).coerceAtLeast(IndicatorHeight))
                .height(IndicatorHeight)
                .graphicsLayer { alpha = indicatorAlpha }
                .clip(RoundedCornerShape(50))
                .background(AppTheme.colors.primary.copy(alpha = 0.26f))
        )

        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                NavBarItem(
                    icon = tab.icon,
                    label = stringResource(tab.title),
                    selected = index == selectedIndex,
                    onClick = { navigator.navigate(tab.route) },
                    badge = if (tab.route == Routes.Library) libraryBadge else null,
                    modifier = Modifier.weight(1f)
                )
            }
            NavBarItem(
                icon = Iconsax.IconsaxAdd,
                label = createLabel,
                selected = false,
                onClick = {
                    navigator.navigate(Routes.Library)
                    libraryState.onTabSelected(LibraryTab.Playlists)
                    libraryState.showCreatePlaylist()
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
) {
    val tint by animateColorAsState(
        if (selected) AppTheme.colors.onSurface else AppTheme.colors.onSurfaceVariant, label = "navTint"
    )
    val iconScale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navScale"
    )
    Column(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.height(IndicatorHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box {
                MillenniumIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                )
                if (badge != null) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp)) { badge() }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = AppTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
