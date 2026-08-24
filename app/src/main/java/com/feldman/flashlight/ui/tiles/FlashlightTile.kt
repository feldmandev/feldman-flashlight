package com.feldman.flashlight.ui.tiles

import android.content.Context
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.storage.showTileInfoFlow
import com.feldman.flashlight.ui.components.SensorCard
import com.feldman.flashlight.ui.components.TorchSlider
import com.feldman.flashlight.ui.pages.PresetButtonRow
import com.feldman.flashlight.ui.theme.AppTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.feldman.flashlight.R

class FlashlightTile : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        setFinishOnTouchOutside(true)
        setContent {

            AppTheme {
                FlashlightTile(
                    onDismiss = { finish() }
                )
            }
        }
    }
}
object FlashlightController {
    private val _torchLevel = MutableStateFlow(0)
    val torchLevel: StateFlow<Int> = _torchLevel.asStateFlow()

    fun startListening(context: Context) {
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        camMgr.registerTorchCallback(object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (!enabled) {
                    _torchLevel.value = 0
                }
            }

            override fun onTorchStrengthLevelChanged(cameraId: String, level: Int) {
                _torchLevel.value = level
            }
        }, null)
    }

    private fun cameraId(context: Context): String? {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }

    fun getMaxLevel(context: Context): Int {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: return 1
        return cm.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
    }

    /** level: 0 = off. On API < 33, any level > 0 turns torch on (no strength control). */
    fun setIntensity(context: Context, level: Int) {
        _torchLevel.value = level

        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: return
        try {
            if (level <= 0) cm.setTorchMode(id, false)
            else cm.turnOnTorchWithStrengthLevel(id, level)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleVolumeKey(context: Context, keyCode: Int): Boolean {
        val maxLevel = getMaxLevel(context)
        val currentLevel = _torchLevel.value
        val step = maxOf(1, (maxLevel * 0.1f).toInt())

        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                val newLevel = if (currentLevel <= 0) {
                    1
                } else {
                    (currentLevel + step).coerceIn(1, maxLevel)
                }
                setIntensity(context, newLevel)
                true
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (currentLevel > 0) {
                    val newLevel = (currentLevel - step).coerceAtLeast(0)
                    setIntensity(context, newLevel)
                }
                true
            }
            else -> false
        }
    }
}


@OptIn(FlowPreview::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlashlightTile(onDismiss: () -> Unit) {
    val context = LocalContext.current

    val maxLevel = remember { FlashlightController.getMaxLevel(context) }
    val level by FlashlightController.torchLevel.collectAsState()

    fun applyLevel(newLevel: Int) {
        val clamped = newLevel.coerceIn(0, maxLevel)
        FlashlightController.setIntensity(context, clamped)
    }

    fun fallbackHalf(): Int =
        if (maxLevel > 1) maxOf(1, (maxLevel * 0.5f).toInt()) else 1

    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    val percent = if (maxLevel <= 1) if (level > 0) 100 else 0
    else ((level.toFloat() / maxLevel) * 100).toInt()
    val showTileInfo by context.showTileInfoFlow().collectAsState(initial = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Flashlight",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                TorchSlider(
                    level = level,
                    maxLevel = maxLevel,
                    applyLevel = { applyLevel(it) }
                )
                if (showTileInfo){
                    Row(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            12.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SensorCard(
                            icon = R.drawable.ic_brightness,
                            label = "Brightness",
                            value = "$percent%",
                            cornerRadius = 28.dp,
                            color = secondaryContainer,
                            index = 1,
                            iconShapeIndex = 1
                        )
                        SensorCard(
                            icon = R.drawable.ic_layers,
                            label = "Level",
                            value = "$level/$maxLevel",
                            cornerRadius = 28.dp,
                            color = secondaryContainer,
                            index = 2,
                            iconShapeIndex = 2
                        )
                    }

                    PresetButtonRow(
                        maxLevel = maxLevel,
                        applyLevel = ::applyLevel,
                        fallbackHalf = ::fallbackHalf,
                        showText = false
                    )
                }

            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
            ) {
                Text("Done")
            }
        }
    )
}

class FlashlightTileService : TileService() {

    private var cameraManager: CameraManager? = null
    private var torchCallback: CameraManager.TorchCallback? = null
    private var currentTorchOn = false

    override fun onStartListening() {
        super.onStartListening()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Register callback to receive actual torch state changes
        torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                currentTorchOn = enabled
                updateTileState(enabled)
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                updateTileState(false)
            }
        }

        cameraManager?.registerTorchCallback(torchCallback!!, null)

        // Initialize tile state from current torch mode (if available)
        val id = cameraManager?.cameraIdList?.firstOrNull {
            cameraManager?.getCameraCharacteristics(it)
                ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
        val torchOn = id?.let {
            // Query system torch mode via callback (API >= 33)
            try {
                (cameraManager?.getTorchStrengthLevel(it) ?: 0) > 0
            } catch (_: Exception) {
                false
            }
        } ?: false

        updateTileState(torchOn)
    }

    override fun onClick() {
        super.onClick()
        val cm = cameraManager ?: getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        // Toggle actual torch state
        val newState = !currentTorchOn
        cm.setTorchMode(id, newState)
        currentTorchOn = newState
        updateTileState(newState)
    }

    override fun onStopListening() {
        super.onStopListening()
        torchCallback?.let { cameraManager?.unregisterTorchCallback(it) }
        torchCallback = null
        cameraManager = null
    }

    private fun updateTileState(isOn: Boolean) {
        qsTile?.apply {
            if (icon == null) {
                icon = Icon.createWithResource(this@FlashlightTileService, R.drawable.ic_flashlight)
            }
            label = "Flashlight"
            state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            updateTile()
        }
    }
}
