package io.github.pavitra0.myusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.pavitra0.myusic.ui.styling.shimmer
import kotlin.random.Random

@Composable
fun TextPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.shimmer.copy(alpha = 0.4f)
) {
    Spacer(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
            .fillMaxWidth(remember { 0.25f + Random.nextFloat() * 0.5f })
            .height(14.dp)
    )
}