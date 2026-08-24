package com.feldman.flashlight.storage

enum class OrientationMode(val key: String) {
    AUTO("auto"),
    PORTRAIT("portrait"),
    LANDSCAPE("landscape");

    companion object {
        fun fromKey(key: String?): OrientationMode =
            entries.find { it.key == key } ?: AUTO
    }
}