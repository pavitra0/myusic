package com.github.musicyou

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.compose.rememberNavController
import com.github.innertube.Innertube
import com.github.innertube.requests.playlistPage
import com.github.innertube.requests.song
import com.github.musicyou.database.MusicDatabase
import com.github.musicyou.models.LocalMenuState
import com.github.musicyou.service.PlayerService
import com.github.musicyou.ui.components.BottomNavigation
import com.github.musicyou.ui.navigation.Navigation
import com.github.musicyou.ui.navigation.Routes
import com.github.musicyou.ui.screens.player.PlayerScaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.github.musicyou.ui.styling.AppTheme
import com.github.musicyou.ui.styling.extractSeedColor
import com.github.musicyou.utils.pureBlackKey
import com.github.musicyou.utils.dynamicThemeFromCoverKey
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.forcePlay
import com.github.musicyou.utils.intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is PlayerService.Binder) this@MainActivity.binder = service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    private var binder by mutableStateOf<PlayerService.Binder?>(null)
    private var data by mutableStateOf<Uri?>(null)

    override fun onStart() {
        super.onStart()
        bindService(intent<PlayerService>(), serviceConnection, BIND_AUTO_CREATE)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val launchedFromNotification = intent?.extras?.getBoolean("expandPlayerBottomSheet") == true
        data = intent?.data ?: intent?.getStringExtra(Intent.EXTRA_TEXT)?.toUri()

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()
            val scope = rememberCoroutineScope()
            val pureBlack by rememberPreference(pureBlackKey, false)
            val dynamicThemeFromCover by rememberPreference(dynamicThemeFromCoverKey, true)
            var seedColor by remember { mutableStateOf<Color?>(null) }

            val playerState = rememberStandardBottomSheetState(
                initialValue = SheetValue.Hidden,
                confirmValueChange = { value ->
                    if (value == SheetValue.Hidden) {
                        binder?.stopRadio()
                        binder?.player?.clearMediaItems()
                    }

                    return@rememberStandardBottomSheetState true
                },
                skipHiddenState = false
            )

            // Track the current artwork URI reactively via Player.Listener
            var currentArtworkUri by remember { mutableStateOf<Uri?>(null) }

            DisposableEffect(binder?.player) {
                val player = binder?.player ?: return@DisposableEffect onDispose { }

                // Initialize with current value
                currentArtworkUri = player.currentMediaItem?.mediaMetadata?.artworkUri

                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        currentArtworkUri = mediaItem?.mediaMetadata?.artworkUri
                    }
                }

                player.addListener(listener)
                onDispose { player.removeListener(listener) }
            }

            // Extract seed color whenever artwork changes
            LaunchedEffect(currentArtworkUri, dynamicThemeFromCover) {
                if (dynamicThemeFromCover && currentArtworkUri != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val result = context.imageLoader.execute(
                                ImageRequest.Builder(context)
                                    .data(currentArtworkUri)
                                    .allowHardware(false)
                                    .size(64, 64)
                                    .build()
                            )
                            val bitmap = result.image?.toBitmap()
                            if (bitmap != null) {
                                val extracted = bitmap.extractSeedColor()
                                withContext(Dispatchers.Main) {
                                    seedColor = extracted
                                }
                            }
                        } catch (_: Exception) {
                            // Failed to load artwork, keep previous seed color
                        }
                    }
                } else {
                    seedColor = null
                }
            }

            AppTheme(
                seedColor = seedColor,
                pureBlack = pureBlack
            ) {

                Box(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(value = LocalPlayerServiceBinder provides binder) {
                        val menuState = LocalMenuState.current

                        Scaffold(
                            bottomBar = {
                                AnimatedVisibility(
                                    visible = playerState.targetValue != SheetValue.Expanded,
                                    enter = slideInVertically(initialOffsetY = { it / 2 }),
                                    exit = slideOutVertically(targetOffsetY = { it })
                                ) {
                                    BottomNavigation(navController = navController)
                                }
                            }
                        ) { paddingValues ->
                            PlayerScaffold(
                                navController = navController,
                                sheetState = playerState,
                                scaffoldPadding = paddingValues
                            ) {
                                Navigation(
                                    navController = navController,
                                    sheetState = playerState,
                                    intentAction = intent?.action
                                )
                            }
                        }

                        if (menuState.isDisplayed) {
                            ModalBottomSheet(
                                onDismissRequest = menuState::hide,
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                dragHandle = {
                                    Surface(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        shape = MaterialTheme.shapes.extraLarge
                                    ) {
                                        Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
                                    }
                                }
                            ) {
                                menuState.content()
                            }
                        }
                    }
                }
            }

            DisposableEffect(binder?.player) {
                val player = binder?.player ?: return@DisposableEffect onDispose { }

                if (player.currentMediaItem == null) scope.launch { playerState.hide() }
                else {
                    if (launchedFromNotification) {
                        intent.replaceExtras(Bundle())
                        scope.launch { playerState.expand() }
                    } else scope.launch { playerState.partialExpand() }
                }

                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED && mediaItem != null)
                            if (mediaItem.mediaMetadata.extras?.getBoolean("isFromPersistentQueue") != true) scope.launch { playerState.expand() }
                            else scope.launch { playerState.partialExpand() }
                    }
                }

                player.addListener(listener)
                onDispose { player.removeListener(listener) }
            }

            LaunchedEffect(data) {
                val uri = data ?: return@LaunchedEffect

                lifecycleScope.launch(Dispatchers.Main) {
                    when (val path = uri.pathSegments.firstOrNull()) {
                        "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                            val browseId = "VL$playlistId"

                            if (playlistId.startsWith("OLAK5uy_")) {
                                Innertube.playlistPage(browseId = browseId)?.getOrNull()
                                    ?.let {
                                        it.songsPage?.items?.firstOrNull()?.album?.endpoint?.browseId?.let { browseId ->
                                            navController.navigate(
                                                route = Routes.Album(id = browseId)
                                            )
                                        }
                                    }
                            } else navController.navigate(
                                route = Routes.Playlist(id = browseId)
                            )
                        }

                        "channel", "c" -> uri.lastPathSegment?.let { channelId ->
                            navController.navigate(
                                route = Routes.Artist(id = channelId)
                            )
                        }

                        else -> when {
                            path == "watch" -> uri.getQueryParameter("v")
                            uri.host == "youtu.be" -> path
                            else -> null
                        }?.let { videoId ->
                            Innertube.song(videoId)?.getOrNull()?.let { song ->
                                val binder = snapshotFlow { binder }.filterNotNull().first()
                                withContext(Dispatchers.Main) {
                                    binder.player.forcePlay(song.asMediaItem)
                                }
                            }
                        }
                    }
                }

                data = null
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        data = intent.data ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri()
    }

    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }
}

lateinit var database: MusicDatabase

val LocalPlayerServiceBinder = staticCompositionLocalOf<PlayerService.Binder?> { null }
val LocalPlayerPadding = compositionLocalOf { 0.dp }