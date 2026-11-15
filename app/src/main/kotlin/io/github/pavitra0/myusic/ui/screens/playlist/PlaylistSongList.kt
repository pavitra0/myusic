package io.github.pavitra0.myusic.ui.screens.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.LocalPlayerPadding
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.models.ActionInfo
import io.github.pavitra0.myusic.models.LocalMenuState
import io.github.pavitra0.myusic.ui.components.CoverScaffold
import io.github.pavitra0.myusic.ui.components.NonQueuedMediaItemMenu
import io.github.pavitra0.myusic.ui.components.ShimmerHost
import io.github.pavitra0.myusic.ui.components.SwipeToActionBox
import io.github.pavitra0.myusic.ui.components.adaptiveThumbnailContent
import io.github.pavitra0.myusic.ui.items.ListItemPlaceholder
import io.github.pavitra0.myusic.ui.items.SongItem
import io.github.pavitra0.myusic.utils.asMediaItem
import io.github.pavitra0.myusic.utils.enqueue
import io.github.pavitra0.myusic.utils.forcePlayAtIndex
import io.github.pavitra0.myusic.utils.forcePlayFromBeginning

@Composable
fun PlaylistSongList(
    playlistPage: Innertube.PlaylistOrAlbumPage?,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    val thumbnailContent =
        adaptiveThumbnailContent(playlistPage == null, playlistPage?.thumbnail?.url)

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "thumbnail") {
            CoverScaffold(
                primaryButton = ActionInfo(
                    onClick = {
                        playlistPage?.songsPage?.items?.let { songs ->
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    songs.shuffled().map(Innertube.SongItem::asMediaItem)
                                )
                            }
                        }
                    },
                    icon = Icons.Outlined.Shuffle,
                    description = R.string.shuffle
                ),
                secondaryButton = ActionInfo(
                    onClick = {
                        playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                            ?.let { mediaItems ->
                                binder?.player?.enqueue(mediaItems)
                            }
                    },
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    description = R.string.enqueue
                ),
                content = thumbnailContent
            )
        }

        item(key = "spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        itemsIndexed(items = playlistPage?.songsPage?.items ?: emptyList()) { index, song ->
            SwipeToActionBox(
                primaryAction = ActionInfo(
                    onClick = { binder?.player?.enqueue(song.asMediaItem) },
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    description = R.string.enqueue
                )
            ) {
                SongItem(
                    song = song,
                    onClick = {
                        playlistPage?.songsPage?.items?.map(Innertube.SongItem::asMediaItem)
                            ?.let { mediaItems ->
                                binder?.stopRadio()
                                binder?.player?.forcePlayAtIndex(mediaItems, index)
                            }
                    },
                    onLongClick = {
                        menuState.display {
                            NonQueuedMediaItemMenu(
                                onDismiss = menuState::hide,
                                mediaItem = song.asMediaItem,
                                onGoToAlbum = onGoToAlbum,
                                onGoToArtist = onGoToArtist
                            )
                        }
                    }
                )
            }
        }

        if (playlistPage == null) {
            item(key = "loading") {
                ShimmerHost {
                    repeat(4) {
                        ListItemPlaceholder()
                    }
                }
            }
        }
    }
}
