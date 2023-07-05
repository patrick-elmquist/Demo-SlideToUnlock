@file:OptIn(ExperimentalMaterialApi::class)

package com.patrick.elmquist.demo.slidetounlock

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.OutlinedButton
import androidx.compose.material.SwipeProgress
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.patrick.elmquist.demo.slidetounlock.ui.theme.DemoSlideToUnlockTheme
import kotlin.math.roundToInt

@Composable
fun SlideToUnlock(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onUnlockRequested: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeableState(
        initialValue = if (isLoading) Track.Anchor.End else Track.Anchor.Start,
        confirmStateChange = { anchor ->
            if (anchor == Track.Anchor.End) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onUnlockRequested()
            }
            true
        }
    )

    val swipeFraction by remember {
        derivedStateOf { calculateSwipeFraction(swipeState.progress) }
    }

    val backgroundColor by remember {
        derivedStateOf { calculateTrackColor(swipeFraction) }
    }

    val hintTextColor by remember {
        derivedStateOf { calculateHintTextColor(swipeFraction) }
    }

    LaunchedEffect(isLoading) {
        swipeState.animateTo(if (isLoading) Track.Anchor.End else Track.Anchor.Start)
    }

    Track(
        swipeState = swipeState,
        color = backgroundColor,
        enabled = !isLoading,
        modifier = modifier,
    ) {
        Text(
            text = "Swipe to unlock reward",
            color = hintTextColor,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(Hint.PaddingValues),
        )

        Thumb(
            isLoading = isLoading,
            modifier = Modifier
                .offset { IntOffset(swipeState.offset.value.roundToInt(), 0) },
        )
    }
}

@Composable
private fun Track(
    swipeState: SwipeableState<Track.Anchor>,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit),
) {
    val density = LocalDensity.current
    var fullWidth by remember { mutableStateOf(0) }
    val startOfTrack = 0f
    val endOfTrack = remember(fullWidth) {
        with(density) { fullWidth - (2 * Track.HorizontalPadding + Thumb.Size).toPx() }
    }

    Box(
        modifier = modifier
            .onSizeChanged { fullWidth = it.width }
            .height(Track.Height)
            .fillMaxWidth()
            .swipeable(
                enabled = enabled,
                state = swipeState,
                orientation = Orientation.Horizontal,
                anchors = mapOf(
                    startOfTrack to Track.Anchor.Start,
                    endOfTrack to Track.Anchor.End,
                ),
                thresholds = Track.SnapThreshold,
                velocityThreshold = Track.VelocityThreshold,
            )
            .background(
                color = color,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(Track.PaddingValues),
        content = content,
    )
}

@Composable
private fun Thumb(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Thumb.Size)
            .background(color = Color.White, shape = CircleShape)
            .padding(Thumb.Padding),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(2.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
            )
        }
    }
}

private fun calculateSwipeFraction(progress: SwipeProgress<Track.Anchor>): Float {
    val atAnchor = progress.from == progress.to
    val fromStart = progress.from == Track.Anchor.Start
    return if (atAnchor) {
        if (fromStart) 0f else 1f
    } else {
        if (fromStart) progress.fraction else 1f - progress.fraction
    }
}

private fun calculateHintTextColor(swipeFraction: Float): Color {
    val fraction = (swipeFraction / Hint.FadeOutThreshold).coerceIn(0f..1f)
    return lerp(Color.White, Color.White.copy(alpha = 0f), fraction)
}

private fun calculateTrackColor(swipeFraction: Float): Color {
    val fraction = (swipeFraction / Track.BackgroundChangeThreshold).coerceIn(0f..1f)
    return lerp(Color(0xFF111111), Color(0xFFFFDB00), fraction)
}

private object Hint {
    const val FadeOutThreshold = 0.35f
    val PaddingValues = PaddingValues(horizontal = Thumb.Size + 8.dp)
}

private object Thumb {
    val Size = 40.dp
    val Padding = 8.dp
}

@VisibleForTesting
internal object Track {
    val Height = 56.dp

    val HorizontalPadding = 10.dp
    val PaddingValues = PaddingValues(
        horizontal = HorizontalPadding,
        vertical = 8.dp,
    )

    enum class Anchor { Start, End }

    private const val VelocityMultiplier = 10
    val VelocityThreshold = SwipeableDefaults.VelocityThreshold * VelocityMultiplier

    const val BackgroundChangeThreshold = 0.4f

    private const val SnapThresholdFraction = 0.8f
    private val StartToEndSnapThreshold = FractionalThreshold(SnapThresholdFraction)
    private val EndToStartSnapThreshold = FractionalThreshold(1f - SnapThresholdFraction)
    val SnapThreshold = { from: Anchor, _: Anchor ->
        if (from == Anchor.Start) StartToEndSnapThreshold else EndToStartSnapThreshold
    }
}

private val previewBackgroundColor = Color(0xFFDEDEDE)

@Preview
@Composable
private fun Preview() {
    var isLoading by remember { mutableStateOf(false) }
    DemoSlideToUnlockTheme {
        val spacing = 88.dp
        Column(
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .background(previewBackgroundColor)
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(spacing))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Normal")
                Spacer(modifier = Modifier.widthIn(min = 16.dp))
                Thumb(isLoading = false)
            }

            Spacer(modifier = Modifier.height(0.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Loading")
                Spacer(modifier = Modifier.widthIn(min = 16.dp))
                Thumb(isLoading = true)
            }


            Spacer(modifier = Modifier.height(spacing))

            Text(text = "Inactive")
            Track(
                swipeState = SwipeableState(Track.Anchor.Start),
                enabled = true,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Active")
            Track(
                swipeState = SwipeableState(Track.Anchor.Start),
                enabled = true,
                color = Color(0xFFFFDB00),
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )


            Spacer(modifier = Modifier.height(spacing))


            SlideToUnlock(
                isLoading = isLoading,
                onUnlockRequested = { isLoading = true },
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                colors = ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(percent = 50),
                onClick = { isLoading = false }) {
                Text(text = "Cancel loading", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
