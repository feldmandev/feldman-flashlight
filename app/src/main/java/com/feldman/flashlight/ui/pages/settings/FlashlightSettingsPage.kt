package com.feldman.flashlight.ui.pages.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.storage.LightSourceMode
import com.feldman.flashlight.storage.autoFlashlightOffFlow
import com.feldman.flashlight.storage.defaultFlashlightLevelFlow
import com.feldman.flashlight.storage.defaultLightSourceModeFlow
import com.feldman.flashlight.storage.instantFlashlightFlow
import com.feldman.flashlight.storage.setAutoFlashlightOff
import com.feldman.flashlight.storage.setDefaultFlashlightLevel
import com.feldman.flashlight.storage.setDefaultLightSourceMode
import com.feldman.flashlight.storage.setInstantFlashlight
import com.feldman.flashlight.storage.setVolumeButtonsFlashlight
import com.feldman.flashlight.storage.volumeButtonsFlashlightFlow
import com.feldman.flashlight.ui.components.SettingsCategoryColor
import com.feldman.flashlight.ui.components.SettingsTopBar
import com.feldman.motion.MotionScaffold
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

        item {
            Spacer(Modifier.height(120.dp))
        }
    }
}
