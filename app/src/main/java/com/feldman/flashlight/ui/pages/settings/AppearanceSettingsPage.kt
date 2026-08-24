package com.feldman.flashlight.ui.pages.settings

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.R
import com.feldman.flashlight.storage.OrientationMode
import com.feldman.flashlight.storage.orientationModeFlow
import com.feldman.flashlight.storage.setOrientationMode
import com.feldman.flashlight.ui.components.SettingsCategoryColor
import com.feldman.flashlight.ui.components.SettingsTopBar
import com.feldman.motion.MotionLevel
import com.feldman.motion.MotionScaffold
import com.feldman.motion.MotionSymbols
import com.feldman.motion.MotionThemeRepository
import com.feldman.motion.isDarkTheme
import com.feldman.motion.rememberSymbolPainter
import com.feldman.motion.symbolPainter
import com.feldman.motion.themeColors
import kotlinx.coroutines.launch

/**
 * Everything about how the app looks and moves.
 *
 * Theme, colour and motion level are motion's own settings and are written straight to its
 * [MotionThemeRepository], so they take effect on the next frame with no save step. Style and
 * orientation are the two settings here the app owns itself — orientation drives the activity's
 * requested orientation, and style picks between the Material and glass flashlight controls.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeRepository = remember(context) { MotionThemeRepository(context) }
    val scope = rememberCoroutineScope()

    val themeMode by themeRepository.themeMode.collectAsState(initial = 0)
    val themeColor by themeRepository.themeColor.collectAsState(initial = 0)
    val dynamicColor by themeRepository.dynamicColor.collectAsState(initial = true)
    val tintPalette by themeRepository.tintPalette.collectAsState(initial = false)
    val motionLevel by themeRepository.motionLevel.collectAsState(initial = MotionLevel.MEDIUM)
    val orientationMode by context.orientationModeFlow().collectAsState(initial = OrientationMode.AUTO)
    val expressiveDesign by themeRepository.expressiveDesign.collectAsState(initial = true)
    val useDark = isDarkTheme()
    val dynamicColorSeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDark) dynamicDarkColorScheme(context).primary else dynamicLightColorScheme(context).primary
    } else {
        colorScheme.primary
    }

    MotionScaffold(
        scaffoldModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        topBar = {
            SettingsTopBar(
                title = "Appearance",
                onBack = onBack,
                chromeColor = SettingsCategoryColor.APPEARANCE.container(useDark)
            )
        }
    ) {
        title("Theme")
        section {
            listOf(
                Triple(0, "System", "brightness_auto"),
                Triple(1, "Light", "light_mode"),
                Triple(2, "Dark", "dark_mode")
            ).forEach { (id, label, icon) ->
                choiceItem(
                    key = id,
                    title = label,
                    icon = symbolPainter(icon),
                    selected = themeMode == id,
                    // The item's selected content is always onPrimary, so the selected fill has to
                    // be primary for the pair to have any contrast.
                    containerColor = if (themeMode == id) colorScheme.primary else colorScheme.surfaceContainerHigh,
                    onClick = { scope.launch { themeRepository.setThemeMode(id) } }
                )
            }
        }

        title("Theme color")
        section {
            colorPickerItem(
                colors = themeColors.map { pair -> if (useDark) pair.dark else pair.light },
                selectedIndex = if (dynamicColor == true) -1 else themeColor,
                onSelected = { index ->
                    scope.launch {
                        themeRepository.setThemeColor(index)
                        themeRepository.setDynamicColor(false)
                    }
                },
                dynamicColor = dynamicColorSeed,
                dynamicColorSelected = dynamicColor == true,
                onDynamicColorSelected = {
                    scope.launch { themeRepository.setDynamicColor(true) }
                },
                dynamicColorIcon = rememberSymbolPainter(MotionSymbols.ic_hdr_auto)
            )
            switchItem(
                title = "Vibrant palette",
                description = "Use higher saturation tones",
                icon = symbolPainter(MotionSymbols.ic_contrast),
                checked = tintPalette,
                onCheckedChange = { checked ->
                    scope.launch { themeRepository.setTintPalette(checked) }
                },
                visible = dynamicColor != true
            )
        }

        title("Design")
        section {
            switchItem(
                title = "Expressive design",
                description = "Use big and bold design",
                icon = symbolPainter(MotionSymbols.ic_animation),
                iconShape = if (expressiveDesign) MaterialShapes.Cookie12Sided else MaterialShapes.Circle,
                iconMorphShape = if (expressiveDesign) MaterialShapes.Circle else MaterialShapes.Cookie12Sided,
                iconMorphOnSelection = false,
                checked = expressiveDesign,
                onCheckedChange = { checked ->
                    scope.launch { themeRepository.setExpressiveDesign(checked) }
                }
            )
        }

        title("Motion")
        section {
            listOf(
                Triple(MotionLevel.NONE, "None", "stop_circle"),
                Triple(MotionLevel.LOW, "Low", "trail_length_short"),
                Triple(MotionLevel.MEDIUM, "Medium", "trail_length_medium"),
                Triple(MotionLevel.HIGH, "High", "trail_length")
            ).forEach { (level, label, icon) ->
                choiceItem(
                    key = level.id,
                    title = label,
                    icon = symbolPainter(icon),
                    selected = motionLevel == level,
                    containerColor = if (motionLevel == level) colorScheme.primary else colorScheme.surfaceContainerHigh,
                    onClick = { scope.launch { themeRepository.setMotionLevel(level) } }
                )
            }
        }

        title("Orientation")
        section {
            segmentedPickerItem(
                options = listOf("Auto", "Portrait", "Landscape"),
                icons = listOf(
                    painterResource(R.drawable.ic_auto),
                    painterResource(R.drawable.ic_portrait),
                    painterResource(R.drawable.ic_landscape)
                ),
                selectedIndex = OrientationMode.entries.indexOf(orientationMode),
                onSelected = { index ->
                    scope.launch { context.setOrientationMode(OrientationMode.entries[index]) }
                }
            )
        }

        item {
            Spacer(Modifier.height(120.dp))
        }
    }
}
