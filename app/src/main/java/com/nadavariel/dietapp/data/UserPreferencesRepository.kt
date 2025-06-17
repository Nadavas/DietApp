package com.nadavariel.dietapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// At the top level of your file (outside any class)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        // REMOVED: USER_NAME and USER_WEIGHT as they are now in firestore
        // val USER_NAME = stringPreferencesKey("user_name")
        // val USER_WEIGHT = stringPreferencesKey("user_weight")
    }

    val userEmailFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] ?: ""
        }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] ?: false
        }

    suspend fun saveUserPreferences(email: String, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_EMAIL] = email
            preferences[PreferencesKeys.REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearUserPreferences() {
        context.dataStore.edit { preferences ->
            // This will clear everything in DataStore, which is fine since it only holds email/rememberMe now
            preferences.clear()
        }
    }

    // REMOVED: saveProfileData and loadProfileData methods as they are now in AuthViewModel via firestore
    // No longer needed for DataStore in this repo.
}