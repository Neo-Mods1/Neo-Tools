package com.neomods.tools.storage

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neomods.tools.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Persists app preferences (theme mode + dynamic colors).
 *
 * Language and output-path settings are intentionally omitted: Neo Tools has a
 * single locale and works in-memory on the picked content, so neither applies.
 */
class SettingsManager(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val UI_SCALE = floatPreferencesKey("ui_scale")
    }

    private val scope = CoroutineScope(Dispatchers.Main)

    val themeMode: StateFlow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromValue(preferences[THEME_MODE] ?: "system")
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    val dynamicColors: StateFlow<Boolean> = dataStore.data.map { preferences ->
        preferences[DYNAMIC_COLORS] ?: true
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val uiScale: StateFlow<Float> = dataStore.data.map { preferences ->
        preferences[UI_SCALE] ?: 1.0f
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.value
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }

    suspend fun setUiScale(scale: Float) {
        dataStore.edit { preferences ->
            preferences[UI_SCALE] = scale.coerceIn(0.75f, 1.5f)
        }
    }

    fun applyUiScale(context: Context, scale: Float) {
        val config = Configuration(context.resources.configuration)
        val baseDensity = context.resources.displayMetrics.densityDpi
        config.fontScale = scale
        @Suppress("DEPRECATION")
        config.densityDpi = (baseDensity * scale).toInt()
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}