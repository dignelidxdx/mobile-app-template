package com.example.lugarfavorito.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.example.lugarfavorito.model.Coordinate
import com.example.lugarfavorito.model.PlaceSelection
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface DeviceLocationProvider {
    suspend fun currentCoordinate(): Coordinate
}

class GoogleDeviceLocationProvider(
    private val client: FusedLocationProviderClient,
) : DeviceLocationProvider {
    @SuppressLint("MissingPermission")
    override suspend fun currentCoordinate(): Coordinate {
        val location = client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .await()
            ?: error("No se pudo obtener la ubicación actual.")
        return Coordinate(location.latitude, location.longitude)
    }
}

class PlaceResolver(context: Context) {
    private val geocoder = Geocoder(context, Locale.getDefault())

    @Suppress("DEPRECATION")
    suspend fun resolve(coordinate: Coordinate): PlaceSelection = withContext(Dispatchers.IO) {
        val address = runCatching {
            geocoder.getFromLocation(coordinate.latitude, coordinate.longitude, 1)?.firstOrNull()
        }.getOrNull()

        val coordinateLabel = String.format(
            Locale.getDefault(),
            "%.5f, %.5f",
            coordinate.latitude,
            coordinate.longitude,
        )
        val name = address?.featureName
            ?: address?.locality
            ?: address?.adminArea
            ?: coordinateLabel

        PlaceSelection(
            coordinate = coordinate,
            name = name,
            countryCode = address?.countryCode.orEmpty(),
        )
    }
}
