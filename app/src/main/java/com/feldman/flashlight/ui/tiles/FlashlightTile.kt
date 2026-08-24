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
    data class TorchStatus(
        val hasFlash: Boolean = true,
        val available: Boolean = true,
        val message: String? = null
    )

    private val _torchLevel = MutableStateFlow(0)
    val torchLevel: StateFlow<Int> = _torchLevel.asStateFlow()
    private val _status = MutableStateFlow(TorchStatus())
    val status: StateFlow<TorchStatus> = _status.asStateFlow()

    fun startListening(context: Context): () -> Unit {
        val camMgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                _status.value = TorchStatus()
                if (!enabled) {
                    _torchLevel.value = 0
                } else if (_torchLevel.value == 0) {
                    _torchLevel.value = 1
                }
            }

            override fun onTorchStrengthLevelChanged(cameraId: String, level: Int) {
                _status.value = TorchStatus()
                _torchLevel.value = level
            }

            override fun onTorchModeUnavailable(cameraId: String) {
                _status.value = TorchStatus(
                    available = false,
                    message = "Flash is temporarily unavailable. Close any camera app and try again."
                )
            }
        }
        if (cameraId(context) == null) {
            _status.value = TorchStatus(
                hasFlash = false,
                available = false,
                message = "No rear flash was found. Screen light is still available."
            )
            return {}
        }
        return try {
            camMgr.registerTorchCallback(callback, null)
            val unregister: () -> Unit = {
                try {
                    camMgr.unregisterTorchCallback(callback)
                } catch (_: Exception) {
                    // The camera service may already be gone during teardown.
                }
            }
            unregister
        } catch (_: Exception) {
            _status.value = TorchStatus(
                available = false,
                message = "Flash is temporarily unavailable. Close any camera app and try again."
            )
            val noOp: () -> Unit = {}
            noOp
        }
    }

    private fun cameraId(context: Context): String? {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    fun getMaxLevel(context: Context): Int {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: return 1
        return runCatching {
            cm.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        }.getOrDefault(1)
    }

    /** level: 0 = off. On API < 33, any level > 0 turns torch on (no strength control). */
    fun setIntensity(context: Context, level: Int): Boolean {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraId(context) ?: run {
            _status.value = TorchStatus(
                hasFlash = false,
                available = false,
                message = "No rear flash was found. Screen light is still available."
            )
            return false
        }
        return try {
            if (level <= 0) cm.setTorchMode(id, false)
            else cm.turnOnTorchWithStrengthLevel(id, level)
            _torchLevel.value = level
            _status.value = TorchStatus()
            true
        } catch (_: Exception) {
            _status.value = TorchStatus(
                available = false,
                message = "Flash is temporarily unavailable. Close any camera app and try again."
            )
            false
        }
    }

    fun handleVolumeKey(context: Context, keyCode: Int): Boolean {
        if (!_status.value.hasFlash || !_status.value.available) return false
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
                updateTileUnavailable()
            }
        }

        val id = runCatching {
            cameraManager?.cameraIdList?.firstOrNull {
                cameraManager?.getCameraCharacteristics(it)
                    ?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        if (id == null) {
            updateTileUnavailable()
            return
        }

        runCatching { cameraManager?.registerTorchCallback(torchCallback!!, null) }
            .onFailure {
                updateTileUnavailable()
                return
            }

        val torchOn = id.let {
            // Query system torch mode via callback (API >= 33)
            try {
                (cameraManager?.getTorchStrengthLevel(it) ?: 0) > 0
            } catch (_: Exception) {
                false
            }
        }

        updateTileState(torchOn)
    }

    override fun onClick() {
        super.onClick()
        val cm = cameraManager ?: getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = runCatching {
            cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: run {
            updateTileUnavailable()
            return
        }

        // Toggle actual torch state
        val newState = !currentTorchOn
        runCatching { cm.setTorchMode(id, newState) }
            .onSuccess {
                currentTorchOn = newState
                updateTileState(newState)
            }
            .onFailure { updateTileUnavailable() }
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

    private fun updateTileUnavailable() {
        qsTile?.apply {
            if (icon == null) {
                icon = Icon.createWithResource(this@FlashlightTileService, R.drawable.ic_flashlight)
            }
            label = "Flashlight"
            state = Tile.STATE_UNAVAILABLE
            updateTile()
        }
    }
}
