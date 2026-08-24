package com.feldman.flashlight.ui.tiles

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.feldman.flashlight.MainActivity
import com.feldman.flashlight.storage.PrefKeys
import com.feldman.flashlight.storage.dataStore
import com.feldman.flashlight.ui.pages.FlashlightPageActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class QsPrefsRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Read the setting
            val openFullPages = dataStore.data
                .map { prefs -> prefs[PrefKeys.OPEN_FULL_PAGES] ?: false }
                .first()  // take first emitted value

            val cn = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)

            val target = when (cn?.className) {
                FlashlightTileService::class.java.name -> FlashlightTile::class.java
                else -> null
            }

            if (target != null) {
                val destination = if (openFullPages) {
                    // Launch full page instead of tile
                    when (target) {
                        FlashlightTile::class.java -> FlashlightPageActivity::class.java
                        else -> target
                    }
                } else target

                startActivity(Intent(this@QsPrefsRouterActivity, destination).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                startActivity(Intent(this@QsPrefsRouterActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }

            finish()
        }
    }

}