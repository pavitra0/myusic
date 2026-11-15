package io.github.pavitra0.myusic.ui.screens.artist

import android.content.Intent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.itemsPage
import io.github.pavitra0.myusic.innertube.requests.itemsPageContinuation
import io.github.pavitra0.myusic.innertube.utils.from
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.LocalMenuState
import io.github.pavitra0.myusic.models.Section
import io.github.pavitra0.myusic.ui.components.NonQueuedMediaItemMenu
import io.github.pavitra0.myusic.ui.components.TabScaffold
import io.github.pavitra0.myusic.ui.components.TooltipIconButton
import io.github.pavitra0.myusic.ui.components.adaptiveThumbnailContent
import io.github.pavitra0.myusic.ui.items.AlbumItem
import io.github.pavitra0.myusic.ui.items.ItemPlaceholder
import io.github.pavitra0.myusic.ui.items.ListItemPlaceholder
import io.github.pavitra0.myusic.ui.items.SongItem
import io.github.pavitra0.myusic.ui.screens.search.ItemsPage
import io.github.pavitra0.myusic.utils.artistScreenTabIndexKey
import io.github.pavitra0.myusic.utils.asMediaItem
import io.github.pavitra0.myusic.utils.forcePlay
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.viewmodels.ArtistViewModel
import kotlinx.coroutines.launch

@Composable
fun ArtistScreen(
    browseId: String,
    pop: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val viewModel: ArtistViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        Section(stringResource(id = R.string.overview), Icons.Outlined.Person),
        Section(stringResource(id = R.string.songs), Icons.Outlined.MusicNote),
        Section(stringResource(id = R.string.albums), Icons.Outlined.Album),
        Section(stringResource(id = R.string.singles), Icons.Outlined.Album),
        Section(stringResource(id = R.string.library), Icons.Outlined.LibraryMusic)
    )
    var tabIndex by rememberPreference(artistScreenTabIndexKey, defaultValue = 0)
    val pagerState = rememberPagerState(
        initialPage = tabIndex,
        pageCount = { tabs.size }
    )

    LaunchedEffect(Unit) {
        viewModel.loadArtist(
            browseId = browseId,
            tabIndex = pagerState.currentPage
        )
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { index ->
            tabIndex = index
        }
    }

    val thumbnailContent = adaptiveThumbnailContent(
        isLoading = viewModel.artist?.timestamp == null,
        url = viewModel.artist?.thumbnailUrl
    )

    TabScaffold(
        pagerState = pagerState,
        topIconButtonId = Icons.AutoMirrored.Outlined.ArrowBack,
        onTopIconButtonClick = pop,
        sectionTitle = viewModel.artist?.name ?: "",
        appBarActions = {
            val context = LocalContext.current

            TooltipIconButton(
                description = if (viewModel.artist?.bookmarkedAt == null) R.string.add_bookmark else R.string.remove_bookmark,
                onClick = {
                    val bookmarkedAt =
                        if (viewModel.artist?.bookmarkedAt == null) System.currentTimeMillis() else null

                    database.query {
                        viewModel.artist
                            ?.copy(bookmarkedAt = bookmarkedAt)
                            ?.let(database::update)
                    }
                },
                icon = if (viewModel.artist?.bookmarkedAt == null) Icons.Outlined.BookmarkAdd else Icons.Filled.Bookmark,
                inTopBar = true
            )

            TooltipIconButton(
                description = R.string.share,
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://music.youtube.com/channel/$browseId"
                        )
                    }

                    context.startActivity(Intent.createChooser(sendIntent, null))
                },
                icon = Icons.Outlined.Share,
                inTopBar = true
            )
        },
        tabColumnContent = tabs
    ) { index ->
        when (index) {
            0 -> ArtistOverview(
                youtubeArtistPage = viewModel.artistPage,
                thumbnailContent = thumbnailContent,
                onAlbumClick = onAlbumClick,
                onArtistClick = onArtistClick,
                onPlaylistClick = onPlaylistClick,
                onViewAllSongsClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onViewAllAlbumsClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                onViewAllSinglesClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                }
            )

            1 -> {
                val binder = LocalPlayerServiceBinder.current
                val menuState = LocalMenuState.current

                ItemsPage(
                    tag = "artist/$browseId/songs",
                    itemsPageProvider = viewModel.artistPage?.let { artistPage ->
                        { continuation ->
                            continuation?.let {
                                Innertube.itemsPageContinuation(
                                    continuation = continuation,
                                    fromMusicResponsiveListItemRenderer = Innertube.SongItem::from,
                                )
                            } ?: artistPage
                                .songsEndpoint
                                ?.takeIf { it.browseId != null }
                                ?.let { endpoint ->
                                    Innertube.itemsPage(
                                        browseId = endpoint.browseId!!,
                                        params = endpoint.params,
                                        fromMusicResponsiveListItemRenderer = Innertube.SongItem::from,
                                    )
                                }
                            ?: Result.success(
                                Innertube.ItemsPage(
                                    items = artistPage.songs,
                                    continuation = null
                                )
                            )
                        }
                    },
                    itemContent = { song ->
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
                                        onGoToAlbum = onAlbumClick
                                    )
                                }
                            }
                        )
                    },
                    itemPlaceholderContent = {
                        ListItemPlaceholder()
                    }
                )
            }

            2 -> ItemsPage(
                tag = "artist/$browseId/albums",
                emptyItemsText = stringResource(id = R.string.no_albums_artist),
                itemsPageProvider = viewModel.artistPage?.let { artistPage ->
                    { continuation ->
                        continuation?.let {
                            Innertube.itemsPageContinuation(
                                continuation = continuation,
                                fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                            )
                        } ?: artistPage
                            .albumsEndpoint
                            ?.takeIf { it.browseId != null }
                            ?.let { endpoint ->
                                Innertube.itemsPage(
                                    browseId = endpoint.browseId!!,
                                    params = endpoint.params,
                                    fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                )
                            }
                        ?: Result.success(
                            Innertube.ItemsPage(
                                items = artistPage.albums,
                                continuation = null
                            )
                        )
                    }
                },
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

            3 -> ItemsPage(
                tag = "artist/$browseId/singles",
                emptyItemsText = stringResource(id = R.string.no_singles_artist),
                itemsPageProvider = viewModel.artistPage?.let { artistPage ->
                    { continuation ->
                        continuation?.let {
                            Innertube.itemsPageContinuation(
                                continuation = continuation,
                                fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                            )
                        } ?: artistPage
                            .singlesEndpoint
                            ?.takeIf { it.browseId != null }
                            ?.let { endpoint ->
                                Innertube.itemsPage(
                                    browseId = endpoint.browseId!!,
                                    params = endpoint.params,
                                    fromMusicTwoRowItemRenderer = Innertube.AlbumItem::from,
                                )
                            }
                        ?: Result.success(
                            Innertube.ItemsPage(
                                items = artistPage.singles,
                                continuation = null
                            )
                        )
                    }
                },
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

            4 -> ArtistLocalSongs(
                browseId = browseId,
                thumbnailContent = thumbnailContent,
                onGoToAlbum = onAlbumClick
            )
        }
    }
}