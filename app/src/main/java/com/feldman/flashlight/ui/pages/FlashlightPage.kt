package com.feldman.flashlight.ui.pages

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.feldman.flashlight.ui.navigation.appDestinations
import com.feldman.motion.MotionNavFlow
import com.feldman.flashlight.storage.autoFlashlightOffFlow
import com.feldman.flashlight.storage.autoOffTimerMinutesFlow
import com.feldman.flashlight.storage.defaultFlashlightLevelFlow
import com.feldman.flashlight.storage.instantFlashlightFlow
import com.feldman.flashlight.storage.screenLightColorArgbFlow
import com.feldman.flashlight.ui.components.AppTopBar
import com.feldman.flashlight.ui.components.SensorCard
import com.feldman.flashlight.ui.components.SettingsAction
import com.feldman.flashlight.ui.components.TorchSlider
import com.feldman.flashlight.ui.navigation.AppDest
import com.feldman.flashlight.ui.tiles.FlashlightController
import com.feldman.motion.FontAxes
import com.feldman.motion.MotionButton
import com.feldman.motion.MotionButtonDefaults
import com.feldman.motion.MotionLevel
import com.feldman.motion.MotionScaffold
import com.feldman.motion.feldmanFont
import com.feldman.motion.rememberMotionLevel
import com.feldman.motion.symbolPainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import android.view.KeyEvent
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import com.feldman.flashlight.storage.volumeButtonsFlashlightFlow
import com.feldman.flashlight.R

import com.feldman.flashlight.storage.LightSourceMode
import com.feldman.flashlight.storage.defaultLightSourceModeFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.DialogProperties

class FlashlightPageActivity : ComponentActivity() {

    private var volumeControlsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            volumeButtonsFlashlightFlow().collect { enabled ->
                volumeControlsEnabled = enabled
            }
        }

        setContent {
            MotionNavFlow(
                startDestination = AppDest.Flashlight,
                destinations = appDestinations,
                onRootBack = { finish() },
                navigation = null
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (volumeControlsEnabled) {
            if (FlashlightController.handleVolumeKey(this, keyCode)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

/**
 * The app's only top-level page.
 *
 * The bar is transparent and the controls draw underneath it, which is what lets the page run edge
 * to edge. In landscape, the bar stays with the left control pane so the title and settings action
 * remain a single visual group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashlightPage(
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val activity = context as? Activity

    val defaultMode by context.defaultLightSourceModeFlow().collectAsState(initial = null)
    val autoOffTimerMinutes by context.autoOffTimerMinutesFlow().collectAsState(initial = 0)
    val screenLightColorArgb by context.screenLightColorArgbFlow().collectAsState(initial = 0xFFFFFFFF.toInt())
    val torchLevel by FlashlightController.torchLevel.collectAsState()
    val signalPatternActive by signalPatternActiveFlow.collectAsState()
    var userSelectedMode by rememberSaveable { mutableStateOf<String?>(null) }
    val lightMode = userSelectedMode?.let { LightSourceMode.fromKey(it) } ?: defaultMode ?: LightSourceMode.FLASH

    var isScreenLightOn by remember { mutableStateOf(false) }
    var screenBrightnessPercent by remember { mutableFloatStateOf(100f) }

    var screenPatternActive by remember { mutableStateOf(false) }
    var screenPatternLightState by remember { mutableStateOf(false) }
    var timerCancelledForSession by remember { mutableStateOf(false) }
    var timerSecondsRemaining by remember { mutableStateOf<Int?>(null) }

    val isScreenActive = isScreenLightOn && (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH)
    val screenLightColor = Color(screenLightColorArgb)
    val screenContentColor = if (screenLightColor.luminance() > 0.4f) Color(0xFF1E2022) else Color.White
    val anyLightActive = torchLevel > 0 || isScreenLightOn || screenPatternActive || signalPatternActive

    DisposableEffect(isScreenActive, screenPatternActive, screenPatternLightState, screenLightColor) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val oldLightStatusBars = controller?.isAppearanceLightStatusBars
        val oldLightNavigationBars = controller?.isAppearanceLightNavigationBars
        val lightBackground = (isScreenActive || (screenPatternActive && screenPatternLightState)) &&
            screenLightColor.luminance() > 0.4f

        controller?.isAppearanceLightStatusBars = lightBackground
        controller?.isAppearanceLightNavigationBars = lightBackground

        onDispose {
            if (oldLightStatusBars != null) controller.isAppearanceLightStatusBars = oldLightStatusBars
            if (oldLightNavigationBars != null) controller.isAppearanceLightNavigationBars = oldLightNavigationBars
        }
    }

    LaunchedEffect(anyLightActive, autoOffTimerMinutes, timerCancelledForSession) {
        if (!anyLightActive) {
            timerCancelledForSession = false
            timerSecondsRemaining = null
            return@LaunchedEffect
        }
        if (autoOffTimerMinutes <= 0 || timerCancelledForSession) {
            timerSecondsRemaining = null
            return@LaunchedEffect
        }

        var seconds = autoOffTimerMinutes * 60
        timerSecondsRemaining = seconds
        while (seconds > 0) {
            delay(1_000)
            seconds -= 1
            timerSecondsRemaining = seconds
        }
        stopBlinking(context) { screenPatternLightState = it }
        FlashlightController.setIntensity(context, 0)
        isScreenLightOn = false
        screenPatternActive = false
        timerSecondsRemaining = null
    }

    // Dynamically manage Window Screen Brightness & Keep Screen On in real-time
    DisposableEffect(isScreenActive, screenBrightnessPercent, screenPatternActive, screenPatternLightState) {
        val window = activity?.window
        val lp = window?.attributes
        val oldBrightness = lp?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        if (screenPatternActive) {
            lp?.screenBrightness = if (screenPatternLightState) {
                (screenBrightnessPercent.coerceIn(1f, 100f) / 100f).coerceIn(0.01f, 1.0f)
            } else 0.01f
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else if (isScreenActive) {
            lp?.screenBrightness = (screenBrightnessPercent.coerceIn(1f, 100f) / 100f).coerceIn(0.01f, 1.0f)
            window?.attributes = lp
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window?.attributes = lp
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            lp?.screenBrightness = oldBrightness
            window?.attributes = lp
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val pageBgColor = when {
        screenPatternActive -> if (screenPatternLightState) screenLightColor else Color(0xFF101010)
        isScreenActive -> screenLightColor
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
            containerColor = Color.Transparent,
            topBar = {
                if (!isLandscape && !screenPatternActive) {
                    AppTopBar(
                        title = "Flashlight",
                        titleColor = if (isScreenActive) screenContentColor else null,
                        actions = {
                            SettingsAction(
                                onClick = onOpenSettings,
                                tint = if (isScreenActive) screenContentColor else null
                            )
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
            ) {
                FlashlightContent(
                    lightMode = lightMode,
                    onLightModeChange = { newMode ->
                        userSelectedMode = newMode.key
                        if (newMode == LightSourceMode.SCREEN && FlashlightController.torchLevel.value > 0) {
                            FlashlightController.setIntensity(context, 0)
                        }
                    },
                    isScreenLightOn = isScreenLightOn,
                    onToggleScreenLight = { isScreenLightOn = it },
                    screenBrightnessPercent = screenBrightnessPercent,
                    onScreenBrightnessChange = { screenBrightnessPercent = it },
                    onStartScreenPattern = {
                        screenPatternActive = true
                        screenPatternLightState = true
                    },
                    onScreenPatternLightState = { isLight ->
                        screenPatternLightState = isLight
                    },
                    timerSecondsRemaining = timerSecondsRemaining,
                    onCancelTimer = {
                        timerCancelledForSession = true
                        timerSecondsRemaining = null
                    },
                    screenContentColor = screenContentColor,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Full Screen Blinking / Morse / SOS Pattern Overlay with high-visibility Stop action
        if (screenPatternActive) {
            ScreenPatternOverlay(
                isLightState = screenPatternLightState,
                lightColor = screenLightColor,
                onStop = {
                    stopBlinking(context) { screenPatternLightState = it }
                    screenPatternActive = false
                }
            )
        }
    }
}

/**
 * Full-screen blinking / morse / SOS pattern overlay.
 * Alternates between light (white) and dark states, with an always-visible primary-colored stop button.
 */
@Composable
fun ScreenPatternOverlay(
    isLightState: Boolean,
    lightColor: Color = Color.White,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(Unit) {
        val window = activity?.window
        val lp = window?.attributes
        val oldBrightness = lp?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        lp?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window?.attributes = lp
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            lp?.screenBrightness = oldBrightness
            window?.attributes = lp
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val bgColor = if (isLightState) lightColor else Color(0xFF101010)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .semantics {
                contentDescription = "Signal pattern active. Tap to stop."
                role = Role.Button
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onStop
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = onStop,
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .padding(bottom = 54.dp)
                .height(58.dp)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Stop",
                    fontFamily = feldmanFont(width = 125f, round = 100f, weight = 700),
                    fontSize = 19.sp
                )
            }
        }
    }
}

@Composable
private fun TorchStatusBanner(message: String, isError: Boolean) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun AutoOffCountdownBar(seconds: Int, onCancel: () -> Unit) {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Turns off in %d:%02d".format(minutes, remainingSeconds),
                style = MaterialTheme.typography.labelLarge
            )
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FlashlightContent(
    lightMode: LightSourceMode,
    onLightModeChange: (LightSourceMode) -> Unit,
    isScreenLightOn: Boolean,
    onToggleScreenLight: (Boolean) -> Unit,
    screenBrightnessPercent: Float,
    onScreenBrightnessChange: (Float) -> Unit,
    onStartScreenPattern: () -> Unit,
    onScreenPatternLightState: (Boolean) -> Unit,
    timerSecondsRemaining: Int?,
    onCancelTimer: () -> Unit,
    screenContentColor: Color,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context) {
        val stopListening = FlashlightController.startListening(context)
        onDispose(stopListening)
    }
    val torchStatus by FlashlightController.status.collectAsState()
    val maxLevel = remember(torchStatus.hasFlash) { FlashlightController.getMaxLevel(context) }
    val luminance = rememberAmbientLuminance(context)

    val autoFlashlightOff by context.autoFlashlightOffFlow().collectAsState(initial = true)
    val instantFlashlight by context.instantFlashlightFlow().collectAsState(initial = false)
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = 100)

    val level by FlashlightController.torchLevel.collectAsState()
    val isOn = level > 0

    LaunchedEffect(torchStatus.hasFlash) {
        if (!torchStatus.hasFlash && lightMode != LightSourceMode.SCREEN) {
            onLightModeChange(LightSourceMode.SCREEN)
        }
    }

    DisposableEffect(lifecycleOwner, autoFlashlightOff) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && autoFlashlightOff) {
                FlashlightController.setIntensity(context, 0)
                onToggleScreenLight(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(instantFlashlight, defaultFlashlightLevel) {
        if (instantFlashlight && level == 0) {
            val brightness = (defaultFlashlightLevel / 100f * maxLevel).toInt().coerceIn(0, maxLevel)
            if (lightMode != LightSourceMode.SCREEN) {
                FlashlightController.setIntensity(context, brightness)
            }
        }
    }

    fun fallbackHalf(): Int =
        if (maxLevel > 1) maxOf(1, (maxLevel * 0.5f).toInt()) else 1

    fun applyTorchLevel(newLevel: Int) {
        val clamped = newLevel.coerceIn(0, maxLevel)
        FlashlightController.setIntensity(context, clamped)
    }

    var showBlinkDialog by remember { mutableStateOf(false) }
    var showMorseDialog by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }

    val isScreenActive = isScreenLightOn && (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH)

    // Dynamic light styling for pure white flashlight mode
    val cardColor = if (isScreenActive) Color(0xFFF7F8FA) else MaterialTheme.colorScheme.surfaceContainer
    val cardValueColor = if (isScreenActive) Color(0xFF1E2022) else MaterialTheme.colorScheme.onSurface
    val cardLabelColor = if (isScreenActive) Color(0xFF60646C) else MaterialTheme.colorScheme.onSurfaceVariant
    val cardBorder = if (isScreenActive) BorderStroke(1.dp, Color(0xFFE5E8EB)) else null

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == ORIENTATION_LANDSCAPE
    val screenHeightDp = configuration.screenHeightDp

    // Dynamic responsive heights: chunky and thick on tall screens, auto-resized down on shorter/wider screens so both columns align with exact matching height
    val (presetBtnHeight, flashBtnHeight, btnSpacing) = if (isLandscape) {
        val availableColHeight = (screenHeightDp - 60).coerceIn(180, 360)
        val spacing = if (availableColHeight < 240) 6.dp else 8.dp
        val pHeight = ((availableColHeight - 3 * spacing.value) / 4).coerceIn(38f, 84f).dp
        val totalHeight = pHeight * 4 + spacing * 3
        val fHeight = ((totalHeight.value - 2 * spacing.value) / 3).dp
        Triple(pHeight, fHeight, spacing)
    } else {
        val pHeight = when {
            screenHeightDp >= 860 -> 68.dp
            screenHeightDp >= 780 -> 60.dp
            screenHeightDp >= 680 -> 52.dp
            screenHeightDp >= 580 -> 44.dp
            else -> 38.dp
        }
        val spacing = if (screenHeightDp >= 780) 10.dp else 8.dp
        val totalHeight = pHeight * 4 + spacing * 3
        val fHeight = ((totalHeight.value - 2 * spacing.value) / 3).dp
        Triple(pHeight, fHeight, spacing)
    }

    val percent = when (lightMode) {
        LightSourceMode.FLASH -> if (maxLevel <= 1) (if (level > 0) 100 else 0) else ((level.toFloat() / maxLevel) * 100).toInt()
        LightSourceMode.SCREEN -> if (isScreenLightOn) screenBrightnessPercent.toInt() else 0
        LightSourceMode.BOTH -> if (isScreenLightOn) screenBrightnessPercent.toInt() else if (level > 0) ((level.toFloat() / maxLevel) * 100).toInt() else 0
    }

    val effectiveSliderLevel = when (lightMode) {
        LightSourceMode.FLASH -> level
        LightSourceMode.SCREEN -> if (isScreenLightOn) (screenBrightnessPercent / 100f * maxLevel).toInt().coerceIn(1, maxLevel) else 0
        LightSourceMode.BOTH -> if (isScreenLightOn || level > 0) maxOf(level, (screenBrightnessPercent / 100f * maxLevel).toInt().coerceIn(1, maxLevel)) else 0
    }

    val segmentColors = if (isScreenActive) {
        SegmentedButtonDefaults.colors(
            activeContainerColor = Color.White,
            activeContentColor = Color(0xFF1E2022),
            inactiveContainerColor = Color(0xFFF2F4F7),
            inactiveContentColor = Color(0xFF666B73),
            activeBorderColor = Color(0xFFCFD4DC),
            inactiveBorderColor = Color(0xFFE2E4E8)
        )
    } else {
        SegmentedButtonDefaults.colors()
    }

    val sliderColors = if (isScreenActive) {
        SliderDefaults.colors(
            activeTrackColor = Color(0xFFDCDFE4),
            inactiveTrackColor = Color(0xFFF2F4F7),
            thumbColor = Color(0xFF33373D)
        )
    } else {
        SliderDefaults.colors()
    }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Half: Mode Picker + Sensor Cards + Torch Slider
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppTopBar(
                    title = "Flashlight",
                    titleColor = if (isScreenActive) screenContentColor else null,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    actions = {
                        SettingsAction(
                            onClick = onOpenSettings,
                            tint = if (isScreenActive) screenContentColor else null
                        )
                    }
                )

                // 1. Light Mode Picker (Flash / Screen / Both)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LightSourceMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = lightMode == mode,
                            enabled = mode == LightSourceMode.SCREEN || torchStatus.hasFlash,
                            onClick = {
                                onLightModeChange(mode)
                                if (mode == LightSourceMode.SCREEN && level > 0) {
                                    applyTorchLevel(0)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = LightSourceMode.entries.size
                            ),
                            colors = segmentColors,
                            border = SegmentedButtonDefaults.borderStroke(
                                color = if (isScreenActive) (if (lightMode == mode) Color(0xFFCFD4DC) else Color(0xFFE2E4E8)) else MaterialTheme.colorScheme.outline
                            ),
                            label = {
                                Text(
                                    text = mode.label,
                                    fontWeight = if (lightMode == mode) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                torchStatus.message?.let { message ->
                    TorchStatusBanner(message = message, isError = torchStatus.hasFlash)
                }

                timerSecondsRemaining?.let { seconds ->
                    AutoOffCountdownBar(seconds = seconds, onCancel = onCancelTimer)
                }

                // 2. Sensor Info Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SensorCard(
                        icon = R.drawable.ic_brightness,
                        label = "Brightness",
                        value = "$percent%",
                        cornerRadius = 20.dp,
                        color = cardColor,
                        valueColor = cardValueColor,
                        labelColor = cardLabelColor,
                        border = cardBorder,
                        iconShapeIndex = 1
                    )
                    SensorCard(
                        icon = R.drawable.ic_layers,
                        label = if (lightMode == LightSourceMode.SCREEN) "Screen" else "Level",
                        value = if (lightMode == LightSourceMode.SCREEN) (if (isScreenLightOn) "Active" else "Off") else "$level/$maxLevel",
                        cornerRadius = 20.dp,
                        color = cardColor,
                        valueColor = cardValueColor,
                        labelColor = cardLabelColor,
                        border = cardBorder,
                        iconShapeIndex = 2
                    )
                    SensorCard(
                        icon = R.drawable.ic_light,
                        label = "Luminance",
                        value = "${luminance.toInt()} lx",
                        cornerRadius = 20.dp,
                        color = cardColor,
                        valueColor = cardValueColor,
                        labelColor = cardLabelColor,
                        border = cardBorder,
                        iconShapeIndex = 3
                    )
                }

                // 3. Torch Brightness Slider
                TorchSlider(
                    level = effectiveSliderLevel,
                    maxLevel = maxLevel,
                    applyLevel = { newLevel ->
                        val pct = if (maxLevel > 0) (newLevel.toFloat() / maxLevel * 100f) else 0f
                        when (lightMode) {
                            LightSourceMode.FLASH -> {
                                applyTorchLevel(newLevel)
                            }
                            LightSourceMode.SCREEN -> {
                                onScreenBrightnessChange(pct)
                                onToggleScreenLight(newLevel > 0)
                            }
                            LightSourceMode.BOTH -> {
                                applyTorchLevel(newLevel)
                                onScreenBrightnessChange(pct)
                                onToggleScreenLight(newLevel > 0)
                            }
                        }
                    },
                    colors = sliderColors
                )
            }

            // Right Half: 2 action columns (Presets on Left, Flash Modes on Right)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    PresetButtonColumn(
                        maxLevel = maxLevel,
                        applyLevel = { targetLevel ->
                            val pct = if (maxLevel > 0) (targetLevel.toFloat() / maxLevel * 100f) else 0f
                            when (lightMode) {
                                LightSourceMode.FLASH -> {
                                    applyTorchLevel(targetLevel)
                                }
                                LightSourceMode.SCREEN -> {
                                    onScreenBrightnessChange(pct)
                                    onToggleScreenLight(targetLevel > 0)
                                }
                                LightSourceMode.BOTH -> {
                                    applyTorchLevel(targetLevel)
                                    onScreenBrightnessChange(pct)
                                    onToggleScreenLight(targetLevel > 0)
                                }
                            }
                        },
                        fallbackHalf = ::fallbackHalf,
                        buttonHeight = presetBtnHeight,
                        spacing = btnSpacing,
                        isScreenActive = isScreenActive
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    FlashModeColumn(
                        onBlink = { showBlinkDialog = true },
                        onMorse = { showMorseDialog = true },
                        onSos = { showSosDialog = true },
                        buttonHeight = flashBtnHeight,
                        spacing = btnSpacing,
                        isScreenActive = isScreenActive
                    )
                }
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Light Mode Picker (Flash / Screen / Both)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                LightSourceMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = lightMode == mode,
                        enabled = mode == LightSourceMode.SCREEN || torchStatus.hasFlash,
                        onClick = {
                            onLightModeChange(mode)
                            if (mode == LightSourceMode.SCREEN && level > 0) {
                                applyTorchLevel(0)
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = LightSourceMode.entries.size
                        ),
                        colors = segmentColors,
                        border = SegmentedButtonDefaults.borderStroke(
                            color = if (isScreenActive) (if (lightMode == mode) Color(0xFFCFD4DC) else Color(0xFFE2E4E8)) else MaterialTheme.colorScheme.outline
                        ),
                        label = {
                            Text(
                                text = mode.label,
                                fontWeight = if (lightMode == mode) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            torchStatus.message?.let { message ->
                TorchStatusBanner(message = message, isError = torchStatus.hasFlash)
            }

            timerSecondsRemaining?.let { seconds ->
                AutoOffCountdownBar(seconds = seconds, onCancel = onCancelTimer)
            }

            // 2. Sensor Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SensorCard(
                    icon = R.drawable.ic_brightness,
                    label = "Brightness",
                    value = "$percent%",
                    cornerRadius = 28.dp,
                    color = cardColor,
                    valueColor = cardValueColor,
                    labelColor = cardLabelColor,
                    border = cardBorder,
                    iconShapeIndex = 1
                )
                SensorCard(
                    icon = R.drawable.ic_layers,
                    label = if (lightMode == LightSourceMode.SCREEN) "Screen" else "Level",
                    value = if (lightMode == LightSourceMode.SCREEN) (if (isScreenLightOn) "Active" else "Off") else "$level/$maxLevel",
                    cornerRadius = 28.dp,
                    color = cardColor,
                    valueColor = cardValueColor,
                    labelColor = cardLabelColor,
                    border = cardBorder,
                    iconShapeIndex = 2
                )
                SensorCard(
                    icon = R.drawable.ic_light,
                    label = "Luminance",
                    value = "${luminance.toInt()} lx",
                    cornerRadius = 28.dp,
                    color = cardColor,
                    valueColor = cardValueColor,
                    labelColor = cardLabelColor,
                    border = cardBorder,
                    iconShapeIndex = 3
                )
            }

            // 3. Middle Action Columns: Brightness Presets on Left, Flash Modes on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Quick Brightness Presets (100%, 50%, 33%, 0%)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    PresetButtonColumn(
                        maxLevel = maxLevel,
                        applyLevel = { targetLevel ->
                            val pct = if (maxLevel > 0) (targetLevel.toFloat() / maxLevel * 100f) else 0f
                            when (lightMode) {
                                LightSourceMode.FLASH -> {
                                    applyTorchLevel(targetLevel)
                                }
                                LightSourceMode.SCREEN -> {
                                    onScreenBrightnessChange(pct)
                                    onToggleScreenLight(targetLevel > 0)
                                }
                                LightSourceMode.BOTH -> {
                                    applyTorchLevel(targetLevel)
                                    onScreenBrightnessChange(pct)
                                    onToggleScreenLight(targetLevel > 0)
                                }
                            }
                        },
                        fallbackHalf = ::fallbackHalf,
                        buttonHeight = presetBtnHeight,
                        spacing = btnSpacing,
                        isScreenActive = isScreenActive
                    )
                }

                // Right Column: Flash Modes (Blink, Morse, SOS)
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    FlashModeColumn(
                        onBlink = { showBlinkDialog = true },
                        onMorse = { showMorseDialog = true },
                        onSos = { showSosDialog = true },
                        buttonHeight = flashBtnHeight,
                        spacing = btnSpacing,
                        isScreenActive = isScreenActive
                    )
                }
            }

            // 4. Torch Brightness Slider (adjusts screen / torch brightness in real-time)
            TorchSlider(
                level = effectiveSliderLevel,
                maxLevel = maxLevel,
                applyLevel = { newLevel ->
                    val pct = if (maxLevel > 0) (newLevel.toFloat() / maxLevel * 100f) else 0f
                    when (lightMode) {
                        LightSourceMode.FLASH -> {
                            applyTorchLevel(newLevel)
                        }
                        LightSourceMode.SCREEN -> {
                            onScreenBrightnessChange(pct)
                            onToggleScreenLight(newLevel > 0)
                        }
                        LightSourceMode.BOTH -> {
                            applyTorchLevel(newLevel)
                            onScreenBrightnessChange(pct)
                            onToggleScreenLight(newLevel > 0)
                        }
                    }
                },
                colors = sliderColors
            )
        }
    }

    if (showBlinkDialog) {
        BlinkFlashDialog(
            context = context,
            maxLevel = maxLevel,
            lightMode = lightMode,
            onStartScreenPattern = { interval, brightnessPercent ->
                onStartScreenPattern()
                startBlinking(
                    context = context,
                    lightMode = lightMode,
                    intervalState = mutableFloatStateOf(interval),
                    brightnessState = mutableFloatStateOf(
                        (brightnessPercent / 100f * maxLevel).coerceIn(0f, maxLevel.toFloat())
                    ),
                    onScreenState = onScreenPatternLightState
                )
            },
            onDismiss = { showBlinkDialog = false }
        )
    }
    if (showMorseDialog) {
        MorseFlashDialog(
            context = context,
            maxLevel = maxLevel,
            lightMode = lightMode,
            onStartScreenPattern = { msg, wpmVal, brightVal ->
                onStartScreenPattern()
                startMorse(
                    context = context,
                    message = msg,
                    wpm = wpmVal,
                    brightnessPercent = brightVal,
                    maxLevel = maxLevel,
                    lightMode = lightMode,
                    onScreenState = onScreenPatternLightState
                )
            },
            onDismiss = { showMorseDialog = false }
        )
    }
    if (showSosDialog) {
        SOSFlashDialog(
            context = context,
            maxLevel = maxLevel,
            lightMode = lightMode,
            onStartScreenPattern = { wpmVal, brightVal ->
                onStartScreenPattern()
                startMorse(
                    context = context,
                    message = "SOS",
                    wpm = wpmVal,
                    brightnessPercent = brightVal,
                    maxLevel = maxLevel,
                    lightMode = lightMode,
                    onScreenState = onScreenPatternLightState
                )
            },
            onDismiss = { showSosDialog = false }
        )
    }
}

/**
 * The Blink / Morse / SOS vertical column.
 */
@Composable
private fun FlashModeColumn(
    onBlink: () -> Unit,
    onMorse: () -> Unit,
    onSos: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 64.dp,
    spacing: Dp = 8.dp,
    isScreenActive: Boolean = false
) {
    val motionLevel = rememberMotionLevel()
    val defaultState = if (isScreenActive) {
        MotionButtonDefaults.default(motionLevel).copy(
            backgroundColor = Color(0xFFF2F4F7),
            contentColor = Color(0xFF1E2022),
            fontAxes = FontAxes(weight = 600, width = 125f)
        )
    } else {
        MotionButtonDefaults.default(motionLevel).copy(
            backgroundColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            fontAxes = FontAxes(weight = 600, width = 125f)
        )
    }
    val defaultPressedState = MotionButtonDefaults.defaultPressed(motionLevel, from = defaultState).copy(
        fontAxes = when (motionLevel) {
            MotionLevel.NONE, MotionLevel.LOW -> FontAxes(weight = 600, width = 125f)
            MotionLevel.MEDIUM -> FontAxes(weight = 550, width = 135f)
            MotionLevel.HIGH -> FontAxes(weight = 500, width = 140f)
        }
    )

    val modes = listOf(
        Triple("Blink", "flash_on", onBlink),
        Triple("Morse", "graphic_eq", onMorse),
        Triple("SOS", "sos", onSos)
    )

    val fontSize = if (buttonHeight >= 80.dp) 18.sp else if (buttonHeight >= 55.dp) 17.sp else 15.sp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val colWidth = maxWidth
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            modes.forEach { (label, iconName, onClick) ->
                MotionButton(
                    text = label,
                    icon = iconName,
                    onClick = onClick,
                    width = colWidth,
                    height = buttonHeight,
                    fontSize = fontSize,
                    defaultState = defaultState,
                    defaultPressedState = defaultPressedState,
                    motionLevel = motionLevel
                )
            }
        }
    }
}

/**
 * Expressive Dialog Header with a rounded icon container and bold Feldman typography.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveDialogHeader(
    title: String,
    subtitle: String,
    icon: String,
    iconContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    iconContainerShape: Shape = RoundedCornerShape(16.dp)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconContainerColor, iconContainerShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = symbolPainter(icon),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = iconColor
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = feldmanFont(width = 125f, weight = 700),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Row content for a dialog slider: label, icon, value pill, and a smooth slider. Meant to sit
 * inside a connected [MotionScaffold] section `item`, which supplies the card background and the
 * corner radius that connects it to its neighbours.
 */
@Composable
private fun ExpressiveDialogSlider(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = symbolPainter(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = feldmanFont(width = 125f, weight = 650),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Row content for the Morse message field, sized the same way as [ExpressiveDialogSlider] so it
 * connects cleanly above it in a shared section.
 */
@Composable
private fun MessageInputRow(
    message: String,
    onMessageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = symbolPainter("edit"),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Message",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        androidx.compose.material3.OutlinedTextField(
            value = message,
            onValueChange = { onMessageChange(it.uppercase()) },
            placeholder = { Text("Enter message...") },
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BlinkFlashDialog(
    context: Context,
    maxLevel: Int,
    lightMode: LightSourceMode,
    onStartScreenPattern: (interval: Float, brightnessPercent: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val dialogModifier = if (isLandscape) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    } else {
        Modifier
    }
    val dialogContentModifier = if (isLandscape) {
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }
    var interval by remember { mutableFloatStateOf(300f) }
    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isBlinking by remember { mutableStateOf(false) }
    val motionLevel = rememberMotionLevel()

    AlertDialog(
        modifier = dialogModifier,
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape),
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            ExpressiveDialogHeader(
                title = "Strobe Blink",
                subtitle = "Periodic flashing strobe pulse",
                icon = "flash_on",
                iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                iconContainerShape = MaterialShapes.Diamond.toShape()
            )
        },
        text = {
            Column(
                modifier = dialogContentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MotionScaffold(
                    contentModifier = Modifier,
                    scrollable = false,
                    containerColor = Color.Transparent,
                    fitContentHeight = true
                ) {
                    section {
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Interval",
                                valueLabel = "${interval.toInt()} ms",
                                value = interval,
                                onValueChange = { interval = it },
                                valueRange = 100f..1000f,
                                icon = "timer"
                            )
                        }
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Brightness",
                                valueLabel = "${brightnessPercent.toInt()}%",
                                value = brightnessPercent,
                                onValueChange = { brightnessPercent = it },
                                valueRange = 1f..100f,
                                icon = "brightness_high"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val actionButtonState = if (isBlinking) {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                } else {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                }
                val actionPressedState = MotionButtonDefaults.defaultPressed(motionLevel, from = actionButtonState)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    MotionButton(
                        text = if (isBlinking) "Stop Strobe" else "Start Strobe",
                        icon = if (isBlinking) "stop" else "flash_on",
                        onClick = {
                            if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                                onStartScreenPattern(interval, brightnessPercent)
                                onDismiss()
                            } else {
                                if (isBlinking) {
                                    stopBlinking(context)
                                    isBlinking = false
                                } else {
                                    startBlinking(
                                        context = context,
                                        lightMode = lightMode,
                                        intervalState = derivedStateOf { interval },
                                        brightnessState = derivedStateOf {
                                            (brightnessPercent / 100f * maxLevel).coerceIn(0f, maxLevel.toFloat())
                                        }
                                    )
                                    isBlinking = true
                                }
                            }
                        },
                        width = maxWidth,
                        height = 54.dp,
                        fontSize = 17.sp,
                        defaultState = actionButtonState,
                        defaultPressedState = actionPressedState,
                        motionLevel = motionLevel
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    stopBlinking(context)
                    onDismiss()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = feldmanFont(width = 120f, weight = 600)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorseFlashDialog(
    context: Context,
    maxLevel: Int,
    lightMode: LightSourceMode,
    onStartScreenPattern: (message: String, wpm: Float, brightnessPercent: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val dialogModifier = if (isLandscape) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    } else {
        Modifier
    }
    val dialogContentModifier = if (isLandscape) {
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }
    var message by remember { mutableStateOf("HELLO") }
    var wpm by remember { mutableFloatStateOf(15f) }

    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    val motionLevel = rememberMotionLevel()

    AlertDialog(
        modifier = dialogModifier,
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape),
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            ExpressiveDialogHeader(
                title = "Morse Code",
                subtitle = "Optical telegraph transmission",
                icon = "graphic_eq",
                iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                iconContainerShape = MaterialShapes.ClamShell.toShape()
            )
        },
        text = {
            Column(
                modifier = dialogContentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MotionScaffold(
                    contentModifier = Modifier,
                    scrollable = false,
                    containerColor = Color.Transparent,
                    fitContentHeight = true
                ) {
                    section {
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            MessageInputRow(
                                message = message,
                                onMessageChange = { message = it }
                            )
                        }
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Transmission Speed",
                                valueLabel = "${wpm.toInt()} WPM",
                                value = wpm,
                                onValueChange = { wpm = it },
                                valueRange = 5f..35f,
                                icon = "speed"
                            )
                        }
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Brightness",
                                valueLabel = "${brightnessPercent.toInt()}%",
                                value = brightnessPercent,
                                onValueChange = { brightnessPercent = it },
                                valueRange = 1f..100f,
                                icon = "brightness_high"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val actionButtonState = if (isPlaying) {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                } else {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                }
                val actionPressedState = MotionButtonDefaults.defaultPressed(motionLevel, from = actionButtonState)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    MotionButton(
                        text = if (isPlaying) "Stop Transmission" else "Transmit Morse",
                        icon = if (isPlaying) "stop" else "graphic_eq",
                        onClick = {
                            if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                                onStartScreenPattern(message, wpm, brightnessPercent)
                                onDismiss()
                            } else {
                                if (isPlaying) {
                                    stopBlinking(context)
                                    isPlaying = false
                                } else {
                                    isPlaying = true
                                    startMorse(context, message, wpm, brightnessPercent, maxLevel, lightMode)
                                }
                            }
                        },
                        width = maxWidth,
                        height = 54.dp,
                        fontSize = 17.sp,
                        defaultState = actionButtonState,
                        defaultPressedState = actionPressedState,
                        motionLevel = motionLevel
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    stopBlinking(context)
                    onDismiss()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = feldmanFont(width = 120f, weight = 600)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SOSFlashDialog(
    context: Context,
    maxLevel: Int,
    lightMode: LightSourceMode,
    onStartScreenPattern: (wpm: Float, brightnessPercent: Float) -> Unit,
    onDismiss: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.orientation == ORIENTATION_LANDSCAPE
    val dialogModifier = if (isLandscape) {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    } else {
        Modifier
    }
    val dialogContentModifier = if (isLandscape) {
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }
    var wpm by remember { mutableFloatStateOf(20f) }

    val defaultFlashlightLevel by context.defaultFlashlightLevelFlow().collectAsState(initial = -1)
    var brightnessPercent by remember { mutableFloatStateOf(100f) }

    LaunchedEffect(defaultFlashlightLevel) {
        if (defaultFlashlightLevel != -1) {
            brightnessPercent = defaultFlashlightLevel.toFloat()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    val motionLevel = rememberMotionLevel()

    AlertDialog(
        modifier = dialogModifier,
        properties = DialogProperties(usePlatformDefaultWidth = !isLandscape),
        onDismissRequest = {
            stopBlinking(context)
            onDismiss()
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            ExpressiveDialogHeader(
                title = "Emergency SOS",
                subtitle = "International distress signal",
                icon = "sos",
                iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                iconColor = MaterialTheme.colorScheme.onErrorContainer,
                iconContainerShape = MaterialShapes.ClamShell.toShape()
            )
        },
        text = {
            Column(
                modifier = dialogContentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MotionScaffold(
                    contentModifier = Modifier,
                    scrollable = false,
                    containerColor = Color.Transparent,
                    fitContentHeight = true
                ) {
                    section {
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Signal Speed",
                                valueLabel = "${wpm.toInt()} WPM",
                                value = wpm,
                                onValueChange = { wpm = it },
                                valueRange = 5f..40f,
                                icon = "speed"
                            )
                        }
                        item(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            ExpressiveDialogSlider(
                                title = "Brightness",
                                valueLabel = "${brightnessPercent.toInt()}%",
                                value = brightnessPercent,
                                onValueChange = { brightnessPercent = it },
                                valueRange = 1f..100f,
                                icon = "brightness_high"
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val actionButtonState = if (isPlaying) {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                } else {
                    MotionButtonDefaults.default(motionLevel).copy(
                        backgroundColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        fontAxes = FontAxes(weight = 600, width = 125f)
                    )
                }
                val actionPressedState = MotionButtonDefaults.defaultPressed(motionLevel, from = actionButtonState)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    MotionButton(
                        text = if (isPlaying) "Stop SOS" else "Transmit SOS",
                        icon = if (isPlaying) "stop" else "sos",
                        onClick = {
                            if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                                onStartScreenPattern(wpm, brightnessPercent)
                                onDismiss()
                            } else {
                                if (isPlaying) {
                                    stopBlinking(context)
                                    isPlaying = false
                                } else {
                                    isPlaying = true
                                    startMorse(context, "SOS", wpm, brightnessPercent, maxLevel, lightMode)
                                }
                            }
                        },
                        width = maxWidth,
                        height = 54.dp,
                        fontSize = 17.sp,
                        defaultState = actionButtonState,
                        defaultPressedState = actionPressedState,
                        motionLevel = motionLevel
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    stopBlinking(context)
                    onDismiss()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = feldmanFont(width = 120f, weight = 600)
                )
            }
        }
    )
}

private val MORSE_CODE_MAP = mapOf(
    // 🇺🇸 English / Latin A–Z
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",

    // 🇩🇪 German (Umlauts)
    'Ä' to ".-.-", 'Ö' to "---.", 'Ü' to "..--", 'ẞ' to "...--..",

    // 🇫🇷 French (accents)
    'À' to ".--.-", 'Â' to ".-", 'Æ' to ".-.-", 'Ç' to "-.-..", 'É' to "..-..", 'È' to ".-..-",
    'Ê' to "-..-.", 'Ë' to "..-..", 'Ô' to "---.", 'Œ' to "---.", 'Ù' to "..--", 'Û' to "..--",
    'Ü' to "..--", 'Ÿ' to "-.--..",

    // 🇷🇺 Russian / Cyrillic
    'А' to ".-", 'Б' to "-...", 'В' to ".--", 'Г' to "--.", 'Д' to "-..", 'E' to ".",
    'Ж' to "...-", 'З' to "--..", 'И' to "..", 'Й' to ".---", 'К' to "-.-", 'Л' to ".-..",
    'М' to "--", 'Н' to "-.", 'О' to "---", 'П' to ".--.", 'Р' to ".-.", 'С' to "...",
    'Т' to "-", 'У' to "..-", 'Ф' to "..-.", 'Х' to "....", 'Ц' to "-.-.", 'Ч' to "---.",
    'Ш' to "----", 'Щ' to "--.-", 'Ъ' to "--.--", 'Ы' to "-.--", 'Ь' to "-..-", 'Э' to "..-..",
    'Ю' to "..--", 'Я' to ".-.-",

    // 🇮🇱 Hebrew (based on ITU-Wabun extensions)
    'א' to ".-", 'ב' to "-...", 'ג' to "--.", 'ד' to "-..", 'ה' to "....", 'ו' to ".--",
    'ז' to "--..", 'ח' to "----", 'ט' to "-", 'י' to "..", 'כ' to "-.-", 'ל' to ".-..",
    'מ' to "--", 'נ' to "-.", 'ס' to "...", 'ע' to "---", 'פ' to ".--.", 'צ' to "-.-.",
    'ק' to "--.-", 'ר' to ".-.", 'ש' to "----", 'ת' to "-",

    // Numbers
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
)

private val _signalPatternActive = MutableStateFlow(false)
private val signalPatternActiveFlow = _signalPatternActive.asStateFlow()

fun startMorse(
    context: Context,
    message: String,
    wpm: Float,
    brightnessPercent: Float,
    maxLevel: Int,
    lightMode: LightSourceMode = LightSourceMode.FLASH,
    onScreenState: (Boolean) -> Unit = {}
) {
    blinkJob?.cancel()
    _signalPatternActive.value = true
    val job = CoroutineScope(Dispatchers.Default).launch {
        val dot = (1200 / wpm).toLong()
        val dash = dot * 3
        val gap = dot
        val letterGap = dot * 3
        val wordGap = dot * 7
        val brightness = (brightnessPercent / 100f * maxLevel).toInt().coerceAtLeast(1)

        while (isActive) {
            for (char in message.uppercase()) {
                if (!isActive) break

                if (char == ' ') {
                    if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
                        FlashlightController.setIntensity(context, 0)
                    }
                    if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                        onScreenState(false)
                    }
                    delay(wordGap)
                    continue
                }

                val code = MORSE_CODE_MAP[char]
                if (code == null) {
                    if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
                        FlashlightController.setIntensity(context, 0)
                    }
                    if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                        onScreenState(false)
                    }
                    delay(letterGap)
                    continue
                }

                for (symbol in code) {
                    if (!isActive) break
                    val duration = if (symbol == '.') dot else dash

                    if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
                        FlashlightController.setIntensity(context, brightness)
                    }
                    if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                        onScreenState(true)
                    }
                    delay(duration)

                    if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
                        FlashlightController.setIntensity(context, 0)
                    }
                    if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                        onScreenState(false)
                    }
                    delay(gap)
                }

                delay(letterGap)
            }
            if (message.uppercase() != "SOS") break
            delay(wordGap)
        }

        if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
            FlashlightController.setIntensity(context, 0)
        }
        if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
            onScreenState(false)
        }
    }
    blinkJob = job
    job.invokeOnCompletion {
        if (blinkJob === job) _signalPatternActive.value = false
    }
}

private var blinkJob: Job? = null

fun startBlinking(
    context: Context,
    lightMode: LightSourceMode = LightSourceMode.FLASH,
    intervalState: State<Float>,
    brightnessState: State<Float>,
    onScreenState: (Boolean) -> Unit = {}
) {
    blinkJob?.cancel()
    _signalPatternActive.value = true
    val job = CoroutineScope(Dispatchers.Default).launch {
        var isOn = true
        while (isActive) {
            val interval = intervalState.value.toInt()
            val brightness = brightnessState.value.toInt()

            if (lightMode == LightSourceMode.FLASH || lightMode == LightSourceMode.BOTH) {
                FlashlightController.setIntensity(context, if (isOn) brightness else 0)
            }
            if (lightMode == LightSourceMode.SCREEN || lightMode == LightSourceMode.BOTH) {
                onScreenState(isOn)
            }
            isOn = !isOn
            delay(interval.toLong())
        }
    }
    blinkJob = job
    job.invokeOnCompletion {
        if (blinkJob === job) _signalPatternActive.value = false
    }
}

fun stopBlinking(context: Context? = null, onScreenState: ((Boolean) -> Unit)? = null) {
    blinkJob?.cancel()
    blinkJob = null
    _signalPatternActive.value = false
    context?.let { FlashlightController.setIntensity(it, 0) }
    onScreenState?.invoke(false)
}


/**
 * The vertical 100% / 50% / 33% / 0% quick level column using standard MotionButton with secondary color.
 */
@Composable
fun PresetButtonColumn(
    maxLevel: Int,
    applyLevel: (Int) -> Unit,
    fallbackHalf: () -> Int,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 46.dp,
    spacing: Dp = 8.dp,
    isScreenActive: Boolean = false
) {
    val motionLevel = rememberMotionLevel()
    val defaultState = if (isScreenActive) {
        MotionButtonDefaults.default(motionLevel).copy(
            backgroundColor = Color(0xFFF2F4F7),
            contentColor = Color(0xFF1E2022),
            fontAxes = FontAxes(weight = 600, width = 125f)
        )
    } else {
        MotionButtonDefaults.default(motionLevel).copy(
            backgroundColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            fontAxes = FontAxes(weight = 600, width = 125f)
        )
    }
    val defaultPressedState = MotionButtonDefaults.defaultPressed(motionLevel, from = defaultState).copy(
        fontAxes = when (motionLevel) {
            MotionLevel.NONE, MotionLevel.LOW -> FontAxes(weight = 600, width = 125f)
            MotionLevel.MEDIUM -> FontAxes(weight = 550, width = 135f)
            MotionLevel.HIGH -> FontAxes(weight = 500, width = 140f)
        }
    )

    val options = listOf("Max", "Medium", "Low", "Off")
    val icons = listOf(
        "brightness_high",
        "brightness_medium",
        "brightness_low",
        "brightness_empty"
    )
    val levels = listOf(maxLevel, fallbackHalf(), maxLevel / 3, 0)

    val fontSize = if (buttonHeight >= 64.dp) 18.sp else if (buttonHeight >= 46.dp) 16.sp else 14.sp

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val colWidth = maxWidth
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            options.forEachIndexed { index, label ->
                MotionButton(
                    text = label,
                    icon = icons[index],
                    onClick = { applyLevel(levels[index]) },
                    width = colWidth,
                    height = buttonHeight,
                    fontSize = fontSize,
                    defaultState = defaultState,
                    defaultPressedState = defaultPressedState,
                    motionLevel = motionLevel
                )
            }
        }
    }
}

/** The connected 0 / 33 / 50 / 100 % row. Tapping one jumps the torch and flashes the button. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresetButtonRow(
    maxLevel: Int,
    applyLevel: (Int) -> Unit,
    fallbackHalf: () -> Int,
    modifier: Modifier = Modifier,
    height: Dp = 54.dp,
    showText: Boolean = true,
    isScreenActive: Boolean = false
) {
    val options = listOf("0", "33", "50", "100")
    val levels = listOf(0, maxLevel / 3, fallbackHalf(), maxLevel)

    var selectedIndex by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()

    val toggleColors = if (isScreenActive) {
        ToggleButtonDefaults.toggleButtonColors(
            containerColor = Color(0xFFF4F5F8),
            contentColor = Color(0xFF1E2022),
            checkedContainerColor = Color(0xFFD8DCE2),
            checkedContentColor = Color(0xFF1E2022)
        )
    } else {
        ToggleButtonDefaults.toggleButtonColors()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    selectedIndex = index
                    applyLevel(levels[index])
                    scope.launch {
                        delay(300)
                        selectedIndex = -1
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                colors = toggleColors,
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Icon(
                    painter = painterResource(
                        when (index) {
                            0 -> R.drawable.ic_brightness_0
                            1 -> R.drawable.ic_brightness_1
                            2 -> R.drawable.ic_brightness_2
                            else -> R.drawable.ic_brightness_3
                        }
                    ),
                    contentDescription = "brightness level",
                    modifier = Modifier.size(24.dp),
                    tint = if (isScreenActive) Color(0xFF1E2022) else androidx.compose.material3.LocalContentColor.current
                )
                if (showText) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = if (isScreenActive) Color(0xFF1E2022) else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun rememberAmbientLuminance(context: Context): Float {
    var luminance by remember { mutableFloatStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    DisposableEffect(lightSensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                luminance = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return luminance
}
