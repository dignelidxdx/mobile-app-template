package com.example.lugarfavorito.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.lugarfavorito.model.FavoritePlace
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesFavoritePlacesRepository(
    private val dataStore: DataStore<Preferences>,
) : FavoritePlacesRepository {
    private val favoritesKey = stringSetPreferencesKey("favorite_places")

    override val favorites: Flow<List<FavoritePlace>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            preferences[favoritesKey]
                .orEmpty()
                .mapNotNull(FavoritePlaceCodec::decode)
                .sortedByDescending(FavoritePlace::savedAtEpochMillis)
        }

    override suspend fun save(place: FavoritePlace) {
        dataStore.edit { preferences ->
            val withoutPreviousVersion = preferences[favoritesKey]
                .orEmpty()
                .filterNot { FavoritePlaceCodec.decode(it)?.id == place.id }
                .toSet()
            preferences[favoritesKey] = withoutPreviousVersion + FavoritePlaceCodec.encode(place)
        }
    }

    override suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            preferences[favoritesKey] = preferences[favoritesKey]
                .orEmpty()
                .filterNot { FavoritePlaceCodec.decode(it)?.id == id }
                .toSet()
        }
    }
}
