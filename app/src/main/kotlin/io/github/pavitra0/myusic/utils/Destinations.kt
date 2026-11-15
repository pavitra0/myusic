package io.github.pavitra0.myusic.utils

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import io.github.pavitra0.myusic.ui.navigation.TopDestinations

fun NavDestination.isTopDestination(): Boolean {
    return TopDestinations.list.any { this.hasRoute(route = it.route::class) }
}

fun Collection<NavDestination>.areTopDestinations(): Boolean {
    return this.all { it.isTopDestination() }
}