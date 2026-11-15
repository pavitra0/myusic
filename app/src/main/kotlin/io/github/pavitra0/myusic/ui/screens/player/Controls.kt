package io.github.pavitra0.myusic.ui.screens.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.Song
import io.github.pavitra0.myusic.ui.components.SeekBar
import io.github.pavitra0.myusic.ui.styling.Dimensions
import io.github.pavitra0.myusic.utils.forceSeekToNext
import io.github.pavitra0.myusic.utils.forceSeekToPrevious
import io.github.pavitra0.myusic.utils.formatAsDuration
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.utils.trackLoopEnabledKey
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun Controls(
    mediaId: String,
    title: String,
    artist: String,
    shouldBePlaying: Boolean,
    position: Long,
    duration: Long,
    onGoToArtist: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val binder = LocalPlayerServiceBinder.current
    binder?.player ?: return

    var trackLoopEnabled by rememberPreference(trackLoopEnabledKey, defaultValue = false)
    var scrubbingPosition by remember(mediaId) { mutableStateOf<Long?>(null) }
    var likedAt by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(mediaId) {
        database.likedAt(mediaId).distinctUntilChanged().collect { likedAt = it }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(
            modifier = Modifier.weight(1f)
        )

        // Song title — large and bold
        Text(
            text = title,
            modifier = Modifier
                .basicMarquee()
                .fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist name
        Text(
            text = artist,
            modifier = Modifier
                .clickable(
                    enabled = onGoToArtist != null,
                    onClick = onGoToArtist ?: {}
                )
                .fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(
            modifier = Modifier.weight(0.5f)
        )

        // Seek bar
        SeekBar(
            value = scrubbingPosition ?: position,
            minimumValue = 0,
            maximumValue = duration,
            onDragStart = {
                scrubbingPosition = it
            },
            onDrag = { delta ->
                scrubbingPosition = if (duration != C.TIME_UNSET) {
                    scrubbingPosition?.plus(delta)?.coerceIn(0, duration)
                } else {
                    null
                }
            },
            onDragEnd = {
                scrubbingPosition?.let(binder.player::seekTo)
                scrubbingPosition = null
            },
            color = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            scrubberRadius = 8.dp,
            barHeight = 4.dp,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        // Duration timestamps
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatAsDuration(scrubbingPosition ?: position),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (duration != C.TIME_UNSET) {
                Text(
                    text = formatAsDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(0.75f)
        )

        // Main playback controls row
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Like button
            IconButton(
                onClick = {
                    val currentMediaItem = binder.player.currentMediaItem
                    database.query {
                        if (database.like(
                                mediaId,
                                if (likedAt == null) System.currentTimeMillis() else null
                            ) == 0
                        ) {
                            currentMediaItem
                                ?.takeIf { it.mediaId == mediaId }
                                ?.let {
                                    database.insert(currentMediaItem, Song::toggleLike)
                                }
                        }
                    }
                },
                modifier = Modifier.weight(1F)
            ) {
                Icon(
                    imageVector = if (likedAt == null) Icons.Outlined.FavoriteBorder else Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = if (likedAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Previous
            IconButton(
                onClick = binder.player::forceSeekToPrevious,
                modifier = Modifier.weight(1F)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            // Play/Pause — large circular filled button (Metrolist style)
            FilledIconButton(
                onClick = {
                    if (shouldBePlaying) {
                        binder.player.pause()
                    } else {
                        if (binder.player.playbackState == Player.STATE_IDLE) {
                            binder.player.prepare()
                        } else if (binder.player.playbackState == Player.STATE_ENDED) {
                            binder.player.seekToDefaultPosition(0)
                        }
                        binder.player.play()
                    }
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector =
                    if (shouldBePlaying) Icons.Filled.Pause
                    else if (binder.player.playbackState == Player.STATE_ENDED) Icons.Filled.Replay
                    else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            // Next
            IconButton(
                onClick = binder.player::forceSeekToNext,
                modifier = Modifier.weight(1F)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat toggle
            IconButton(
                onClick = { trackLoopEnabled = !trackLoopEnabled },
                modifier = Modifier.weight(1F)
            ) {
                Icon(
                    imageVector = if (trackLoopEnabled) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = if (trackLoopEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )
    }
}