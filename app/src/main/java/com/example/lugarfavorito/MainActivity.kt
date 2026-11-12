package com.example.lugarfavorito

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lugarfavorito.data.PreferencesFavoritePlacesRepository
import com.example.lugarfavorito.location.GoogleDeviceLocationProvider
import com.example.lugarfavorito.location.PlaceResolver
import com.example.lugarfavorito.translation.MlKitTranslationService
import com.example.lugarfavorito.ui.LugarFavoritoTheme
import com.example.lugarfavorito.ui.MapScreen
import com.example.lugarfavorito.ui.MapViewModel
import com.example.lugarfavorito.ui.MapViewModelFactory
import com.google.android.gms.location.LocationServices

private val Context.favoritePlacesDataStore by preferencesDataStore(name = "favorite_places")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = MapViewModelFactory(
            favoritesRepository = PreferencesFavoritePlacesRepository(favoritePlacesDataStore),
            locationProvider = GoogleDeviceLocationProvider(
                LocationServices.getFusedLocationProviderClient(this),
            ),
            placeResolver = PlaceResolver(this),
            translationService = MlKitTranslationService(),
        )

        setContent {
            LugarFavoritoTheme {
                val mapViewModel: MapViewModel = viewModel(factory = factory)
                MapScreen(mapViewModel)
            }
        }
    }
}
