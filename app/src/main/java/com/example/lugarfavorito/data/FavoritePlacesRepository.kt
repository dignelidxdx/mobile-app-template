package com.example.lugarfavorito.data

import com.example.lugarfavorito.model.FavoritePlace
import kotlinx.coroutines.flow.Flow

interface FavoritePlacesRepository {
    val favorites: Flow<List<FavoritePlace>>

    suspend fun save(place: FavoritePlace)

    suspend fun delete(id: String)
}
