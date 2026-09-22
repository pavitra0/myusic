package com.github.musicyou.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong

@Composable
fun SeekBar(
    value: Long,
    minimumValue: Long,
    maximumValue: Long,
    onDragStart: (Long) -> Unit,
    onDrag: (Long) -> Unit,
    onDragEnd: () -> Unit,
    color: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 4.dp,
    scrubberColor: Color = color,
    scrubberRadius: Dp = 7.dp,
    shape: Shape = RectangleShape,
    drawSteps: Boolean = false,
) {
    var isDragging by remember { mutableStateOf(false) }

    val animatedScrubberRadius by animateDpAsState(
        targetValue = if (isDragging) scrubberRadius * 1.3f else scrubberRadius,
        label = "scrubberRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue <= minimumValue) return@pointerInput

                var acc = 0f

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val initialPos = (offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        onDragStart(initialPos)
                    },
                    onHorizontalDrag = { _, delta ->
                        acc += delta / size.width * (maximumValue - minimumValue)
                        if (acc !in -1f..1f) {
                            onDrag(acc.toLong())
                            acc -= acc.toLong()
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        acc = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        isDragging = false
                        acc = 0f
                        onDragEnd()
                    }
                )
            }
            .pointerInput(minimumValue, maximumValue) {
                if (maximumValue <= minimumValue) return@pointerInput

                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val pos = (offset.x / size.width * (maximumValue - minimumValue) + minimumValue).roundToLong()
                        onDragStart(pos)
                        tryAwaitRelease()
                        isDragging = false
                        onDragEnd()
                    }
                )
            }
            .padding(horizontal = scrubberRadius)
            .drawWithContent {
                val progress = if (maximumValue <= minimumValue) {
                    0f
                } else {
                    ((value - minimumValue).toFloat() / (maximumValue - minimumValue).toFloat()).coerceIn(0f, 1f)
                }

                val scrubberPosition = progress * size.width
                val barHeightPx = barHeight.toPx()
                val centerY = size.height / 2f

                // Draw background track
                drawLine(
                    color = backgroundColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = barHeightPx,
                    cap = StrokeCap.Round
                )

                // Draw active progress line
                if (scrubberPosition > 0f) {
                    drawLine(
                        color = color,
                        start = Offset(0f, centerY),
                        end = Offset(scrubberPosition, centerY),
                        strokeWidth = barHeightPx,
                        cap = StrokeCap.Round
                    )
                }

                // Draw scrubber thumb circle
                drawCircle(
                    color = scrubberColor,
                    radius = animatedScrubberRadius.toPx(),
                    center = Offset(scrubberPosition, centerY)
                )
            }
            .height(24.dp)
    )
}
