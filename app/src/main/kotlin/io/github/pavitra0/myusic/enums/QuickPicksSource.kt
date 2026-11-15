package io.github.pavitra0.myusic.enums

import androidx.annotation.StringRes
import io.github.pavitra0.myusic.R

enum class QuickPicksSource(
    @StringRes val resourceId: Int
) {
    Trending(
        resourceId = R.string.most_played,
    ),
    LastPlayed(
        resourceId = R.string.last_played,
    ),
    Random(
        resourceId = R.string.random,
    )
}