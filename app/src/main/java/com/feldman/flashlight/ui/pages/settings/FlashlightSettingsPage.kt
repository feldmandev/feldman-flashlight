package com.feldman.flashlight.ui.pages.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import com.feldman.flashlight.storage.LightSourceMode
import com.feldman.flashlight.storage.autoOffTimerMinutesFlow
import com.feldman.flashlight.storage.autoFlashlightOffFlow
import com.feldman.flashlight.storage.defaultFlashlightLevelFlow
import com.feldman.flashlight.storage.defaultLightSourceModeFlow
import com.feldman.flashlight.storage.instantFlashlightFlow
import com.feldman.flashlight.storage.screenLightColorArgbFlow
import com.feldman.flashlight.storage.setAutoOffTimerMinutes
import com.feldman.flashlight.storage.setAutoFlashlightOff
import com.feldman.flashlight.storage.setDefaultFlashlightLevel
import com.feldman.flashlight.storage.setDefaultLightSourceMode
import com.feldman.flashlight.storage.setInstantFlashlight
import com.feldman.flashlight.storage.setScreenLightColorArgb
import com.feldman.flashlight.storage.setVolumeButtonsFlashlight
import com.feldman.flashlight.storage.volumeButtonsFlashlightFlow
import com.feldman.flashlight.ui.components.SettingsCategoryColor
import com.feldman.flashlight.ui.components.SettingsTopBar
import com.feldman.motion.MotionScaffold
import com.feldman.motion.MotionButton
import com.feldman.motion.MotionButtonState
import com.feldman.motion.ITEM_SPACER
import com.feldman.motion.isDarkTheme
import com.feldman.motion.motionBottomSheetAnchor
import com.feldman.motion.symbolPainter
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * How the torch behaves. Every choice writes straight to prefs and the flashlight page picks it up
 * on its next composition, so this page has no save step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightSettingsPage(
    onBack: () -> Unit,
    onOpenScreenColor: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultLightMode by context.defaultLightSourceModeFlow().collectAsState(initial = LightSourceMode.FLASH)
    val instantFlashlight by context.instantFlashlightFlow().collectAsState(initial = false)
    val autoFlashlightOff by context.autoFlashlightOffFlow().collectAsState(initial = true)
    val volumeButtonsFlashlight by context.volumeButtonsFlashlightFlow().collectAsState(initial = true)
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = 100)
    val autoOffTimerMinutes by context.autoOffTimerMinutesFlow().collectAsState(initial = 0)
    val screenLightColorArgb by context.screenLightColorArgbFlow().collectAsState(initial = 0xFFFFFFFF.toInt())

    val screenColors = listOf(
        "White" to 0xFFFFFFFF.toInt(),
        "Warm" to 0xFFFFE7C2.toInt(),
        "Red" to 0xFFFF4D4D.toInt()
    )

    MotionScaffold(
        scaffoldModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        topBar = {
            SettingsTopBar(
                title = "Flashlight",
                onBack = onBack,
                chromeColor = SettingsCategoryColor.FLASHLIGHT.container(isDarkTheme())
            )
        }
    ) {
        title("Default light source")
        section {
            listOf(
                Triple(LightSourceMode.FLASH, "Flashlight (Rear LED)", "flashlight_on"),
                Triple(LightSourceMode.SCREEN, "Screen light", "smartphone"),
                Triple(LightSourceMode.BOTH, "Both (Flash + Screen)", "flash_on")
            ).forEach { (mode, label, icon) ->
                choiceItem(
                    key = mode.key,
                    title = label,
                    icon = symbolPainter(icon),
                    selected = defaultLightMode == mode,
                    containerColor = if (defaultLightMode == mode) colorScheme.primary else colorScheme.surfaceContainerHigh,
                    onClick = { scope.launch { context.setDefaultLightSourceMode(mode) } }
                )
            }
        }
        title("Automation")
        section {
            switchItem(
                title = "Turn on at launch",
                description = "Automatically activate the flashlight when opening the app",
                checked = instantFlashlight,
                onCheckedChange = { scope.launch { context.setInstantFlashlight(it) } }
            )
            switchItem(
                title = "Turn off on exit",
                description = "Automatically switch the torch off when leaving or backgrounding the app",
                checked = autoFlashlightOff,
                onCheckedChange = { scope.launch { context.setAutoFlashlightOff(it) } }
            )
        }

        title("Automatic shutoff")
        section {
            listOf(0, 1, 5, 15, 30).forEach { minutes ->
                val label = if (minutes == 0) "Off" else "$minutes minute${if (minutes == 1) "" else "s"}"
                choiceItem(
                    key = minutes,
                    title = label,
                    icon = symbolPainter(if (minutes == 0) "timer_off" else "timer"),
                    selected = autoOffTimerMinutes == minutes,
                    containerColor = if (autoOffTimerMinutes == minutes) colorScheme.primary else colorScheme.surfaceContainerHigh,
                    onClick = { scope.launch { context.setAutoOffTimerMinutes(minutes) } }
                )
            }
        }

        title("Controls")
        section {
            switchItem(
                title = "Hardware volume keys",
                description = "Use volume up and down buttons to adjust brightness or toggle the flashlight",
                checked = volumeButtonsFlashlight,
                onCheckedChange = { scope.launch { context.setVolumeButtonsFlashlight(it) } }
            )
        }

        title("Brightness")
        section {
            item(padding = 16.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Default brightness: $defaultFlashlightLevel%",
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = defaultFlashlightLevel.toFloat(),
                        onValueChange = { percent ->
                            scope.launch { context.setDefaultFlashlightLevel(percent.toInt()) }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        title("Screen light color")
        section {
            screenColors.forEach { (label, argb) ->
                val itemColor = Color(argb)
                val itemContentColor = if (itemColor.luminance() > 0.4f) Color(0xFF1E2022) else Color.White
                item(
                    key = argb,
                    padding = 0.dp,
                    containerColor = itemColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = screenLightColorArgb == argb,
                                role = Role.RadioButton,
                                onClick = { scope.launch { context.setScreenLightColorArgb(argb) } }
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = symbolPainter("palette"),
                            contentDescription = null,
                            tint = itemContentColor
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = itemContentColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (screenLightColorArgb == argb) {
                            Icon(
                                painter = symbolPainter("check"),
                                contentDescription = "Selected",
                                tint = itemContentColor
                            )
                        }
                    }
                }
            }
        }
        item(modifier = Modifier.fillMaxWidth()) {
            Column {
                Spacer(Modifier.height(ITEM_SPACER))
                MotionButton(
                    text = "Choose custom color",
                    icon = "palette",
                    onClick = onOpenScreenColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .motionBottomSheetAnchor(),
                    height = 56.dp,
                    fontSize = 18.sp
                )
            }
        }

        item {
            Spacer(Modifier.height(120.dp))
        }
    }

}

@Composable
fun ScreenColorSheet(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenLightColorArgb by context.screenLightColorArgbFlow()
        .collectAsState(initial = 0xFFFFFFFF.toInt())

    ScreenColorSheetContent(
        initialColor = Color(screenLightColorArgb),
        onDismiss = onBack,
        onConfirm = { color ->
            scope.launch {
                context.setScreenLightColorArgb(color.toArgb())
                onBack()
            }
        }
    )
}

@Composable
private fun ScreenColorSheetContent(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val initialArgb = initialColor.toArgb()
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb, it) }
    }
    var selectedArgb by remember(initialColor) { mutableIntStateOf(initialArgb) }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var hex by remember(initialColor) { mutableStateOf("%06X".format(initialArgb and 0xFFFFFF)) }
    val selectedColor = Color(selectedArgb)
    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(selectedArgb, it) }

    fun updateFromHsb() {
        selectedArgb = android.graphics.Color.HSVToColor(
            floatArrayOf(hue, saturation, hsv[2])
        )
        hex = "%06X".format(selectedArgb and 0xFFFFFF)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Custom screen color",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    ColorGradientSlider(
                        label = "Hue",
                        valueText = "${hue.roundToInt()}°",
                        value = hue,
                        valueRange = 0f..359f,
                        colors = hueGradientColors(),
                        onValueChange = { value ->
                            hue = value
                            updateFromHsb()
                        }
                    )
                    HorizontalDivider(thickness = 3.dp, color = colorScheme.surface)
                    ColorGradientSlider(
                        label = "Saturation",
                        valueText = "${(saturation * 100).roundToInt()}%",
                        value = saturation,
                        valueRange = 0f..1f,
                        colors = listOf(
                            Color.hsv(hue, 0f, hsv[2]),
                            Color.hsv(hue, 1f, hsv[2])
                        ),
                        onValueChange = { value ->
                            saturation = value
                            updateFromHsb()
                        }
                    )
                }
            }

            Surface(
                color = colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .requiredSize(48.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                    )
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { value ->
                            val cleaned = value
                                .removePrefix("#")
                                .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
                                .take(6)
                                .uppercase()
                            hex = cleaned
                            if (cleaned.length == 6) {
                                cleaned.toIntOrNull(16)?.let { rgb ->
                                    selectedArgb = 0xFF000000.toInt() or rgb
                                    val parsedHsv = FloatArray(3).also {
                                        android.graphics.Color.colorToHSV(selectedArgb, it)
                                    }
                                    if (parsedHsv[1] > 0f) hue = parsedHsv[0]
                                    saturation = parsedHsv[1]
                                }
                            }
                        },
                        label = { Text("Hex") },
                        prefix = { Text("#") },
                        isError = hex.length != 6,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            MotionButton(
                icon = "close",
                onClick = onDismiss,
                width = 88.dp,
                height = 56.dp,
                iconSize = 24.dp,
                defaultState = MotionButtonState(
                    backgroundColor = colorScheme.surfaceContainerHighest,
                    contentColor = colorScheme.onSurfaceVariant,
                    outlineWidth = 0.dp
                )
            )
            MotionButton(
                icon = "check",
                onClick = { onConfirm(selectedColor) },
                width = 88.dp,
                height = 56.dp,
                iconSize = 24.dp,
                defaultState = MotionButtonState(
                    backgroundColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    outlineWidth = 0.dp
                )
            )
        }
    }
}

@Composable
private fun ColorGradientSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: List<Color>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label)
            Text(valueText, color = colorScheme.onSurfaceVariant)
        }

        val interactionSource = remember { MutableInteractionSource() }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(40.dp),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    thumbSize = DpSize(6.dp, 40.dp)
                )
            },
            track = { sliderState ->
                GradientSliderTrack(sliderState, colors)
            }
        )
    }
}

private fun hueGradientColors(): List<Color> = buildList {
    for (hue in 0 until 360 step 6) {
        add(Color.hsv(hue.toFloat(), 1f, 1f))
    }
}

@Composable
private fun GradientSliderTrack(
    sliderState: SliderState,
    colors: List<Color>
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val fraction = if (isRtl) {
            1f - sliderState.coercedValueAsFraction
        } else {
            sliderState.coercedValueAsFraction
        }
        val thumbCenter = size.width * fraction
        val gap = 6.dp.toPx()
        val outsideRadius = 8.dp.toPx()
        val insideRadius = 2.dp.toPx()
        val brush = Brush.horizontalGradient(
            colors = if (isRtl) colors.reversed() else colors,
            startX = 0f,
            endX = size.width
        )

        fun drawSegment(left: Float, right: Float, leftRadius: Float, rightRadius: Float) {
            if (right <= left) return
            drawPath(
                path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = 0f,
                            right = right,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(leftRadius),
                            topRightCornerRadius = CornerRadius(rightRadius),
                            bottomRightCornerRadius = CornerRadius(rightRadius),
                            bottomLeftCornerRadius = CornerRadius(leftRadius)
                        )
                    )
                },
                brush = brush
            )
        }

        drawSegment(0f, thumbCenter - gap, outsideRadius, insideRadius)
        drawSegment(thumbCenter + gap, size.width, insideRadius, outsideRadius)
    }
}
