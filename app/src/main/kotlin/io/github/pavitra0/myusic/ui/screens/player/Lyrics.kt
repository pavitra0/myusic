package io.github.pavitra0.myusic.ui.screens.player

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import io.github.pavitra0.myusic.innertube.Innertube
import io.github.pavitra0.myusic.innertube.requests.lyrics
import io.github.pavitra0.myusic.kugou.KuGou
import io.github.pavitra0.myusic.LocalPlayerServiceBinder
import io.github.pavitra0.myusic.R
import io.github.pavitra0.myusic.database
import io.github.pavitra0.myusic.models.LocalMenuState
import io.github.pavitra0.myusic.models.Lyrics
import io.github.pavitra0.myusic.ui.components.Menu
import io.github.pavitra0.myusic.ui.components.MenuEntry
import io.github.pavitra0.myusic.ui.components.TextFieldDialog
import io.github.pavitra0.myusic.ui.components.TextPlaceholder
import io.github.pavitra0.myusic.ui.styling.Dimensions
import io.github.pavitra0.myusic.ui.styling.onOverlay
import io.github.pavitra0.myusic.utils.SynchronizedLyrics
import io.github.pavitra0.myusic.utils.isLandscape
import io.github.pavitra0.myusic.utils.isShowingSynchronizedLyricsKey
import io.github.pavitra0.myusic.utils.rememberPreference
import io.github.pavitra0.myusic.utils.toast
import io.github.pavitra0.myusic.utils.verticalFadingEdge
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun Lyrics(
    mediaId: String,
    isDisplayed: Boolean,
    onDismiss: () -> Unit,
    size: Dp,
    mediaMetadataProvider: () -> MediaMetadata,
    durationProvider: () -> Long,
    ensureSongInserted: () -> Unit,
    fullScreenLyrics: Boolean,
    toggleFullScreenLyrics: () -> Unit
) {
    AnimatedVisibility(
        visible = isDisplayed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val context = LocalContext.current
        val menuState = LocalMenuState.current
        val currentView = LocalView.current

        var isShowingSynchronizedLyrics by rememberPreference(isShowingSynchronizedLyricsKey, false)

        var isEditing by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        var lyrics by remember {
            mutableStateOf<Lyrics?>(null)
        }

        val text = if (isShowingSynchronizedLyrics) lyrics?.synced else lyrics?.fixed

        var isError by remember(mediaId, isShowingSynchronizedLyrics) {
            mutableStateOf(false)
        }

        LaunchedEffect(mediaId, isShowingSynchronizedLyrics) {
            withContext(Dispatchers.IO) {
                database.lyrics(mediaId).collect {
                    if (isShowingSynchronizedLyrics && it?.synced == null) {
                        val mediaMetadata = mediaMetadataProvider()
                        var duration = withContext(Dispatchers.Main) {
                            durationProvider()
                        }

                        while (duration == C.TIME_UNSET) {
                            delay(duration = 100.milliseconds)
                            duration = withContext(Dispatchers.Main) {
                                durationProvider()
                            }
                        }

                        KuGou.lyrics(
                            artist = mediaMetadata.artist?.toString() ?: "",
                            title = mediaMetadata.title?.toString() ?: "",
                            duration = duration / 1000
                        )?.onSuccess { syncedLyrics ->
                            database.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = it?.fixed,
                                    synced = syncedLyrics?.value ?: ""
                                )
                            )
                        }?.onFailure {
                            isError = true
                        }
                    } else if (!isShowingSynchronizedLyrics && it?.fixed == null) {
                        Innertube.lyrics(videoId = mediaId)?.onSuccess { fixedLyrics ->
                            database.upsert(
                                Lyrics(
                                    songId = mediaId,
                                    fixed = fixedLyrics ?: "",
                                    synced = it?.synced
                                )
                            )
                        }?.onFailure {
                            isError = true
                        }
                    } else {
                        lyrics = it
                    }
                }
            }
        }

        if (isEditing) {
            TextFieldDialog(
                title = stringResource(id = R.string.edit_lyrics),
                hintText = stringResource(id = R.string.enter_lyrics),
                initialTextInput = text ?: "",
                singleLine = false,
                maxLines = 10,
                isTextInputValid = { true },
                onDismiss = { isEditing = false },
                onDone = {
                    database.query {
                        ensureSongInserted()
                        database.upsert(
                            Lyrics(
                                songId = mediaId,
                                fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else it,
                                synced = if (isShowingSynchronizedLyrics) it else lyrics?.synced,
                            )
                        )
                    }
                }
            )
        }

        if (isShowingSynchronizedLyrics) {
            DisposableEffect(Unit) {
                currentView.keepScreenOn = true
                onDispose {
                    currentView.keepScreenOn = false
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .fillMaxSize()
                .background(Color.Black.copy(0.8f))
        ) {
            AnimatedVisibility(
                visible = isError && text == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    text = if (isShowingSynchronizedLyrics) {
                        stringResource(id = R.string.error_fetching_synchronized_lyrics)
                    } else {
                        stringResource(id = R.string.error_fetching_lyrics)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(all = 4.dp)
                        .fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = text?.let(String::isEmpty) == true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                if (isShowingSynchronizedLyrics) {
                    Column(
                        modifier = Modifier.padding(all = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.synchronized_lyrics_not_available),
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { isShowingSynchronizedLyrics = false }
                        ) {
                            Text(text = stringResource(id = R.string.show_unsynchronized_lyrics))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.lyrics_not_available),
                        fontSize = 18.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(all = 8.dp)
                            .fillMaxWidth()
                    )
                }
            }

            if (text?.isNotEmpty() == true) {
                if (isShowingSynchronizedLyrics) {
                    val density = LocalDensity.current
                    val player = LocalPlayerServiceBinder.current?.player
                        ?: return@AnimatedVisibility

                    val synchronizedLyrics = remember(text) {
                        SynchronizedLyrics(KuGou.Lyrics(text).sentences) {
                            player.currentPosition + 50
                        }
                    }

                    val lazyListState = rememberLazyListState(
                        synchronizedLyrics.index,
                        with(density) { size.roundToPx() } / 6)

                    LaunchedEffect(synchronizedLyrics) {
                        val center = with(density) { size.roundToPx() } / 6

                        while (isActive) {
                            delay(duration = 50.milliseconds)
                            if (synchronizedLyrics.update()) {
                                lazyListState.animateScrollToItem(
                                    synchronizedLyrics.index,
                                    center
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        userScrollEnabled = false,
                        contentPadding = PaddingValues(vertical = size / 2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.verticalFadingEdge()
                    ) {
                        itemsIndexed(items = synchronizedLyrics.sentences) { index, sentence ->
                            Text(
                                text = sentence.second,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 20.sp,
                                fontWeight = if (index == synchronizedLyrics.index)
                                    FontWeight.SemiBold
                                else FontWeight.Normal,
                                lineHeight = 28.sp,
                                modifier = Modifier
                                    .padding(vertical = 4.dp, horizontal = 32.dp)
                                    .alpha(if (index == synchronizedLyrics.index) 1F else Dimensions.mediumOpacity)
                            )
                        }
                    }
                } else {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        modifier = Modifier
                            .verticalFadingEdge()
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                            .padding(vertical = size / 4, horizontal = 32.dp)
                    )
                }
            }

            if (text == null && !isError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.shimmer()
                ) {
                    repeat(4) {
                        TextPlaceholder(
                            modifier = Modifier.alpha(1f - it * 0.2f)
                        )
                    }
                }
            }

            if (!isLandscape) {
                IconButton(
                    onClick = toggleFullScreenLyrics,
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Icon(
                        imageVector = if (fullScreenLyrics) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onOverlay
                    )
                }
            }

            IconButton(
                onClick = {
                    menuState.display {
                        Menu {
                            MenuEntry(
                                icon = Icons.Outlined.Schedule,
                                text = if (isShowingSynchronizedLyrics) {
                                    stringResource(id = R.string.show_unsynchronized_lyrics)
                                } else {
                                    stringResource(id = R.string.show_synchronized_lyrics)
                                },
                                secondaryText = if (isShowingSynchronizedLyrics) null else {
                                    stringResource(id = R.string.provided_by_kugou)
                                },
                                onClick = {
                                    menuState.hide()
                                    isShowingSynchronizedLyrics =
                                        !isShowingSynchronizedLyrics
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Edit,
                                text = stringResource(id = R.string.edit_lyrics),
                                onClick = {
                                    menuState.hide()
                                    isEditing = true
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Search,
                                text = stringResource(id = R.string.search_lyrics_online),
                                onClick = {
                                    menuState.hide()
                                    val mediaMetadata = mediaMetadataProvider()

                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_WEB_SEARCH).apply {
                                                putExtra(
                                                    SearchManager.QUERY,
                                                    "${mediaMetadata.title} ${mediaMetadata.artist} lyrics"
                                                )
                                            }
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        context.toast("Couldn't find an application to browse the Internet")
                                    }
                                }
                            )

                            MenuEntry(
                                icon = Icons.Outlined.Download,
                                text = stringResource(id = R.string.fetch_lyrics_again),
                                enabled = lyrics != null,
                                onClick = {
                                    menuState.hide()
                                    database.query {
                                        database.upsert(
                                            Lyrics(
                                                songId = mediaId,
                                                fixed = if (isShowingSynchronizedLyrics) lyrics?.fixed else null,
                                                synced = if (isShowingSynchronizedLyrics) null else lyrics?.synced,
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onOverlay
                )
            }
        }
    }
}