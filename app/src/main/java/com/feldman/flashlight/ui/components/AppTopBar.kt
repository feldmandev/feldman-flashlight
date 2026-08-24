package com.feldman.flashlight.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.feldman.motion.feldmanFont
import com.feldman.motion.rememberExpressiveDesign
import com.feldman.motion.symbolPainter

/**
 * The back arrow every settings sub-page shares. Written once here so a new sub-page cannot
 * accidentally drift from the rest.
 *
 * [chromeColor] fills the bar with the category's own container colour — the same colour its icon
 * chip carries in the settings hub — so the page you land on is recognisable from the bar alone
 * before you read the title. The hub itself has no category, and keeps the neutral surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    chromeColor: Color = colorScheme.surfaceContainer
) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                FilledIconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colorScheme.surface,
                        contentColor = colorScheme.onSurface
                    )
                ) {
                    Icon(symbolPainter("arrow_back"), "Back")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = chromeColor)
    )
}

/**
 * Title for a top-level page's app bar: the Feldman wordmark in the accent colour, sized to carry
 * a bar of its own rather than sit inside a settings list.
 */
@Composable
fun AppPageTitle(text: String, color: Color? = null) {
    val expressiveDesign = rememberExpressiveDesign()

    if (expressiveDesign) {
        Text(
            text = text,
            color = color ?: colorScheme.primary,
            fontFamily = feldmanFont(width = 120f, weight = 900),
            fontSize = 26.sp
        )
    } else {
        Text(
            text = text,
            color = color ?: colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Bar for a top-level page. Transparent on purpose — the flashlight page draws edge to edge
 * underneath it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    containerColor: Color = Color.Transparent,
    titleColor: Color? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = { AppPageTitle(title, color = titleColor) },
        actions = actions,
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleColor ?: colorScheme.primary
        )
    )
}

/** The gear that opens settings from a top-level page. */
@Composable
fun SettingsAction(
    onClick: () -> Unit,
    tint: Color? = null
) {
    IconButton(onClick = onClick) {
        Icon(
            symbolPainter("settings", fill = 1f),
            contentDescription = "Settings",
            tint = tint ?: androidx.compose.material3.LocalContentColor.current
        )
    }
}
