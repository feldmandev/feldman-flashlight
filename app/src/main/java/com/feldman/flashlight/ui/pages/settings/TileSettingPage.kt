package com.feldman.flashlight.ui.pages.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.storage.openFullPagesFlow
import com.feldman.flashlight.storage.setOpenFullPages
import com.feldman.flashlight.storage.setShowTileInfo
import com.feldman.flashlight.storage.showTileInfoFlow
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
