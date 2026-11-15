package io.github.pavitra0.myusic.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration

val isLandscape
    @Composable
    @ReadOnlyComposable
    get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
