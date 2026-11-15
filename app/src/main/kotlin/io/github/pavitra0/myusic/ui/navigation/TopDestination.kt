package io.github.pavitra0.myusic.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector

data class TopDestination<T : Any>(
    val route: T,
    @StringRes val resourceId: Int,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)