@file:Suppress("PROPERTY_WONT_BE_SERIALIZED")

package com.feldman.flashlight.ui.navigation

import androidx.compose.runtime.Composable
import com.feldman.flashlight.R
import com.feldman.flashlight.ui.pages.FlashlightPage
import com.feldman.flashlight.ui.pages.SettingsPage
import com.feldman.flashlight.ui.pages.settings.AppearanceSettingsPage
import com.feldman.flashlight.ui.pages.settings.FlashlightSettingsPage
import com.feldman.flashlight.ui.pages.settings.TileSettingsPage
import com.feldman.motion.MotionDest
import com.feldman.motion.MotionNavigator
import com.feldman.motion.MotionPaneType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Every screen in the app, as a motion [MotionDest].
 *
 * A destination carries its own label, icons and content, so adding a screen is one entry here plus
 * one line in [appDestinations] — nothing else in the app needs to know about it.
 *
 * There is no bottom bar: the flashlight is the app, and settings hang off its top bar. So every
 * destination past the flashlight is a push onto the stack, and motion animates them all as depth.
 */
@Serializable
@Parcelize
sealed class AppDest : MotionDest {

    @Serializable
    @Parcelize
    data object Flashlight : AppDest() {
        @IgnoredOnParcel
        @Transient
        override val label = "Flashlight"

        @IgnoredOnParcel
        @Transient
        override val filledIcon = R.drawable.ic_flashlight

        @IgnoredOnParcel
        @Transient
        override val outlineIcon = R.drawable.ic_flashlight_outline

        @Composable
        override fun Content(
            onNavigate: MotionNavigator,
            onBack: () -> Unit,
            searchQuery: String,
            onFabAction: ((() -> Unit) -> Unit) -> Unit
        ) {
            FlashlightPage(onOpenSettings = { onNavigate(Settings) })
        }
    }

    @Serializable
    @Parcelize
    data object Settings : AppDest() {
        @IgnoredOnParcel
        @Transient
        override val label = "Settings"

        @IgnoredOnParcel
        @Transient
        override val filledIcon = R.drawable.ic_settings

        @IgnoredOnParcel
        @Transient
        override val outlineIcon = R.drawable.ic_settings_outline

        @IgnoredOnParcel
        @Transient
        override val parent = Flashlight

        @IgnoredOnParcel
        @Transient
        override val pane = MotionPaneType.LIST

        @Composable
        override fun Content(
            onNavigate: MotionNavigator,
            onBack: () -> Unit,
            searchQuery: String,
            onFabAction: ((() -> Unit) -> Unit) -> Unit
        ) {
            SettingsPage(
                onBack = onBack,
                onOpenAppearance = { onNavigate(Appearance) },
                onOpenFlashlight = { onNavigate(FlashlightSettings) },
                onOpenTile = { onNavigate(TileSettings) }
            )
        }
    }

    @Serializable
    @Parcelize
    data object Appearance : AppDest() {
        @IgnoredOnParcel
        @Transient
        override val label = "Appearance"

        @IgnoredOnParcel
        @Transient
        override val filledIcon = 0

        @IgnoredOnParcel
        @Transient
        override val outlineIcon = 0

        @IgnoredOnParcel
        @Transient
        override val parent = Settings

        @IgnoredOnParcel
        @Transient
        override val pane = MotionPaneType.DETAIL

        // Sub-pages own the whole screen: the bar would only compete with their own top bar and
        // back arrow, which is already the way back out.
        @IgnoredOnParcel
        @Transient
        override val showNavigation = false

        @Composable
        override fun Content(
            onNavigate: MotionNavigator,
            onBack: () -> Unit,
            searchQuery: String,
            onFabAction: ((() -> Unit) -> Unit) -> Unit
        ) {
            AppearanceSettingsPage(onBack = onBack)
        }
    }

    @Serializable
    @Parcelize
    data object FlashlightSettings : AppDest() {
        @IgnoredOnParcel
        @Transient
        override val label = "Flashlight"

        @IgnoredOnParcel
        @Transient
        override val filledIcon = 0

        @IgnoredOnParcel
        @Transient
        override val outlineIcon = 0

        @IgnoredOnParcel
        @Transient
        override val parent = Settings

        @IgnoredOnParcel
        @Transient
        override val pane = MotionPaneType.DETAIL

        @IgnoredOnParcel
        @Transient
        override val showNavigation = false

        @Composable
        override fun Content(
            onNavigate: MotionNavigator,
            onBack: () -> Unit,
            searchQuery: String,
            onFabAction: ((() -> Unit) -> Unit) -> Unit
        ) {
            FlashlightSettingsPage(onBack = onBack)
        }
    }

    @Serializable
    @Parcelize
    data object TileSettings : AppDest() {
        @IgnoredOnParcel
        @Transient
        override val label = "Tile"

        @IgnoredOnParcel
        @Transient
        override val filledIcon = 0

        @IgnoredOnParcel
        @Transient
        override val outlineIcon = 0

        @IgnoredOnParcel
        @Transient
        override val parent = Settings

        @IgnoredOnParcel
        @Transient
        override val pane = MotionPaneType.DETAIL

        @IgnoredOnParcel
        @Transient
        override val showNavigation = false

        @Composable
        override fun Content(
            onNavigate: MotionNavigator,
            onBack: () -> Unit,
            searchQuery: String,
            onFabAction: ((() -> Unit) -> Unit) -> Unit
        ) {
            TileSettingsPage(onBack = onBack)
        }
    }
}

/** Registration list for the nav host — every destination class the app can display. */
val appDestinations: List<AppDest> = listOf(
    AppDest.Flashlight,
    AppDest.Settings,
    AppDest.Appearance,
    AppDest.FlashlightSettings,
    AppDest.TileSettings
)
