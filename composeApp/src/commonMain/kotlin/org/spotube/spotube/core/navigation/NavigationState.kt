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

package org.spotube.spotube.core.navigation

import org.spotube.spotube.core.ui.component.rememberDisplayCornerRadius
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.EnterExitState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import org.spotube.spotube.core.ui.theming.AppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import org.spotube.spotube.core.di.injectLogger
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.component.KoinComponent

val TOP_LEVEL_ROUTES: Set<NavKey> = setOf(
    Routes.Home,
    Routes.Search,
    Routes.Library,
    Routes.Settings,
    // Bootstrap destination: needs its own back stack when the app starts on it.
    Routes.Welcome)

val serializersConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Routes.Home::class, Routes.Home.serializer())
            subclass(Routes.Search::class, Routes.Search.serializer())
            subclass(Routes.Library::class, Routes.Library.serializer())
            subclass(Routes.Settings::class, Routes.Settings.serializer())
            subclass(Routes.Plugins::class, Routes.Plugins.serializer())
            subclass(Routes.WebView::class, Routes.WebView.serializer())
            subclass(Routes.Playlist::class, Routes.Playlist.serializer())
            subclass(Routes.Artist::class, Routes.Artist.serializer())
            subclass(Routes.Album::class, Routes.Album.serializer())
            subclass(Routes.Blacklist::class, Routes.Blacklist.serializer())
            subclass(Routes.Devices::class, Routes.Devices.serializer())
            subclass(Routes.Jam::class, Routes.Jam.serializer())
            subclass(Routes.Welcome::class, Routes.Welcome.serializer())
            subclass(Routes.LocalFolder::class, Routes.LocalFolder.serializer())
        }
    }
}

class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) : KoinComponent {
    var topLevelRoute by topLevelRoute

    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }

    internal val logger by injectLogger<NavigationState>()

}

@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute,
        topLevelRoutes,
        configuration = serializersConfig,
        serializer = MutableStateSerializer(PolymorphicSerializer(NavKey::class))
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key ->
        rememberNavBackStack(
            configuration = serializersConfig,
            key
        )
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    // Every page gets the app background: during transitions and the predictive back gesture
    // two pages are on screen at once, and transparent pages would show through each other.
    // While a page leaves (predictive back included) its corners round to the screen's own.
    val opaquePageDecorator = remember {
        NavEntryDecorator<NavKey> { entry ->
            val corner = rememberDisplayCornerRadius()
            val transition = LocalNavAnimatedContentScope.current.transition
            val radius by transition.animateDp(label = "pageCorner") { state ->
                if (state == EnterExitState.PostExit) corner else 0.dp
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(radius))
                    .background(AppTheme.colors.background)
            ) { entry.Content() }
        }
    }
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        val decorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
            opaquePageDecorator)
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = decorators,
            entryProvider = entryProvider
        )
    }

    val state = stacksInUse
        .flatMap { decoratedEntries[it] ?: emptyList() }
        .toMutableStateList()
    logger.d { "Current entries: ${state.joinToString("->") { it.contentKey.toString() }}" }
    return state
}

class Navigator(val navigationState: NavigationState) : KoinComponent {
    val logger by injectLogger<Navigator>()
    fun navigate(route: NavKey) {
        if (route in TOP_LEVEL_ROUTES) {
            val targetStack = navigationState.backStacks[navigationState.topLevelRoute]
            if (targetStack != null) {
                // Clear everything except the very first entry (the root)
                while (targetStack.size > 1) {
                    targetStack.removeLastOrNull()
                }
            }
            navigationState.topLevelRoute = route
            logger.d { "Navigating to top-level route: $route, cleared back stack: ${targetStack != null}" }
        } else {
            logger.d { "Navigating to route: $route" }
            logger.d {
                "Back stack before navigation: ${
                    navigationState.backStacks[navigationState.topLevelRoute]?.joinToString(
                        "->"
                    )
                }"
            }
            val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            if (currentStack?.lastOrNull() != route) {
                currentStack?.add(route)
            }
        }
    }

    fun pop() {
        val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
            ?: error("Current back stack not found for route: ${navigationState.topLevelRoute}")
        val currentRoute = currentStack.last()
        if (currentRoute == navigationState.topLevelRoute) {
            navigationState.topLevelRoute = navigationState.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}