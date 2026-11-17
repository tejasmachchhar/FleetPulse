package com.fleetpulse.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.fleetDataStore: DataStore<Preferences> by preferencesDataStore(name = "fleetpulse_prefs")

data class AppPreferencesState(
    val themeMode: String = "system",
    val onboardingComplete: Boolean = false,
    val notificationsEnabled: Boolean = true,
    /** False until DataStore has emitted its first value. */
    val loaded: Boolean = false,
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val onboarding = booleanPreferencesKey("onboarding_complete")
        val notifications = booleanPreferencesKey("notifications_enabled")
    }

    val state: Flow<AppPreferencesState> = context.fleetDataStore.data.map { p ->
        AppPreferencesState(
            themeMode = p[Keys.theme] ?: "system",
            onboardingComplete = p[Keys.onboarding] ?: false,
            notificationsEnabled = p[Keys.notifications] ?: true,
            loaded = true,
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.fleetDataStore.edit { it[Keys.theme] = mode }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.fleetDataStore.edit { it[Keys.onboarding] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.fleetDataStore.edit { it[Keys.notifications] = value }
    }
}
