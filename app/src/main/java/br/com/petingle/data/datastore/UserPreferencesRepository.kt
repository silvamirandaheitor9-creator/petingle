package br.com.petingle.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_DARK_THEME      = booleanPreferencesKey("dark_theme")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_TERMS_ACCEPTED  = booleanPreferencesKey("terms_accepted")
        private val KEY_USER_NAME       = stringPreferencesKey("user_name")
        private val KEY_PROFILE_PHOTO   = stringPreferencesKey("profile_photo_path")
        private val KEY_BONUS_PET_SLOTS = intPreferencesKey("bonus_pet_slots")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_DARK_THEME] ?: false }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    val isTermsAccepted: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_TERMS_ACCEPTED] ?: false }

    val userName: Flow<String> = context.dataStore.data
        .map { it[KEY_USER_NAME] ?: "" }

    val profilePhotoPath: Flow<String> = context.dataStore.data
        .map { it[KEY_PROFILE_PHOTO] ?: "" }

    val bonusPetSlots: Flow<Int> = context.dataStore.data
        .map { it[KEY_BONUS_PET_SLOTS] ?: 0 }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        context.dataStore.edit { it[KEY_TERMS_ACCEPTED] = accepted }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setProfilePhotoPath(path: String) {
        context.dataStore.edit { it[KEY_PROFILE_PHOTO] = path }
    }

    suspend fun addBonusPetSlots(slots: Int) {
        require(slots > 0)
        context.dataStore.edit { preferences ->
            preferences[KEY_BONUS_PET_SLOTS] =
                (preferences[KEY_BONUS_PET_SLOTS] ?: 0) + slots
        }
    }
}
