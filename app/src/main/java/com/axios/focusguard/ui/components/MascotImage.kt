package com.axios.focusguard.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.axios.focusguard.R

/**
 * Poses for the AxiosFocusGuard mascot (Green Alien Cat).
 */
enum class MascotPose(@DrawableRes val drawableRes: Int) {
    CLOCK(R.drawable.ic_mascot_clock),
    NEUTRAL(R.drawable.ic_mascot_neutral),
    SHIELD(R.drawable.ic_mascot_shield),
    THINKING(R.drawable.ic_mascot_thinking),
    YAY(R.drawable.ic_mascot_yay),
    ZEN(R.drawable.ic_mascot_zen)
}

/**
 * Unified component to render the app mascot with smooth transitions between poses.
 *
 * @param pose The [MascotPose] to display.
 * @param modifier Modifier for sizing and layout.
 * @param size The size of the mascot image.
 */
@Composable
fun MascotImage(
    pose: MascotPose,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Crossfade(
        targetState = pose,
        animationSpec = tween(durationMillis = 500),
        label = "MascotPoseTransition"
    ) { currentPose ->
        Image(
            painter = painterResource(id = currentPose.drawableRes),
            contentDescription = "Mascot ${currentPose.name}",
            modifier = modifier.size(size)
        )
    }
}
