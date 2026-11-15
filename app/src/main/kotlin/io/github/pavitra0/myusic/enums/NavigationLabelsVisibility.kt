package io.github.pavitra0.myusic.enums

import androidx.annotation.StringRes
import io.github.pavitra0.myusic.R

enum class NavigationLabelsVisibility(
    @StringRes val resourceId: Int
) {
    Visible(resourceId = R.string.visible),
    VisibleWhenActive(resourceId = R.string.visible_when_active),
    Hidden(resourceId = R.string.hidden)
}