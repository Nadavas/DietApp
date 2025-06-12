package com.nadavariel.dietapp.data // MAKE SURE THIS PACKAGE IS CORRECT

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map // This import is crucial for .map to work

// At the top level of your file (outside any class)
// This is the single instance of DataStore for the entire application.
// By delegating to preferencesDataStore, you ensure a singleton instance
// and prevent multiple instances from managing the same file.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val USER_EMAIL = stringPreferencesKey("user_email")
        // We will add a key for the password here, but will store it securely via EncryptedSharedPreferences later.
        // For now, we'll focus on the email and the "remember me" flag.
    }

    // Flow to observe the "remember me" state
    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] ?: false // Default to false if not set
        }

    // Flow to observe the stored user email
    val userEmailFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] ?: "" // Default to empty string
        }

    // Function to save preferences
    suspend fun saveUserPreferences(email: String, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.REMEMBER_ME] = rememberMe
        }
    }

    // Function to clear preferences (e.g., on sign out)
    suspend fun clearUserPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}