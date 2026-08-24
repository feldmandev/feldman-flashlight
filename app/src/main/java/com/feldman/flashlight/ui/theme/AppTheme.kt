package com.feldman.flashlight.ui.theme

import androidx.compose.runtime.Composable
import com.feldman.motion.MotionTheme

/**
 * The app's theme is motion's theme.
 *
 * Kept as a named entry point so the tile and the standalone page activities read the same way as
 * the main activity, but there is nothing app-specific left in here — colour, typography, shapes and
 * the light/dark choice all come from [MotionTheme] and its repository.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MotionTheme(content = content)
}

/** The resolved light/dark state, from motion's stored theme mode. */
@Composable
fun isDarkTheme(): Boolean = com.feldman.motion.isDarkTheme()
