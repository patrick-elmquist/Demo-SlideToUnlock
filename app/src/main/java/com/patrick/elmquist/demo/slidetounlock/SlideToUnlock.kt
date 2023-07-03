@file:OptIn(ExperimentalMaterialApi::class)

package com.patrick.elmquist.demo.slidetounlock

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.material3.Divider
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
    var swipeFraction by remember { mutableStateOf(0f) }
    val swipeState = rememberSwipeableState(
        initialValue = if (isLoading) Track.Anchor.End else Track.Anchor.Start,
    ) { anchor ->
        if (anchor == Track.Anchor.End) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onUnlockRequested()
        }
        true
    }

    LaunchedEffect(isLoading) {
        swipeState.animateTo(if (isLoading) Track.Anchor.End else Track.Anchor.Start)
    }

    LaunchedEffect(swipeState.progress) {
        swipeFraction = calculateSwipeFraction(swipeState.progress)
    }

    val (inactiveBackgroundColor, activeBackgroundColor) = Track.BackgroundColors
    val backgroundColor by remember {
        derivedStateOf {
            calculateBackgroundColor(swipeFraction, inactiveBackgroundColor, activeBackgroundColor)
        }
    }

    val (inactiveHintColor, activeHintColor) = Hint.TextColors
    val hintTextColor by remember {
        derivedStateOf {
            calculateHintTextColor(swipeFraction, inactiveHintColor, activeHintColor)
        }
    }

    Track(
        swipeState = swipeState,
        color = backgroundColor,
        enabled = !isLoading,
        modifier = modifier,
    ) {
        Hint(
            text = "Swipe to unlock reward",
            color = hintTextColor,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(Hint.PaddingValues),
        )

        Thumb(
            isLoading = isLoading,
            modifier = Modifier.offset { IntOffset(swipeState.offset.value.roundToInt(), 0) },
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
    var fullWidth by remember { mutableStateOf(0) }
    val startOfTrack = 0f
    val endOfTrack = with(LocalDensity.current) {
        fullWidth - (2 * Track.HorizontalPadding + Thumb.Size).toPx()
    }
    val anchors = mapOf(
        startOfTrack to Track.Anchor.Start,
        endOfTrack to Track.Anchor.End,
    )
    Box(
        modifier = modifier
            .onSizeChanged { fullWidth = it.width }
            .height(Track.Height)
            .fillMaxWidth()
            .swipeable(
                enabled = enabled,
                state = swipeState,
                orientation = Orientation.Horizontal,
                anchors = anchors,
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
private fun Hint(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun Thumb(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Thumb.Size)
            .background(
                color = Color.White,
                shape = CircleShape,
            )
            .padding(Thumb.Padding),
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp)
        } else {
            Image(
                painter = painterResource(R.drawable.arrow_right),
                contentDescription = null,
            )
        }
    }
}

private fun calculateSwipeFraction(progress: SwipeProgress<Track.Anchor>): Float {
    val from = progress.from
    val atAnchor = from == progress.to
    val fromStart = from == Track.Anchor.Start
    val fraction = progress.fraction
    return if (atAnchor) {
        if (fromStart) 0f else 1f
    } else {
        if (fromStart) fraction else 1f - fraction
    }
}

private fun calculateHintTextColor(
    swipeFraction: Float,
    inactiveHintColor: Color,
    activeHintColor: Color,
): Color {
    val fraction = (swipeFraction / Hint.FadeOutThreshold).coerceIn(0f..1f)
    return lerp(inactiveHintColor, activeHintColor, fraction)
}

private fun calculateBackgroundColor(
    swipeFraction: Float,
    inactiveBackgroundColor: Color,
    activeBackgroundColor: Color,
): Color {
    var fraction = (swipeFraction / Track.BackgroundChangeThreshold).coerceIn(0f..1f)
    fraction = LinearOutSlowInEasing.transform(fraction)
    return lerp(inactiveBackgroundColor, activeBackgroundColor, fraction)
}

private object Hint {
    val TextColors
        // Note: don't use Color.Transparent when mixing colors, it's not the same thing
        @Composable
        get() = Color.White to Color.White.copy(alpha = 0f)

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

    val BackgroundColors
        @Composable
        get() = Color(0xFF111111) to Color(0xFFFFDB00)

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

private val previewBackgroundColor = Color(0xFFEEEEEE)

@Preview
@Composable
private fun PreviewTrack() {
    var isLoading by remember { mutableStateOf(false) }
    DemoSlideToUnlockTheme {
        Column(
            verticalArrangement = spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(previewBackgroundColor)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = "THUMB",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

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


            Divider(modifier = Modifier.padding(vertical = 24.dp))


            Text(
                text = "TRACK",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 0.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Inactive")
            Track(
                swipeState = SwipeableState(Track.Anchor.Start),
                enabled = true,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Active")
            Track(
                swipeState = SwipeableState(Track.Anchor.Start),
                enabled = true,
                color = Color(0xFFFFDB00),
                modifier = Modifier.fillMaxWidth(),
                content = {},
            )


            Divider(modifier = Modifier.padding(vertical = 24.dp))


            Text(
                text = "FINAL PRODUCT",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            SlideToUnlock(
                isLoading = isLoading,
                onUnlockRequested = { isLoading = true },
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                shape = RoundedCornerShape(percent = 50),
                onClick = { isLoading = false }) {
                Text(text = "Cancel loading", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
