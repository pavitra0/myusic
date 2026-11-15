package io.github.pavitra0.myusic.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.pavitra0.myusic.enums.BuiltInPlaylist
import io.github.pavitra0.myusic.enums.SettingsSection
import io.github.pavitra0.myusic.ui.screens.album.AlbumScreen
import io.github.pavitra0.myusic.ui.screens.artist.ArtistScreen
import io.github.pavitra0.myusic.ui.screens.builtinplaylist.BuiltInPlaylistScreen
import io.github.pavitra0.myusic.ui.screens.home.HomeAlbums
import io.github.pavitra0.myusic.ui.screens.home.HomeArtistList
import io.github.pavitra0.myusic.ui.screens.home.HomePlaylists
import io.github.pavitra0.myusic.ui.screens.home.HomeSongs
import io.github.pavitra0.myusic.ui.screens.home.QuickPicks
import io.github.pavitra0.myusic.ui.screens.localplaylist.LocalPlaylistScreen
import io.github.pavitra0.myusic.ui.screens.playlist.PlaylistScreen
import io.github.pavitra0.myusic.ui.screens.search.SearchScreen
import io.github.pavitra0.myusic.ui.screens.settings.SettingsPage
import io.github.pavitra0.myusic.ui.screens.settings.SettingsScreen
import io.github.pavitra0.myusic.utils.homeScreenTabIndexKey
import io.github.pavitra0.myusic.utils.rememberPreference
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.rememberSlideDistance
import kotlin.reflect.KClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(
    navController: NavHostController,
    sheetState: SheetState,
    intentAction: String?
) {
    val scope = rememberCoroutineScope()
    val slideDistance = rememberSlideDistance()
    val (screenIndex, _) = rememberPreference(homeScreenTabIndexKey, defaultValue = 0)

    val homeRoute = TopDestinations.routes.getOrElse(
        index = screenIndex,
        defaultValue = { Routes.Home }
    )

    NavHost(
        navController = navController,
        startDestination = if (intentAction == "io.github.pavitra0.myusic.action.search") {
            Routes.Search::class
        } else homeRoute::class,
        enterTransition = {
            NavigationTransitions.enterTransition(
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        exitTransition = {
            NavigationTransitions.exitTransition(
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        popEnterTransition = {
            NavigationTransitions.popEnterTransition(
                initialDestination = initialState.destination,
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        },
        popExitTransition = {
            NavigationTransitions.popExitTransition(
                initialDestination = initialState.destination,
                targetDestination = targetState.destination,
                slideDistance = slideDistance
            )
        }
    ) {
        val navigateToAlbum = { browseId: String ->
            navController.navigate(route = Routes.Album(id = browseId))
        }

        val navigateToArtist = { browseId: String ->
            navController.navigate(route = Routes.Artist(id = browseId))
        }

        val popDestination = {
            if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED)
                navController.popBackStack()
        }

        fun <T : Any> NavGraphBuilder.playerComposable(
            route: KClass<T>,
            content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
        ) {
            composable(route = route) { navBackStackEntry ->
                content(navBackStackEntry)

                BackHandler(enabled = sheetState.currentValue == SheetValue.Expanded) {
                    scope.launch {
                        sheetState.partialExpand()
                    }
                }
            }
        }

        playerComposable(route = Routes.Home::class) {
            QuickPicks(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist,
                onPlaylistClick = { browseId ->
                    navController.navigate(route = Routes.Playlist(id = browseId))
                },
                onOfflinePlaylistClick = {
                    navController.navigate(route = Routes.BuiltInPlaylist(index = 1))
                }
            )
        }

        playerComposable(route = Routes.Songs::class) {
            HomeSongs(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Artists::class) {
            HomeArtistList(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onArtistClick = { artist -> navigateToArtist(artist.id) }
            )
        }

        playerComposable(route = Routes.Albums::class) {
            HomeAlbums(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onAlbumClick = { album -> navigateToAlbum(album.id) }
            )
        }

        playerComposable(route = Routes.Playlists::class) {
            HomePlaylists(
                openSearch = { navController.navigate(route = Routes.Search) },
                openSettings = { navController.navigate(route = Routes.Settings) },
                onBuiltInPlaylist = { playlistIndex ->
                    navController.navigate(route = Routes.BuiltInPlaylist(index = playlistIndex))
                },
                onPlaylistClick = { playlist ->
                    navController.navigate(route = Routes.LocalPlaylist(id = playlist.id))
                }
            )
        }

        playerComposable(route = Routes.Artist::class) { navBackStackEntry ->
            val route: Routes.Artist = navBackStackEntry.toRoute()

            ArtistScreen(
                browseId = route.id,
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist,
                onPlaylistClick = { browseId ->
                    navController.navigate(route = Routes.Playlist(id = browseId))
                }
            )
        }

        playerComposable(route = Routes.Album::class) { navBackStackEntry ->
            val route: Routes.Album = navBackStackEntry.toRoute()

            AlbumScreen(
                browseId = route.id,
                pop = popDestination,
                onAlbumClick = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Playlist::class) { navBackStackEntry ->
            val route: Routes.Playlist = navBackStackEntry.toRoute()

            PlaylistScreen(
                browseId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.Settings::class) {
            SettingsScreen(
                pop = popDestination,
                onGoToSettingsPage = { index ->
                    navController.navigate(Routes.SettingsPage(index = index))
                }
            )
        }

        playerComposable(route = Routes.SettingsPage::class) { navBackStackEntry ->
            val route: Routes.SettingsPage = navBackStackEntry.toRoute()

            SettingsPage(
                section = SettingsSection.entries[route.index],
                pop = popDestination
            )
        }

        playerComposable(route = Routes.Search::class) {
            SearchScreen(
                pop = {
                    if (intentAction == "io.github.pavitra0.myusic.action.search") {
                        navController.navigate(route = homeRoute) {
                            popUpTo(route = Routes.Search) {
                                inclusive = true
                            }
                        }
                    } else popDestination()
                },
                onAlbumClick = navigateToAlbum,
                onArtistClick = navigateToArtist,
                onPlaylistClick = { browseId ->
                    navController.navigate(route = Routes.Playlist(id = browseId))
                }
            )
        }

        playerComposable(route = Routes.BuiltInPlaylist::class) { navBackStackEntry ->
            val route: Routes.BuiltInPlaylist = navBackStackEntry.toRoute()

            BuiltInPlaylistScreen(
                builtInPlaylist = BuiltInPlaylist.entries[route.index],
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }

        playerComposable(route = Routes.LocalPlaylist::class) { navBackStackEntry ->
            val route: Routes.LocalPlaylist = navBackStackEntry.toRoute()

            LocalPlaylistScreen(
                playlistId = route.id,
                pop = popDestination,
                onGoToAlbum = navigateToAlbum,
                onGoToArtist = navigateToArtist
            )
        }
    }
}