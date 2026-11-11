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

package org.spotube.spotube.modules.welcome

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composeunstyled.Text
import org.jetbrains.compose.resources.stringResource
import org.spotube.millennium.composables.MillenniumIcon
import org.spotube.spotube.core.ui.theming.AppTheme
import org.spotube.spotube.modules.welcome.pages.BrandingPage
import org.spotube.spotube.modules.welcome.pages.DonationPage
import org.spotube.spotube.modules.welcome.pages.HowItWorksPage
import org.spotube.spotube.modules.welcome.pages.PluginsPage
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxCheckCircle
import org.spotube.spotube.resources.iconsax.IconsaxNext
import org.spotube.spotube.resources.iconsax.IconsaxPrevious
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.welcome_back
import spotube.composeapp.generated.resources.welcome_skip

/**
 * First-run setup, in the app's Material 3 Expressive look: a flat tinted page, a row of step
 * pills (the current one stretches and shows its name), the step's content sliding in, and one
 * large action at the bottom. It is a route
 * ([org.spotube.spotube.core.navigation.Routes.Welcome]) rendered inside the app shell.
 */
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = 1040.dp)) {
                // Header: name on the left, "skip" as a quiet pill on the right.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WelcomeWordmark(fontSize = AppTheme.typography.headlineSmall.fontSize)
                    Spacer(Modifier.weight(1f))
                    if (!state.isLastPage) {
                        Text(
                            text = stringResource(Res.string.welcome_skip),
                            style = AppTheme.typography.labelLarge,
                            color = AppTheme.colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(role = Role.Button, onClick = viewModel::completeOnboarding)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                WelcomeSteps(
                    state = state,
                    onSelect = viewModel::goToPage,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                )

                // The step itself, on a large rounded tonal surface.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(AppTheme.colors.surfaceContainer)
                ) {
                    AnimatedContent(
                        targetState = state.currentPageIndex,
                        transitionSpec = {
                            val forward = targetState > initialState
                            (slideInHorizontally { if (forward) it / 4 else -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { if (forward) -it / 4 else it / 4 } + fadeOut())
                        },
                        label = "welcomeStep"
                    ) { index ->
                        when (index) {
                            0 -> BrandingPage()
                            1 -> DonationPage(donations = state.donations, onDonate = viewModel::openDonation)
                            2 -> PluginsPage()
                            else -> HowItWorksPage()
                        }
                    }
                }

                WelcomeActionBar(state = state, viewModel = viewModel)
            }
        }
    }
}

/** One pill per step: the current one is wide, filled with the accent and shows its label. */
@Composable
private fun WelcomeSteps(
    state: WelcomeUiState,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        welcomeStepLabels.forEachIndexed { index, label ->
            val current = index == state.currentPageIndex
            val done = index < state.currentPageIndex
            val background by animateColorAsState(
                when {
                    current -> AppTheme.colors.primary
                    done -> AppTheme.colors.primary.copy(alpha = 0.32f)
                    else -> AppTheme.colors.surfaceContainerHighest
                },
                label = "stepColor"
            )
            val height by animateDpAsState(
                if (current) 40.dp else 12.dp,
                spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "stepHeight"
            )
            Box(
                modifier = Modifier
                    .weight(if (current) 2.4f else 1f)
                    .height(40.dp)
                    .clickable(role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .clip(RoundedCornerShape(50))
                        .background(background),
                    contentAlignment = Alignment.Center
                ) {
                    if (current) {
                        Text(
                            text = stringResource(label),
                            style = AppTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeActionBar(
    state: WelcomeUiState,
    viewModel: WelcomeViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.currentPageIndex > 0) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.surfaceContainerHighest)
                    .clickable(role = Role.Button) { viewModel.goToPage(state.currentPageIndex - 1) },
                contentAlignment = Alignment.Center
            ) {
                MillenniumIcon(
                    imageVector = Iconsax.IconsaxPrevious,
                    contentDescription = stringResource(Res.string.welcome_back),
                    tint = AppTheme.colors.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .clip(if (state.isLastPage) RoundedCornerShape(22.dp) else RoundedCornerShape(50))
                .background(AppTheme.colors.primary)
                .clickable(role = Role.Button, onClick = viewModel::onPrimary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(state.primaryLabel),
                style = AppTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AppTheme.colors.onPrimary
            )
            Spacer(Modifier.width(10.dp))
            MillenniumIcon(
                imageVector = if (state.isLastPage) Iconsax.IconsaxCheckCircle else Iconsax.IconsaxNext,
                contentDescription = null,
                tint = AppTheme.colors.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
