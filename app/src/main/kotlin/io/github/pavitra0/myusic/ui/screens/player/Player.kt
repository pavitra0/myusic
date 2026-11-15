package io.github.pavitra0.myusic.ui.screens.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.pavitra0.myusic.innertube.models.NavigationEndpoint
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.LocalMenuState
import io.github.pavitra0.myusic.ui.components.BaseMediaItemMenu
import io.github.pavitra0.myusic.ui.components.TooltipIconButton
import io.github.pavitra0.myusic.utils.DisposableListener
import io.github.pavitra0.myusic.utils.isLandscape
import io.github.pavitra0.myusic.utils.motionBackgroundKey
import io.github.pavitra0.myusic.utils.positionAndDurationState
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.utils.seamlessPlay
import io.github.pavitra0.myusic.utils.shouldBePlaying
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val menuState = LocalMenuState.current
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }
    var nullableMediaItem by remember {
        mutableStateOf(
            binder.player.currentMediaItem,
            neverEqualPolicy()
        )
    }

    binder.player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                nullableMediaItem = mediaItem
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val mediaItem = nullableMediaItem ?: return
    val positionAndDuration by binder.player.positionAndDurationState()
    val nextSongTitle =
        if (binder.player.hasNextMediaItem()) binder.player.getMediaItemAt(binder.player.nextMediaItemIndex).mediaMetadata.title.toString()
        else stringResource(id = R.string.open_queue)

    var artistId: String? by remember(mediaItem) {
        mutableStateOf(
            mediaItem.mediaMetadata.extras?.getStringArrayList("artistIds")?.let { artists ->
                if (artists.size == 1) artists.first()
                else null
            }
        )
    }

    var isShowingLyrics by rememberSaveable { mutableStateOf(false) }
    var fullScreenLyrics by remember { mutableStateOf(false) }
    var isShowingStatsForNerds by rememberSaveable { mutableStateOf(false) }
    var isQueueOpen by rememberSaveable { mutableStateOf(false) }
    var isShowingSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    val sleepTimerMillisLeft by (binder.sleepTimerMillisLeft
        ?: flowOf(null))
        .collectAsState(initial = null)

    val queueState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            if (artistId == null) {
                val artistsInfo = database.songArtistInfo(mediaItem.mediaId)
                if (artistsInfo.size == 1) artistId = artistsInfo.first().id
            }
        }
    }

    val thumbnailContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
        Thumbnail(
            isShowingLyrics = isShowingLyrics,
            onShowLyrics = { isShowingLyrics = it },
            fullScreenLyrics = fullScreenLyrics,
            toggleFullScreenLyrics = { fullScreenLyrics = !fullScreenLyrics },
            isShowingStatsForNerds = isShowingStatsForNerds,
            onShowStatsForNerds = { isShowingStatsForNerds = it },
            modifier = modifier
        )
    }

    val controlsContent: @Composable (modifier: Modifier) -> Unit = { modifier ->
        Controls(
            mediaId = mediaItem.mediaId,
            title = mediaItem.mediaMetadata.title?.toString().orEmpty(),
            artist = mediaItem.mediaMetadata.artist?.toString().orEmpty(),
            shouldBePlaying = shouldBePlaying,
            position = positionAndDuration.first,
            duration = positionAndDuration.second,
            onGoToArtist = artistId?.let {
                { onGoToArtist(it) }
            },
            modifier = modifier
        )
    }

    val isMotionBackgroundEnabled by rememberPreference(motionBackgroundKey, true)

    Box(modifier = Modifier.fillMaxSize()) {
        if (isMotionBackgroundEnabled) {
            // Motion cover: blurred fluid animated album art background
            MotionCoverBackground(
                artworkUri = mediaItem.mediaMetadata.artworkUri
            )
        }

    Column {
        Box(
            modifier = Modifier.weight(1F)
        ) {
            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(0.66f)
                            .padding(bottom = 16.dp)
                    ) {
                        thumbnailContent(
                            Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    controlsContent(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxHeight()
                            .weight(1f)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 54.dp)
                ) {
                    val weight = animateFloatAsState(
                        targetValue = if (fullScreenLyrics) 0.01f else 1f
                    )

                    val alpha = animateFloatAsState(
                        targetValue = if (fullScreenLyrics) 0f else 1f
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1.25f)
                    ) {
                        thumbnailContent(
                            Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                        )
                    }

                    controlsContent(
                        Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .alpha(alpha = alpha.value)
                            .weight(weight.value)
                    )
                }
            }
        }

        // Queue bar — Metrolist style: rounded top corners, elevated surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .clickable { isQueueOpen = true }
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount < 0) isQueueOpen = true
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { isQueueOpen = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = nextSongTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1F),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )

            TooltipIconButton(
                description = R.string.sleep_timer,
                onClick = { isShowingSleepTimerDialog = true },
                icon = if (sleepTimerMillisLeft == null) Icons.Outlined.Timer else Icons.Filled.Timer
            )

            IconButton(
                onClick = {
                    menuState.display {
                        BaseMediaItemMenu(
                            onDismiss = menuState::hide,
                            mediaItem = mediaItem,
                            onStartRadio = {
                                binder.stopRadio()
                                binder.player.seamlessPlay(mediaItem)
                                binder.setupRadio(NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId))
                            },
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isShowingSleepTimerDialog) {
            SleepTimer(
                sleepTimerMillisLeft = sleepTimerMillisLeft,
                onDismiss = { isShowingSleepTimerDialog = false }
            )
        }

        if (isQueueOpen) {
            ModalBottomSheet(
                onDismissRequest = { isQueueOpen = false },
                modifier = Modifier.fillMaxWidth(),
                sheetState = queueState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Box(modifier = Modifier.size(width = 40.dp, height = 4.dp))
                    }
                }
            ) {
                Queue(
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist
                )
            }
        }
    }
    } // end Box wrapping MotionCoverBackground + Column
}