package com.feldman.flashlight.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "usage_prefs")

object PrefKeys {
    val ORIENTATION_MODE = stringPreferencesKey("orientation_mode")
    val INSTANT_FLASHLIGHT = booleanPreferencesKey("instant_flashlight")
    val DEFAULT_FLASHLIGHT_LEVEL = intPreferencesKey("default_flashlight_level")
    val AUTO_FLASHLIGHT_OFF = booleanPreferencesKey("auto_flashlight_off")
    val VOLUME_BUTTONS_FLASHLIGHT = booleanPreferencesKey("volume_buttons_flashlight")
    val DEFAULT_LIGHT_SOURCE_MODE = stringPreferencesKey("default_light_source_mode")
    val SHOW_TILE_INFO = booleanPreferencesKey("show_tile_info")
    val OPEN_FULL_PAGES = booleanPreferencesKey("open_full_pages")
    val AUTO_OFF_TIMER_MINUTES = intPreferencesKey("auto_off_timer_minutes")
    val SCREEN_LIGHT_COLOR_ARGB = intPreferencesKey("screen_light_color_argb")
}

fun Context.orientationModeFlow(): Flow<OrientationMode> =
    dataStore.data.map { prefs -> OrientationMode.fromKey(prefs[PrefKeys.ORIENTATION_MODE]) }

suspend fun Context.setOrientationMode(mode: OrientationMode) {
    dataStore.edit { it[PrefKeys.ORIENTATION_MODE] = mode.key }
}

fun Context.instantFlashlightFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.INSTANT_FLASHLIGHT] ?: false }

suspend fun Context.setInstantFlashlight(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.INSTANT_FLASHLIGHT] = enabled }
}

fun Context.defaultFlashlightLevelFlow(): Flow<Int> =
    dataStore.data.map { prefs -> prefs[PrefKeys.DEFAULT_FLASHLIGHT_LEVEL] ?: 50 }

suspend fun Context.setDefaultFlashlightLevel(percent: Int) {
    dataStore.edit { it[PrefKeys.DEFAULT_FLASHLIGHT_LEVEL] = percent.coerceIn(0, 100) }
}

fun Context.autoFlashlightOffFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.AUTO_FLASHLIGHT_OFF] ?: true }

suspend fun Context.setAutoFlashlightOff(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.AUTO_FLASHLIGHT_OFF] = enabled }
}

fun Context.volumeButtonsFlashlightFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.VOLUME_BUTTONS_FLASHLIGHT] ?: true }

suspend fun Context.setVolumeButtonsFlashlight(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.VOLUME_BUTTONS_FLASHLIGHT] = enabled }
}

enum class LightSourceMode(val key: String, val label: String) {
    FLASH("flash", "Flash"),
    SCREEN("screen", "Screen"),
    BOTH("both", "Both");

    companion object {
        fun fromKey(key: String?): LightSourceMode =
            entries.find { it.key == key } ?: FLASH
    }
}

fun Context.defaultLightSourceModeFlow(): Flow<LightSourceMode> =
    dataStore.data.map { prefs -> LightSourceMode.fromKey(prefs[PrefKeys.DEFAULT_LIGHT_SOURCE_MODE]) }

suspend fun Context.setDefaultLightSourceMode(mode: LightSourceMode) {
    dataStore.edit { it[PrefKeys.DEFAULT_LIGHT_SOURCE_MODE] = mode.key }
}

fun Context.showTileInfoFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.SHOW_TILE_INFO] ?: false }

suspend fun Context.setShowTileInfo(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.SHOW_TILE_INFO] = enabled }
}

fun Context.openFullPagesFlow(): Flow<Boolean> =
    dataStore.data.map { prefs -> prefs[PrefKeys.OPEN_FULL_PAGES] ?: false }

suspend fun Context.setOpenFullPages(enabled: Boolean) {
    dataStore.edit { it[PrefKeys.OPEN_FULL_PAGES] = enabled }
}

fun Context.autoOffTimerMinutesFlow(): Flow<Int> =
    dataStore.data.map { prefs -> prefs[PrefKeys.AUTO_OFF_TIMER_MINUTES] ?: 0 }

suspend fun Context.setAutoOffTimerMinutes(minutes: Int) {
    dataStore.edit { it[PrefKeys.AUTO_OFF_TIMER_MINUTES] = minutes.coerceAtLeast(0) }
}

fun Context.screenLightColorArgbFlow(): Flow<Int> =
    dataStore.data.map { prefs -> prefs[PrefKeys.SCREEN_LIGHT_COLOR_ARGB] ?: 0xFFFFFFFF.toInt() }

suspend fun Context.setScreenLightColorArgb(argb: Int) {
    dataStore.edit { it[PrefKeys.SCREEN_LIGHT_COLOR_ARGB] = argb }
}
