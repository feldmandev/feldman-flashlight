package com.feldman.flashlight.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Fixed icon-chip colours for the settings hub, one hue per category.
 *
 * Deliberately literal rather than theme roles. The scheme only offers three container roles, so
 * categories drawn from it repeat and stop being recognisable; hand-picked tonal pairs give each
 * row its own identity and stay put when the seed colour changes, which is what makes the list
 * scannable by colour alone. Values are Material tonal-palette steps — container around tone 30 in
 * dark and tone 90 in light, with content at the opposite end.
 */
enum class SettingsCategoryColor(
    private val darkContainer: Long,
    private val darkContent: Long,
    private val lightContainer: Long,
    private val lightContent: Long
) {
    /** Pink, matching where the theme controls live. */
    APPEARANCE(0xFF7D5260, 0xFFFFD8E4, 0xFFFFD8E4, 0xFF631835),
    /** Amber, for the torch itself. */
    FLASHLIGHT(0xFF6C4E00, 0xFFFFDF9E, 0xFFFFDF9E, 0xFF412D00),
    /** Green, for the surface that lives outside the app. */
    TILE(0xFF324F34, 0xFFCBEFD0, 0xFFCBEFD0, 0xFF042106);

    fun container(isDark: Boolean): Color = Color(if (isDark) darkContainer else lightContainer)
    fun content(isDark: Boolean): Color = Color(if (isDark) darkContent else lightContent)
}
