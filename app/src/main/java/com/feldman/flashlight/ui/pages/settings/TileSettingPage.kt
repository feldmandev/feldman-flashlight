package com.feldman.flashlight.ui.pages.settings

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.storage.openFullPagesFlow
import com.feldman.flashlight.storage.setOpenFullPages
import com.feldman.flashlight.storage.setShowTileInfo
import com.feldman.flashlight.storage.showTileInfoFlow
import com.feldman.flashlight.R
import com.feldman.flashlight.ui.tiles.FlashlightTileService
import com.feldman.flashlight.ui.components.SettingsCategoryColor
import com.feldman.flashlight.ui.components.SettingsTopBar
import com.feldman.motion.MotionScaffold
import com.feldman.motion.isDarkTheme
import kotlinx.coroutines.launch

/** What the quick settings tile shows, and where tapping it lands. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TileSettingsPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tileRequestMessage by remember { mutableStateOf<String?>(null) }

    val showTileInfo by context.showTileInfoFlow().collectAsState(initial = true)
    val openFullPages by context.openFullPagesFlow().collectAsState(initial = true)

    MotionScaffold(
        scaffoldModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        topBar = {
            SettingsTopBar(
                title = "Tile",
                onBack = onBack,
                chromeColor = SettingsCategoryColor.TILE.container(isDarkTheme())
            )
        }
    ) {
        title("Add to Quick Settings")
        section {
            item(padding = 16.dp) {
                Button(
                    onClick = {
                        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                        statusBarManager.requestAddTileService(
                            ComponentName(context, FlashlightTileService::class.java),
                            "Flashlight",
                            Icon.createWithResource(context, R.drawable.ic_flashlight),
                            context.mainExecutor
                        ) { result ->
                            tileRequestMessage = when (result) {
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Flashlight was added to Quick Settings"
                                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "Flashlight is already in Quick Settings"
                                else -> "The tile was not added"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Add Flashlight tile")
                }
            }
            tileRequestMessage?.let { message ->
                item(padding = 16.dp) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        title("Tile")
        section {
            switchItem(
                title = "Show expanded info",
                description = "Include the current brightness on the tile",
                checked = showTileInfo,
                onCheckedChange = { scope.launch { context.setShowTileInfo(it) } }
            )
            switchItem(
                title = "Open full pages",
                description = "Long-pressing the tile opens the app instead of a compact panel",
                checked = openFullPages,
                onCheckedChange = { scope.launch { context.setOpenFullPages(it) } }
            )
        }

        item {
            Spacer(Modifier.height(120.dp))
        }
    }
}
