package com.example.lugarfavorito.model

import java.util.Locale

data class FavoritePlace(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val savedAtEpochMillis: Long,
) {
    val coordinate: Coordinate
        get() = Coordinate(latitude, longitude)

    companion object {
        fun idFor(coordinate: Coordinate): String = String.format(
            Locale.US,
            "%.6f:%.6f",
            coordinate.latitude,
            coordinate.longitude,
        )
    }
}
