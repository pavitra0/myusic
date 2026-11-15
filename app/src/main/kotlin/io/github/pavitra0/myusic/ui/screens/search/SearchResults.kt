package io.github.pavitra0.myusic.ui.screens.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.searchPage
import io.github.pavitra0.myusic.innertube.utils.from
import io.github.pavitra0.myusic.LocalPlayerPadding
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.enums.SongSortBy
import io.github.pavitra0.myusic.enums.SortOrder
import io.github.pavitra0.myusic.models.ActionInfo
import io.github.pavitra0.myusic.models.LocalMenuState
import io.github.pavitra0.myusic.models.Section
import io.github.pavitra0.myusic.models.Song
import io.github.pavitra0.myusic.ui.components.ChipScaffold
import io.github.pavitra0.myusic.ui.components.InHistoryMediaItemMenu
import io.github.pavitra0.myusic.ui.components.NonQueuedMediaItemMenu
import io.github.pavitra0.myusic.ui.components.SwipeToActionBox
import io.github.pavitra0.myusic.ui.items.AlbumItem
import io.github.pavitra0.myusic.ui.items.ArtistItem
import io.github.pavitra0.myusic.ui.items.ItemPlaceholder
import io.github.pavitra0.myusic.ui.items.ListItemPlaceholder
import io.github.pavitra0.myusic.ui.items.LocalSongItem
import io.github.pavitra0.myusic.ui.items.PlaylistItem
import io.github.pavitra0.myusic.ui.items.SongItem
import io.github.pavitra0.myusic.ui.items.VideoItem
import io.github.pavitra0.myusic.ui.styling.Dimensions
import io.github.pavitra0.myusic.utils.asMediaItem
import io.github.pavitra0.myusic.utils.enqueue
import io.github.pavitra0.myusic.utils.forcePlay
import io.github.pavitra0.myusic.utils.forcePlayAtIndex
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.utils.searchResultScreenTabIndexKey

@Composable
fun SearchResults(
    query: String,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    val emptyItemsText = stringResource(id = R.string.no_results_found)
    val (tabIndex, onTabIndexChanges) = rememberPreference(searchResultScreenTabIndexKey, 0)
    val sections = listOf(
        Section(stringResource(id = R.string.songs), Icons.Outlined.MusicNote),
        Section(stringResource(id = R.string.albums), Icons.Outlined.Album),
        Section(stringResource(id = R.string.artists), Icons.Outlined.Person),
        Section(stringResource(id = R.string.videos), Icons.Outlined.Movie),
        Section(stringResource(id = R.string.playlists), Icons.AutoMirrored.Outlined.QueueMusic),
        Section(stringResource(id = R.string.featured), Icons.AutoMirrored.Outlined.QueueMusic),
        Section(stringResource(id = R.string.library), Icons.Outlined.LibraryMusic)
    )

    ChipScaffold(
        tabIndex = tabIndex,
        onTabChanged = onTabIndexChanges,
        tabColumnContent = sections
    ) { index ->
        when (index) {
            0 -> {
                val binder = LocalPlayerServiceBinder.current
                val menuState = LocalMenuState.current

                ItemsPage(
                    tag = "searchResults/$query/songs",
                    itemsPageProvider = { continuation ->
                        if (continuation == null) {
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Song.value,
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )
                        } else {
                            Innertube.searchPage(
                                continuation = continuation,
                                fromMusicShelfRendererContent = Innertube.SongItem.Companion::from
                            )
                        }
                    },
                    emptyItemsText = emptyItemsText,
                    itemContent = { song ->
                        SwipeToActionBox(
                            modifier = Modifier.animateItem(),
                            primaryAction = ActionInfo(
                                onClick = { binder?.player?.enqueue(song.asMediaItem) },
                                icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                description = R.string.enqueue
                            )
                        ) {
                            SongItem(
                                song = song,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(song.asMediaItem)
                                    binder?.setupRadio(song.info?.endpoint)
                                },
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            onDismiss = menuState::hide,
                                            mediaItem = song.asMediaItem,
                                            onGoToAlbum = onAlbumClick,
                                            onGoToArtist = onArtistClick
                                        )
                                    }
                                }
                            )
                        }
                    },
                    itemPlaceholderContent = {
                        ListItemPlaceholder()
                    }
                )
            }

            1 -> {
                ItemsPage(
                    tag = "searchResults/$query/albums",
                    itemsPageProvider = { continuation ->
                        if (continuation == null) {
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Album.value,
                                fromMusicShelfRendererContent = Innertube.AlbumItem::from
                            )
                        } else {
                            Innertube.searchPage(
                                continuation = continuation,
                                fromMusicShelfRendererContent = Innertube.AlbumItem::from
                            )
                        }
                    },
                    emptyItemsText = emptyItemsText,
                    itemContent = { album ->
                        AlbumItem(
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )

                    },
                    itemPlaceholderContent = {
                        ItemPlaceholder()
                    }
                )
            }

            2 -> {
                ItemsPage(
                    tag = "searchResults/$query/artists",
                    itemsPageProvider = { continuation ->
                        if (continuation == null) {
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Artist.value,
                                fromMusicShelfRendererContent = Innertube.ArtistItem::from
                            )
                        } else {
                            Innertube.searchPage(
                                continuation = continuation,
                                fromMusicShelfRendererContent = Innertube.ArtistItem::from
                            )
                        }
                    },
                    emptyItemsText = emptyItemsText,
                    itemContent = { artist ->
                        ArtistItem(
                            artist = artist,
                            onClick = { onArtistClick(artist.key) }
                        )
                    },
                    itemPlaceholderContent = {
                        ItemPlaceholder(shape = CircleShape)
                    }
                )
            }

            3 -> {
                val binder = LocalPlayerServiceBinder.current
                val menuState = LocalMenuState.current

                ItemsPage(
                    tag = "searchResults/$query/videos",
                    itemsPageProvider = { continuation ->
                        if (continuation == null) {
                            Innertube.searchPage(
                                query = query,
                                params = Innertube.SearchFilter.Video.value,
                                fromMusicShelfRendererContent = Innertube.VideoItem::from
                            )
                        } else {
                            Innertube.searchPage(
                                continuation = continuation,
                                fromMusicShelfRendererContent = Innertube.VideoItem::from
                            )
                        }
                    },
                    emptyItemsText = emptyItemsText,
                    itemContent = { video ->
                        SwipeToActionBox(
                            modifier = Modifier.animateItem(),
                            primaryAction = ActionInfo(
                                onClick = { binder?.player?.enqueue(video.asMediaItem) },
                                icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                description = R.string.enqueue
                            )
                        ) {
                            VideoItem(
                                video = video,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlay(video.asMediaItem)
                                    binder?.setupRadio(video.info?.endpoint)
                                },
                                onLongClick = {
                                    menuState.display {
                                        NonQueuedMediaItemMenu(
                                            mediaItem = video.asMediaItem,
                                            onDismiss = menuState::hide,
                                            onGoToAlbum = onAlbumClick,
                                            onGoToArtist = onArtistClick
                                        )
                                    }
                                }
                            )
                        }
                    },
                    itemPlaceholderContent = {
                        ListItemPlaceholder(
                            thumbnailHeight = 64.dp,
                            thumbnailAspectRatio = 16F / 9F
                        )
                    }
                )
            }

            4, 5 -> {
                ItemsPage(
                    tag = "searchResults/$query/${if (index == 4) "playlists" else "featured"}",
                    itemsPageProvider = { continuation ->
                        if (continuation == null) {
                            val filter =
                                if (index == 4) Innertube.SearchFilter.CommunityPlaylist else Innertube.SearchFilter.FeaturedPlaylist

                            Innertube.searchPage(
                                query = query,
                                params = filter.value,
                                fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                            )
                        } else {
                            Innertube.searchPage(
                                continuation = continuation,
                                fromMusicShelfRendererContent = Innertube.PlaylistItem::from
                            )
                        }
                    },
                    emptyItemsText = emptyItemsText,
                    itemContent = { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.key) }
                        )
                    },
                    itemPlaceholderContent = {
                        ItemPlaceholder()
                    }
                )
            }

            6 -> {
                val binder = LocalPlayerServiceBinder.current
                val menuState = LocalMenuState.current
                var items: List<Song> by remember { mutableStateOf(emptyList()) }

                LaunchedEffect(Unit) {
                    database.songs(SongSortBy.Title, SortOrder.Ascending).collect {
                        items = it.filter { song ->
                            song.title.contains(
                                other = query,
                                ignoreCase = true
                            ) || song.artistsText?.contains(
                                other = query,
                                ignoreCase = true
                            ) == true
                        }
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 400.dp),
                    contentPadding = PaddingValues(
                        start = 0.dp,
                        top = 8.dp,
                        end = 0.dp,
                        bottom = 16.dp + playerPadding
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = items,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SwipeToActionBox(
                            modifier = Modifier.animateItem(),
                            primaryAction = ActionInfo(
                                onClick = { binder?.player?.enqueue(song.asMediaItem) },
                                icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                description = R.string.enqueue
                            )
                        ) {
                            LocalSongItem(
                                song = song,
                                onClick = {
                                    binder?.stopRadio()
                                    binder?.player?.forcePlayAtIndex(
                                        items.map(Song::asMediaItem),
                                        index
                                    )
                                },
                                onLongClick = {
                                    menuState.display {
                                        InHistoryMediaItemMenu(
                                            song = song,
                                            onDismiss = menuState::hide,
                                            onGoToAlbum = onAlbumClick,
                                            onGoToArtist = onArtistClick
                                        )
                                    }
                                }
                            )
                        }
                    }

                    if (items.isEmpty()) {
                        item(
                            key = "empty",
                            span = { GridItemSpan(maxCurrentLineSpan) }
                        ) {
                            Text(
                                text = emptyItemsText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 32.dp)
                                    .fillMaxWidth()
                                    .alpha(Dimensions.mediumOpacity)
                            )
                        }
                    }
                }
            }
        }
    }
}