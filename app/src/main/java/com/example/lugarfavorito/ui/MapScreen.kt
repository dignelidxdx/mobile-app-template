package com.example.lugarfavorito.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lugarfavorito.model.Coordinate
import com.example.lugarfavorito.model.FavoritePlace
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var locationPermissionGranted by remember { mutableStateOf(context.hasLocationPermission()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationPermissionGranted) viewModel.locateUser()
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) viewModel.updateSourceText(spokenText)
    }

    LaunchedEffect(Unit) {
        if (locationPermissionGranted) {
            viewModel.locateUser()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lugar Favorito", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Mantén pulsado el mapa para elegir",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
        ) {
            MapPanel(
                modifier = Modifier.weight(1f),
                state = state,
                locationPermissionGranted = locationPermissionGranted,
                onCoordinateSelected = viewModel::selectCoordinate,
                onFavoriteSelected = viewModel::selectFavorite,
                onLocate = {
                    if (locationPermissionGranted) viewModel.locateUser()
                    else locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
            )

            state.selectedPlace?.let {
                SelectedPlaceCard(
                    state = state,
                    onToggleFavorite = viewModel::toggleSelectedFavorite,
                )
            }

            if (state.favorites.isNotEmpty()) {
                FavoritesRow(state.favorites, viewModel::selectFavorite)
            }

            TranslationCard(
                state = state,
                onTextChanged = viewModel::updateSourceText,
                onTranslate = viewModel::translate,
                onMicrophone = {
                    val intent = spanishSpeechIntent()
                    if (intent.resolveActivity(context.packageManager) != null) {
                        speechLauncher.launch(intent)
                    } else {
                        viewModel.reportError("Este dispositivo no tiene reconocimiento de voz disponible.")
                    }
                },
            )
        }
    }
}

@Composable
private fun MapPanel(
    modifier: Modifier,
    state: MapUiState,
    locationPermissionGranted: Boolean,
    onCoordinateSelected: (Coordinate) -> Unit,
    onFavoriteSelected: (FavoritePlace) -> Unit,
    onLocate: () -> Unit,
) {
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(state.cameraTarget) {
        state.cameraTarget?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f),
            )
        }
    }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false),
            onMapLongClick = { onCoordinateSelected(Coordinate(it.latitude, it.longitude)) },
        ) {
            state.favorites.forEach { favorite ->
                Marker(
                    state = rememberUpdatedMarkerState(
                        LatLng(favorite.latitude, favorite.longitude),
                    ),
                    title = favorite.name,
                    snippet = "Lugar guardado",
                    onClick = {
                        onFavoriteSelected(favorite)
                        false
                    },
                )
            }
            state.selectedPlace?.let { selected ->
                Marker(
                    state = rememberUpdatedMarkerState(
                        LatLng(selected.coordinate.latitude, selected.coordinate.longitude),
                    ),
                    title = selected.name,
                )
            }
        }

        FloatingActionButton(
            onClick = onLocate,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Ir a mi ubicación")
        }

        if (state.isResolvingPlace) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
            )
        }
    }
}

@Composable
private fun SelectedPlaceCard(state: MapUiState, onToggleFavorite: () -> Unit) {
    val place = state.selectedPlace ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(place.name, fontWeight = FontWeight.SemiBold)
                Text(
                    String.format(
                        Locale.US,
                        "%.5f, %.5f",
                        place.coordinate.latitude,
                        place.coordinate.longitude,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Idioma local: ${state.targetLanguage.displayName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (state.selectedIsFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (state.selectedIsFavorite) {
                        "Eliminar de favoritos"
                    } else {
                        "Guardar en favoritos"
                    },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FavoritesRow(
    favorites: List<FavoritePlace>,
    onFavoriteSelected: (FavoritePlace) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Favoritos (${favorites.size})",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.labelLarge,
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(favorites, key = FavoritePlace::id) { favorite ->
                AssistChip(
                    onClick = { onFavoriteSelected(favorite) },
                    label = {
                        Text(
                            favorite.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TranslationCard(
    state: MapUiState,
    onTextChanged: (String) -> Unit,
    onTranslate: () -> Unit,
    onMicrophone: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Translate, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Español → ${state.targetLanguage.displayName}",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.sourceText,
                onValueChange = onTextChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Escribe o habla en español") },
                trailingIcon = {
                    IconButton(onClick = onMicrophone) {
                        Icon(Icons.Default.Mic, contentDescription = "Hablar en español")
                    }
                },
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onTranslate,
                enabled = state.sourceText.isNotBlank() && !state.isTranslating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isTranslating) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Traducir")
                }
            }
            if (state.translatedText.isNotBlank()) {
                Text(
                    state.translatedText,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Traducido por Google",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun spanishSpeechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es")
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla en español")
}
