package com.github.musicyou.ui.screens.player

import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage

import com.github.musicyou.utils.thumbnail

@Composable
fun MotionCoverBackground(
    artworkUri: Uri?,
    modifier: Modifier = Modifier
) {
    if (artworkUri == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // Full-screen album artwork with heavy blur
        AsyncImage(
            model = artworkUri.thumbnail(size = 256),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slightly enlarge so blur never exposes edges
                    scaleX = 1.15f
                    scaleY = 1.15f
                }
                .blur(
                    radius = 90.dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded
                ),
            alpha = 0.55f
        )

        // Dark overlay for readable controls/text
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.50f)
                            )
                        )
                    )
                }
        )
    }
}