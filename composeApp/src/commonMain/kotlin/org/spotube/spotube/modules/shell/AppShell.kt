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

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledBottomSheet
import com.composeunstyled.rememberBottomSheetState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.NavigationState
import org.spotube.spotube.core.navigation.Navigator
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.ConnectionRequestDialogHost
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.modules.devices.PlayDestinationPickerHost
import org.spotube.spotube.modules.lyrics.LyricsScreen
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackContent
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackContentViewModel
import org.spotube.spotube.modules.shell.alternative_track.AlternativeTrackSheet
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContent
import org.spotube.spotube.modules.shell.player_queue.PlayerQueueContentViewModel
import org.spotube.spotube.modules.shell.player_queue.QueueSheet
import kotlinx.coroutines.launch
import org.spotube.spotube.core.ui.component.PlatformBackHandler
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.spotube.millennium.composables.MillenniumSeparator
import org.spotube.millennium.composables.MillenniumSnackbarHost
import org.spotube.millennium.composables.MillenniumSnackbarHostState
import org.spotube.millennium.theme.MillenniumTheme

val LocalAppShellBottomInset = staticCompositionLocalOf { 0.dp }

@Composable
fun AppShell(
    navigator: Navigator,
    navigationState: NavigationState,
    viewModel: AppShellViewModel = koinViewModel<AppShellViewModel>(),
    queueViewModel: PlayerQueueContentViewModel = koinViewModel<PlayerQueueContentViewModel>(),
    alternativeViewModel: AlternativeTrackContentViewModel = koinViewModel<AlternativeTrackContentViewModel>(),
    content: @Composable () -> Unit
) {
    val navigatorCommands: NavigationCommands = koinInject()
    val remotePlaybackController: RemotePlaybackController = koinInject()
    val snackbarHostState = remember { MillenniumSnackbarHostState() }
    LaunchedEffect(remotePlaybackController) {
        remotePlaybackController.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    val isQueueVisible by queueViewModel.isQueueVisible.collectAsState()
    val isAlternativeVisible by alternativeViewModel.isAlternativeVisible.collectAsState()
    val isLyricsOverlayVisible by viewModel.isLyricsOverlayVisible.collectAsState()

    LaunchedEffect(navigatorCommands, navigator) {
        launch {
            navigatorCommands.navigationCommandFlow.collect { route ->
                navigator.navigate(route)
            }
        }
        launch {
            navigatorCommands.navigationPopCommandFlow.collect { route ->
                navigationState.backStacks[navigationState.topLevelRoute]?.let { backStack ->
                    backStack.lastOrNull()?.let { currentRoute ->
                        if (route == null || route == currentRoute) {
                            navigator.pop()
                        }
                    }
                }
            }
        }
    }

    // Onboarding is a bootstrap, full-screen destination. It draws its own app bar (and thus
    // the desktop window controls), so render it without the sidebar, players or bottom bar.
    if (navigationState.topLevelRoute == Routes.Welcome) {
        content()
        return
    }

    ConnectionRequestDialogHost()
    PlayDestinationPickerHost()

    Box(modifier = Modifier.fillMaxSize()) {
        val useSidebar = viewModel.useSidebar()
        val chromeUiState by viewModel.compactChromeUiState.collectAsState()
        val density = LocalDensity.current
        val navigationBarInset = with(density) {
            WindowInsets.navigationBars.getBottom(this).toDp()
        }
        val bottomOverlayInset =
            viewModel.bottomOverlayInset(useSidebar, navigationBarInset, chromeUiState)

        CompositionLocalProvider(LocalAppShellBottomInset provides bottomOverlayInset) {
            if (useSidebar) {
                val hazeState = rememberHazeState()
                val bgHazeState = rememberHazeState()

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MillenniumTheme.colors.background)
                            .hazeSource(state = bgHazeState) // <-- Put it here!
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AppSidebar(
                                navigator = navigator,
                                navigationState = navigationState
                            )
                            MillenniumSeparator(
                                modifier = Modifier.fillMaxHeight(),
                                vertical = true
                            )
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().hazeSource(hazeState)
                            ) {
                                content()
                            }
                        }
                        AppLargePlayer(
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxWidth(),
                            onQueue = queueViewModel::toggleQueueVisibility,
                            onAlternativeSource = alternativeViewModel::toggleAlternativeVisibility,
                            onLyrics = { navigatorCommands.navigateTo(Routes.Lyrics) })
                    }

                    if (isQueueVisible) {
                        Box(
                            modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { queueViewModel.setQueueVisibility(false) })
                        )
                    }

                    if (isAlternativeVisible) {
                        Box(
                            modifier = Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { alternativeViewModel.setAlternativeVisibility(false) })
                        )
                    }

                    QueueSheet(
                        isVisible = isQueueVisible,
                        onDismiss = { queueViewModel.setQueueVisibility(false) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PlayerQueueContent(
                            viewModel = queueViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AlternativeTrackSheet(
                        isVisible = isAlternativeVisible,
                        onDismiss = { alternativeViewModel.setAlternativeVisibility(false) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AlternativeTrackContent(
                            viewModel = alternativeViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MillenniumTheme.colors.background)
                    )
                    // Lyrics open over the player: what is underneath blurs and recedes while the
                    // lyrics page scales up into place (reversed when it closes).
                    val lyricsProgress by animateFloatAsState(
                        if (isLyricsOverlayVisible) 1f else 0f,
                        spring(dampingRatio = 0.9f, stiffness = 380f),
                        label = "lyricsProgress"
                    )
                    Box(
                        Modifier.fillMaxSize().graphicsLayer {
                            val p = lyricsProgress
                            val scale = 1f - 0.05f * p
                            scaleX = scale
                            scaleY = scale
                            val blur = 24.dp.toPx() * p
                            renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                        }
                    ) {
                    CompactPlayerOverlay(
                        navigator = navigator,
                        navigationState = navigationState,
                        viewModel = viewModel,
                        onQueue = queueViewModel::toggleQueueVisibility,
                        onAlternativeSource = alternativeViewModel::toggleAlternativeVisibility,
                        onExpandLyrics = viewModel::showLyricsOverlay,
                        content = content
                    )
                    }

                    QueueSheet(
                        isVisible = isQueueVisible,
                        onDismiss = { queueViewModel.setQueueVisibility(false) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PlayerQueueContent(
                            viewModel = queueViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AlternativeTrackSheet(
                        isVisible = isAlternativeVisible,
                        onDismiss = { alternativeViewModel.setAlternativeVisibility(false) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AlternativeTrackContent(
                            viewModel = alternativeViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    PlatformBackHandler(enabled = isLyricsOverlayVisible) { viewModel.hideLyricsOverlay() }
                    AnimatedVisibility(
                        visible = isLyricsOverlayVisible,
                        enter = fadeIn(tween(260)) + scaleIn(
                            spring(dampingRatio = 0.85f, stiffness = 420f), initialScale = 0.9f
                        ) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) { it / 10 },
                        exit = fadeOut(tween(200)) + scaleOut(tween(220), targetScale = 0.92f) +
                            slideOutVertically(tween(220)) { it / 12 }
                    ) {
                        LyricsScreen(
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)).statusBarsPadding(),
                            onClose = { viewModel.hideLyricsOverlay() })
                    }
                }
            }
        }

        // Drawn last so it floats above the players/sheets, just above the
        // bottom overlay (large player or compact player + bottombar).
        MillenniumSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomOverlayInset + 12.dp)
        )
    }
}

@OptIn(
    ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class
)
@Composable
internal fun CompactPlayerOverlay(
    navigator: Navigator,
    navigationState: NavigationState,
    viewModel: AppShellViewModel,
    onQueue: () -> Unit,
    onAlternativeSource: () -> Unit,
    onExpandLyrics: () -> Unit,
    floatingPlayer: @Composable (Modifier) -> Unit = { AppFloatingPlayer(modifier = it) },
    expandedPlayer: @Composable (modifier: Modifier, onCollapse: () -> Unit) -> Unit = { modifier, onCollapse ->
        AppExpandedPlayer(
            modifier = modifier,
            onCollapse = onCollapse,
            onQueue = onQueue,
            onAlternativeSource = onAlternativeSource,
            onExpandLyrics = onExpandLyrics
        )
    },
    bottomBar: @Composable (Modifier) -> Unit = {
        AppBottombar(navigator = navigator, navigationState = navigationState, modifier = it)
    },
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val chromeUiState by viewModel.compactChromeUiState.collectAsState()

    val floatingPlayerHeightPx = with(density) { viewModel.floatingPlayerHeight.toPx() }
    val navBarInsetPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val bottomBarHeightPx = with(density) {
        chromeUiState.measuredBottomBarHeight?.toPx()
            ?: (viewModel.bottomBarHeight.toPx() + navBarInsetPx)
    }
    val peekHeightPx = floatingPlayerHeightPx + bottomBarHeightPx
    val peekHeight = with(density) { peekHeightPx.toDp() }

    val partialDetent = remember(peekHeight) {
        SheetDetent("player-peek") { _, _ -> peekHeight }
    }
    // The library remembers its detent definitions at creation. Recreate the state when
    // the measured chrome height changes, rather than keeping a stale peek anchor.
    val sheetState = key(peekHeight) {
        rememberBottomSheetState(
            initialDetent = partialDetent,
            detents = listOf(partialDetent, SheetDetent.FullyExpanded),
            // A spring carries the fling velocity into the settle, so releasing mid-drag
            // glides to the nearest detent instead of stalling and then easing like a tween.
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium)
        )
    }

    // Read only inside draw/derived scopes: the sheet offset changes every drag frame, and
    // reading it in composition would recompose the whole player on each one.
    val progress by remember(sheetState, partialDetent) {
        derivedStateOf {
            val value = if (!sheetState.offset.isFinite() ||
                (sheetState.isIdle && sheetState.currentDetent == partialDetent)
            ) 0f else runCatching {
                sheetState.progress(partialDetent, SheetDetent.FullyExpanded)
            }.getOrDefault(0f).takeIf { it.isFinite() } ?: 0f
            value.coerceIn(0f, 1f)
        }
    }
    // Keyed on the sheet state: it is recreated when the peek height changes, and a derived
    // value left reading the previous sheet's progress would never update again.
    val expandedPlayerVisible by remember(sheetState) {
        derivedStateOf { viewModel.compactSheetUiState(progress).showExpandedPlayer }
    }
    val floatingPlayerVisible by remember(sheetState) {
        derivedStateOf { viewModel.compactSheetUiState(progress).floatingPlayerAlpha > 0.01f }
    }
    // Once shown, keep the expanded player composed: composing it (view models, lyrics, cover
    // art) on the first drag frame of every gesture is the hitch the user feels as lag.
    var expandedPlayerComposed by remember { mutableStateOf(false) }
    if (expandedPlayerVisible) expandedPlayerComposed = true

    // Back closes the full-screen player (back to the page underneath) instead of leaving the app.
    PlatformBackHandler(enabled = sheetState.currentDetent == SheetDetent.FullyExpanded) {
        scope.launch { sheetState.animateTo(partialDetent) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Opening the player pushes the page back: it shrinks a little, blurs and dims, so the
        // player reads as a card rising above it (and the reverse on the way down).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress
                    val scale = 1f - 0.07f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.35f * p
                    val blur = 28.dp.toPx() * p
                    renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
                }
        ) {
            content()
        }

        UnstyledBottomSheet(
            state = sheetState,
            modifier = Modifier.fillMaxSize()
        ) {
            Sheet(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (expandedPlayerComposed) {
                        expandedPlayer(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val p = progress
                                    alpha = viewModel.compactSheetUiState(p).expandedPlayerAlpha
                                    // The player card grows into the screen with rounded top
                                    // corners that flatten as it reaches full height.
                                    val scale = 0.94f + 0.06f * p
                                    scaleX = scale
                                    scaleY = scale
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                    val radius = 32.dp.toPx() * (1f - p)
                                    shape = RoundedCornerShape(topStart = radius, topEnd = radius)
                                    clip = true
                                },
                            { scope.launch { sheetState.animateTo(partialDetent) } }
                        )
                    }

                    // Always composed: building it (view model, cover art, player state) only once
                    // progress drops below the fade-out point stalls the collapse mid-drag.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        floatingPlayer(
                            Modifier
                                .zeroSizeUnless(floatingPlayerVisible)
                                .graphicsLayer {
                                    alpha = viewModel.floatingPlayerAlpha(progress)
                                }
                                // A tap opens the full player too, not only an upward swipe;
                                // the play/next/like buttons inside keep their own clicks.
                                .clickable(role = Role.Button) {
                                    scope.launch { sheetState.animateTo(SheetDetent.FullyExpanded) }
                                })
                    }
                }
            }
        }

        bottomBar(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .zeroSizeUnless(floatingPlayerVisible)
                .onSizeChanged { size ->
                    viewModel.onBottomBarMeasured(with(density) { size.height.toDp() })
                }
                .graphicsLayer {
                    alpha = viewModel.floatingPlayerAlpha(progress)
                })
    }
}

/**
 * Keeps the content composed but takes it out of layout, drawing and hit testing while it is
 * fully faded out, so it neither steals touches from what is above it nor costs a re-composition
 * when it comes back.
 */
private fun Modifier.zeroSizeUnless(visible: Boolean): Modifier =
    if (visible) this else layout { measurable, constraints ->
        measurable.measure(constraints)
        layout(0, 0) {}
    }
