package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "nasrullah_challenge_prefs")

class ThemePreferences(private val context: Context) {
  private val themeKey = stringPreferencesKey("theme_mode")

  val themeModeFlow: Flow<ThemeMode> =
    context.themeDataStore.data.map { prefs ->
      ThemeMode.fromWire(prefs[themeKey])
    }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.themeDataStore.edit { prefs ->
      prefs[themeKey] = mode.wireValue
    }
  }
}
