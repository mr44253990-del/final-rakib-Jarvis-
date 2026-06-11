package com.example.config

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[API_KEY]
    }

    val modelNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL_NAME] ?: "gemini-3.1-flash-lite-preview"
    }

    suspend fun saveSettings(apiKey: String, modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
            preferences[MODEL_NAME] = modelName
        }
    }
}
