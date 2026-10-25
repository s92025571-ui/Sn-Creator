package com.sncreator.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sncreator.app.model.OutputLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.snCreatorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sn_creator_settings"
)

/**
 * Stores ONLY the user's preferred generated-content output language (English or Roman Urdu).
 * Per privacy requirements, generated content history is NEVER persisted locally or remotely.
 */
interface PreferencesRepository {
    val preferredOutputLanguage: Flow<OutputLanguage>
    suspend fun setPreferredOutputLanguage(language: OutputLanguage)
}

class DataStorePreferencesRepository(
    private val context: Context
) : PreferencesRepository {
    private object Keys {
        val OUTPUT_LANGUAGE = stringPreferencesKey("preferred_output_language")
    }

    override val preferredOutputLanguage: Flow<OutputLanguage> =
        context.applicationContext.snCreatorDataStore.data.map { prefs ->
            when (prefs[Keys.OUTPUT_LANGUAGE]) {
                OutputLanguage.ROMAN_URDU.apiValue -> OutputLanguage.ROMAN_URDU
                else -> OutputLanguage.ENGLISH
            }
        }

    override suspend fun setPreferredOutputLanguage(language: OutputLanguage) {
        context.applicationContext.snCreatorDataStore.edit { prefs ->
            prefs[Keys.OUTPUT_LANGUAGE] = language.apiValue
        }
    }
}
