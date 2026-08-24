package com.feldman.flashlight.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.feldman.motion.ITEM_SPACER
import com.feldman.motion.isDarkTheme
import com.feldman.motion.symbolPainter
import kotlinx.coroutines.launch

/**
 * How the torch behaves. Every choice writes straight to prefs and the flashlight page picks it up
 * on its next composition, so this page has no save step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultLightMode by context.defaultLightSourceModeFlow().collectAsState(initial = LightSourceMode.FLASH)
    val instantFlashlight by context.instantFlashlightFlow().collectAsState(initial = false)
    val autoFlashlightOff by context.autoFlashlightOffFlow().collectAsState(initial = true)
    val volumeButtonsFlashlight by context.volumeButtonsFlashlightFlow().collectAsState(initial = true)
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = 100)
    val autoOffTimerMinutes by context.autoOffTimerMinutesFlow().collectAsState(initial = 0)
    val screenLightColorArgb by context.screenLightColorArgbFlow().collectAsState(initial = 0xFFFFFFFF.toInt())
    var showCustomColorDialog by remember { mutableStateOf(false) }

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
                    onClick = { showCustomColorDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(),
                    height = 56.dp,
                    fontSize = 18.sp
                )
            }
        }

        item {
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showCustomColorDialog) {
        ScreenColorDialog(
            initialColor = Color(screenLightColorArgb),
            onDismiss = { showCustomColorDialog = false },
            onConfirm = { color ->
                scope.launch { context.setScreenLightColorArgb(color.toArgb()) }
                showCustomColorDialog = false
            }
        )
    }
}

@Composable
private fun ScreenColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialColor) { mutableFloatStateOf(initialHsv[2].coerceAtLeast(0.2f)) }
    val selectedColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom screen color") },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(selectedColor)
                )
                Spacer(Modifier.size(16.dp))
                Text("Hue")
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("Saturation")
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                Text("Brightness")
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.2f..1f)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedColor) }) { Text("Use color") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
