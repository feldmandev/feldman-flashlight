package com.feldman.flashlight.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * The brightness slider.
 *
 * Holds its own drag position and pushes whole levels out as they change, so dragging stays smooth
 * at any [maxLevel]; the position only snaps back to [level] when something else — a preset button,
 * the tile, the instant-on preference — moves the torch out from under it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TorchSlider(
    level: Int,
    maxLevel: Int,
    applyLevel: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: androidx.compose.material3.SliderColors = SliderDefaults.colors()
) {
    var sliderValue by rememberSaveable { mutableFloatStateOf(level.toFloat()) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(level) {
        if (abs(sliderValue - level) > 0.5f) sliderValue = level.toFloat()
    }

    Slider(
        value = sliderValue,
        onValueChange = {
            sliderValue = it
            val newLevel = it.toInt().coerceIn(0, maxLevel)
            if (newLevel != level) applyLevel(newLevel)
        },
        valueRange = 0f..maxLevel.toFloat(),
        modifier = modifier.semantics { contentDescription = "Brightness level" },
        interactionSource = interactionSource,
        colors = colors,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                thumbSize = DpSize(
                    width = 4.dp,
                    height = 64.dp,
                ),
                colors = colors,
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(48.dp),
                trackCornerSize = 16.dp,
                trackInsideCornerSize = 2.dp,
                thumbTrackGapSize = 6.dp,
                drawStopIndicator = null,
                colors = colors,
            )
        }
    )
}
