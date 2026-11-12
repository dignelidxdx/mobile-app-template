package com.example.lugarfavorito.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.lugarfavorito.data.FavoritePlacesRepository
import com.example.lugarfavorito.location.CountryLanguageResolver
import com.example.lugarfavorito.location.DeviceLocationProvider
import com.example.lugarfavorito.location.LanguageTarget
import com.example.lugarfavorito.location.PlaceResolver
import com.example.lugarfavorito.model.Coordinate
import com.example.lugarfavorito.model.FavoritePlace
import com.example.lugarfavorito.model.PlaceSelection
import com.example.lugarfavorito.translation.TranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapUiState(
    val favorites: List<FavoritePlace> = emptyList(),
    val selectedPlace: PlaceSelection? = null,
    val cameraTarget: Coordinate? = null,
    val sourceText: String = "",
    val translatedText: String = "",
    val targetLanguage: LanguageTarget = CountryLanguageResolver.languageFor(null),
    val isResolvingPlace: Boolean = false,
    val isTranslating: Boolean = false,
    val errorMessage: String? = null,
) {
    val selectedIsFavorite: Boolean
        get() {
            val id = selectedPlace?.coordinate?.let(FavoritePlace::idFor) ?: return false
            return favorites.any { it.id == id }
        }
}

class MapViewModel(
    private val favoritesRepository: FavoritePlacesRepository,
    private val locationProvider: DeviceLocationProvider,
    private val placeResolver: PlaceResolver,
    private val translationService: TranslationService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private val placeRequestGate = LatestRequestGate()
    private val translationRequestGate = LatestRequestGate()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun locateUser() {
        val request = placeRequestGate.next()
        translationRequestGate.invalidate()
        _uiState.update { it.copy(isTranslating = false, translatedText = "") }
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isResolvingPlace = true, errorMessage = null) }
                val coordinate = locationProvider.currentCoordinate()
                val place = placeResolver.resolve(coordinate)
                if (!placeRequestGate.isCurrent(request)) return@runCatching
                _uiState.update {
                    it.copy(
                        selectedPlace = place,
                        cameraTarget = coordinate,
                        targetLanguage = CountryLanguageResolver.languageFor(place.countryCode),
                        isResolvingPlace = false,
                    )
                }
            }.onFailure { if (placeRequestGate.isCurrent(request)) showPlaceError(it) }
        }
    }

    fun selectCoordinate(coordinate: Coordinate) {
        val request = placeRequestGate.next()
        translationRequestGate.invalidate()
        _uiState.update { it.copy(isTranslating = false, translatedText = "") }
        viewModelScope.launch {
            runCatching {
                _uiState.update {
                    it.copy(isResolvingPlace = true, cameraTarget = coordinate, errorMessage = null)
                }
                val place = placeResolver.resolve(coordinate)
                if (!placeRequestGate.isCurrent(request)) return@runCatching
                _uiState.update {
                    it.copy(
                        selectedPlace = place,
                        targetLanguage = CountryLanguageResolver.languageFor(place.countryCode),
                        isResolvingPlace = false,
                    )
                }
            }.onFailure { if (placeRequestGate.isCurrent(request)) showPlaceError(it) }
        }
    }

    fun selectFavorite(place: FavoritePlace) {
        placeRequestGate.invalidate()
        translationRequestGate.invalidate()
        val selection = PlaceSelection(place.coordinate, place.name, place.countryCode)
        _uiState.update {
            it.copy(
                selectedPlace = selection,
                cameraTarget = place.coordinate,
                targetLanguage = CountryLanguageResolver.languageFor(place.countryCode),
                translatedText = "",
                isTranslating = false,
            )
        }
    }

    fun toggleSelectedFavorite() {
        val selected = _uiState.value.selectedPlace ?: return
        val id = FavoritePlace.idFor(selected.coordinate)
        viewModelScope.launch {
            runCatching {
                if (_uiState.value.favorites.any { it.id == id }) {
                    favoritesRepository.delete(id)
                } else {
                    favoritesRepository.save(
                        FavoritePlace(
                            id = id,
                            name = selected.name,
                            latitude = selected.coordinate.latitude,
                            longitude = selected.coordinate.longitude,
                            countryCode = selected.countryCode,
                            savedAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "No se pudo actualizar el favorito.") }
            }
        }
    }

    fun updateSourceText(text: String) {
        translationRequestGate.invalidate()
        _uiState.update {
            it.copy(sourceText = text, translatedText = "", isTranslating = false)
        }
    }

    fun translate() {
        val text = _uiState.value.sourceText.trim()
        if (text.isEmpty()) return
        val target = _uiState.value.targetLanguage
        val request = translationRequestGate.next()
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(isTranslating = true, errorMessage = null) }
                translationService.translate(text, "es", target.code)
            }.onSuccess { translation ->
                if (translationRequestGate.isCurrent(request)) {
                    _uiState.update { it.copy(translatedText = translation, isTranslating = false) }
                }
            }.onFailure { error ->
                if (translationRequestGate.isCurrent(request)) {
                    _uiState.update {
                        it.copy(
                            isTranslating = false,
                            errorMessage = error.message ?: "No se pudo realizar la traducción.",
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun reportError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun showPlaceError(error: Throwable) {
        _uiState.update {
            it.copy(
                isResolvingPlace = false,
                errorMessage = error.message ?: "No se pudo identificar el lugar.",
            )
        }
    }

    override fun onCleared() {
        translationService.close()
    }
}

class MapViewModelFactory(
    private val favoritesRepository: FavoritePlacesRepository,
    private val locationProvider: DeviceLocationProvider,
    private val placeResolver: PlaceResolver,
    private val translationService: TranslationService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MapViewModel::class.java))
        return MapViewModel(
            favoritesRepository,
            locationProvider,
            placeResolver,
            translationService,
        ) as T
    }
}
