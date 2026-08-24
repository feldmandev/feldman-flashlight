package com.feldman.flashlight.ui.pages

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.feldman.flashlight.ui.components.SettingsCategoryColor
import com.feldman.flashlight.ui.components.SettingsTopBar
import com.feldman.flashlight.ui.navigation.AppDest
import com.feldman.motion.MotionScaffold
import com.feldman.motion.isDarkTheme
import com.feldman.motion.symbolPainter

/**
 * Hub for everything configurable. Each row is a destination rather than an inline control, so the
 * page stays a readable map of the app instead of one long scroll mixing switches and pickers.
 * Each category keeps its own icon colour so the list is scannable without reading it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsPage(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenFlashlight: () -> Unit,
    onOpenTile: () -> Unit
) {
    val isDark = isDarkTheme()
    MotionScaffold(
        scaffoldModifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        topBar = { SettingsTopBar("Settings", onBack) }
    ) {
        title("App")
        section {
            pageItem(
                title = "Appearance",
                description = "Theme, colors, motion, and orientation",
                icon = symbolPainter("palette"),
                backgroundColor = SettingsCategoryColor.APPEARANCE.container(isDark),
                iconColor = SettingsCategoryColor.APPEARANCE.content(isDark),
                onClick = onOpenAppearance,
                paneDestination = AppDest.Appearance
            )
        }

        title("Tools")
        section {
            pageItem(
                title = "Flashlight",
                description = "Instant torch, default brightness, and auto off",
                icon = symbolPainter("flashlight_on"),
                backgroundColor = SettingsCategoryColor.FLASHLIGHT.container(isDark),
                iconColor = SettingsCategoryColor.FLASHLIGHT.content(isDark),
                onClick = onOpenFlashlight,
                paneDestination = AppDest.FlashlightSettings,
                iconMorphShape = MaterialShapes.Arch
            )
            pageItem(
                title = "Tile",
                description = "What the quick settings tile shows",
                icon = symbolPainter("dashboard"),
                backgroundColor = SettingsCategoryColor.TILE.container(isDark),
                iconColor = SettingsCategoryColor.TILE.content(isDark),
                onClick = onOpenTile,
                paneDestination = AppDest.TileSettings,
                iconMorphShape = MaterialShapes.Square
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}
