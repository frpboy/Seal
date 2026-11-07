package com.junkfood.seal.ui.common

import android.os.Build
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.junkfood.seal.ui.common.motion.EmphasizeEasing
import com.junkfood.seal.ui.common.motion.EmphasizedAccelerate
import com.junkfood.seal.ui.common.motion.EmphasizedDecelerate
import com.junkfood.seal.ui.common.motion.materialSharedAxisXIn
import com.junkfood.seal.ui.common.motion.materialSharedAxisXOut

const val DURATION_ENTER = 220
const val DURATION_EXIT = 160
const val initialOffset = 0.06f

private fun <T> enterTween() = tween<T>(durationMillis = DURATION_ENTER, easing = EmphasizeEasing)

private fun <T> exitTween() = tween<T>(durationMillis = DURATION_EXIT, easing = EmphasizeEasing)

private val fadeSpring =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
private val fadeTween = tween<Float>(durationMillis = DURATION_EXIT)

private val fadeSpec = fadeTween

fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    usePredictiveBack: Boolean = Build.VERSION.SDK_INT >= 34,
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) {
    if (usePredictiveBack) {
        animatedComposablePredictiveBack(route, arguments, deepLinks, content)
    } else {
        animatedComposableLegacy(route, arguments, deepLinks, content)
    }
}

fun NavGraphBuilder.animatedComposablePredictiveBack(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    // Keep the same restrained forward/back motion across Android versions.
    // No whole-page zoom on back; system animation scale is left to Compose.
    animatedComposableLegacy(route, arguments, deepLinks, content)

fun NavGraphBuilder.animatedComposableLegacy(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = {
            materialSharedAxisXIn(initialOffsetX = { (it * initialOffset).toInt() }, durationMillis = DURATION_ENTER)
        },
        exitTransition = {
            materialSharedAxisXOut(targetOffsetX = { -(it * initialOffset).toInt() }, durationMillis = DURATION_EXIT)
        },
        popEnterTransition = {
            materialSharedAxisXIn(initialOffsetX = { -(it * initialOffset).toInt() }, durationMillis = DURATION_ENTER)
        },
        popExitTransition = {
            materialSharedAxisXOut(targetOffsetX = { (it * initialOffset).toInt() }, durationMillis = DURATION_EXIT)
        },
        content = content,
    )

fun NavGraphBuilder.animatedComposableVariant(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = {
            slideInHorizontally(enterTween(), initialOffsetX = { (it * initialOffset).toInt() }) +
                fadeIn(fadeSpec)
        },
        exitTransition = { fadeOut(fadeSpec) },
        popEnterTransition = { fadeIn(fadeSpec) },
        popExitTransition = {
            slideOutHorizontally(exitTween(), targetOffsetX = { (it * initialOffset).toInt() }) +
                fadeOut(fadeSpec)
        },
        content = content,
    )

val springSpec =
    spring(stiffness = Spring.StiffnessMedium, visibilityThreshold = IntOffset.VisibilityThreshold)

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.slideInVerticallyComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit,
) =
    composable(
        route = route,
        arguments = arguments,
        deepLinks = deepLinks,
        enterTransition = {
            slideInVertically(initialOffsetY = { (it * initialOffset).toInt() }, animationSpec = enterTween()) + fadeIn(fadeSpec)
        },
        exitTransition = { fadeOut(fadeSpec) },
        popEnterTransition = { fadeIn(fadeSpec) },
        popExitTransition = {
            slideOutVertically(targetOffsetY = { (it * initialOffset).toInt() }, animationSpec = exitTween()) + fadeOut(fadeSpec)
        },
        content = content,
    )
