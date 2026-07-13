package com.neomods.tools.theme

/**
 * User-selectable theme mode, persisted via [com.neomods.tools.storage.SettingsManager].
 */
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            else -> SYSTEM
        }
    }
}