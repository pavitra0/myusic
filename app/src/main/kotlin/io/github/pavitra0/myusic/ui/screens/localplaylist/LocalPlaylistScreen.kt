package io.github.pavitra0.myusic.ui.screens.localplaylist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.playlistPage
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.Playlist
import io.github.pavitra0.myusic.models.SongPlaylistMap
import io.github.pavitra0.myusic.ui.components.ConfirmationDialog
import io.github.pavitra0.myusic.ui.components.TextFieldDialog
import io.github.pavitra0.myusic.ui.components.TooltipIconButton
import io.github.pavitra0.myusic.utils.asMediaItem
import io.github.pavitra0.myusic.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPlaylistScreen(
    playlistId: Long,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    var playlist: Playlist? by remember { mutableStateOf(null) }

    var isRenaming by rememberSaveable { mutableStateOf(false) }
    var isDeleting by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        database.playlist(playlistId).filterNotNull().collect { playlist = it }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = playlist?.name ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = pop) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (!playlist?.browseId.isNullOrEmpty()) {
                        TooltipIconButton(
                            description = R.string.sync_playlist,
                            onClick = {
                                playlist?.browseId?.let { browseId ->
                                    database.transaction {
                                        runBlocking(Dispatchers.IO) {
                                            withContext(Dispatchers.IO) {
                                                Innertube.playlistPage(browseId = browseId)
                                                    ?.completed()
                                            }
                                        }?.getOrNull()?.let { remotePlaylist ->
                                            database.clearPlaylist(playlistId)

                                            remotePlaylist.songsPage
                                                ?.items
                                                ?.map(Innertube.SongItem::asMediaItem)
                                                ?.onEach(database::insert)
                                                ?.mapIndexed { position, mediaItem ->
                                                    SongPlaylistMap(
                                                        songId = mediaItem.mediaId,
                                                        playlistId = playlistId,
                                                        position = position
                                                    )
                                                }?.let(database::insertSongPlaylistMaps)
                                        }
                                    }
                                }
                            },
                            icon = Icons.Outlined.Sync,
                            inTopBar = true
                        )
                    }

                    TooltipIconButton(
                        description = R.string.rename_playlist,
                        onClick = { isRenaming = true },
                        icon = Icons.Outlined.Edit,
                        inTopBar = true
                    )

                    TooltipIconButton(
                        description = R.string.delete_playlist,
                        onClick = { isDeleting = true },
                        icon = Icons.Outlined.Delete,
                        inTopBar = true
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            LocalPlaylistSongs(
                playlistId = playlistId,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )

            if (isRenaming) {
                TextFieldDialog(
                    title = stringResource(id = R.string.rename_playlist),
                    hintText = stringResource(id = R.string.playlist_name_hint),
                    initialTextInput = playlist?.name ?: "",
                    onDismiss = { isRenaming = false },
                    onDone = { text ->
                        database.query {
                            playlist?.copy(name = text)
                                ?.let(database::update)
                        }
                    }
                )
            }

            if (isDeleting) {
                ConfirmationDialog(
                    title = stringResource(id = R.string.delete_playlist_dialog),
                    onDismiss = { isDeleting = false },
                    onConfirm = {
                        database.query {
                            playlist?.let(database::delete)
                        }
                        pop()
                    }
                )
            }
        }
    }
}