package com.feldman.flashlight

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.lifecycleScope
import com.feldman.flashlight.storage.OrientationMode
import com.feldman.flashlight.storage.PrefKeys
import com.feldman.flashlight.storage.dataStore
import com.feldman.flashlight.ui.navigation.AppDest
import com.feldman.flashlight.ui.navigation.appDestinations
import com.feldman.motion.MotionNavFlow
import android.view.KeyEvent
import com.feldman.flashlight.storage.volumeButtonsFlashlightFlow
import com.feldman.flashlight.ui.tiles.FlashlightController
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var volumeControlsEnabled: Boolean = true

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            volumeButtonsFlashlightFlow().collect { enabled ->
                volumeControlsEnabled = enabled
            }
        }

        lifecycleScope.launch {
            dataStore.data.map { prefs ->
                OrientationMode.fromKey(prefs[PrefKeys.ORIENTATION_MODE])
            }.collect { mode ->

                requestedOrientation = when (mode) {
                    OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
