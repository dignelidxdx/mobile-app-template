package com.example.lugarfavorito.data

import com.example.lugarfavorito.model.FavoritePlace
import java.nio.charset.StandardCharsets
import java.util.Base64

object FavoritePlaceCodec {
    private const val separator = '|'

    fun encode(place: FavoritePlace): String = listOf(
        place.id,
        encodeText(place.name),
        place.latitude.toString(),
        place.longitude.toString(),
        place.countryCode,
        place.savedAtEpochMillis.toString(),
    ).joinToString(separator.toString())

    fun decode(value: String): FavoritePlace? = runCatching {
        val parts = value.split(separator)
        require(parts.size == 6)
        FavoritePlace(
            id = parts[0],
            name = decodeText(parts[1]),
            latitude = parts[2].toDouble(),
            longitude = parts[3].toDouble(),
            countryCode = parts[4],
            savedAtEpochMillis = parts[5].toLong(),
        )
    }.getOrNull()

    private fun encodeText(value: String): String = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String = String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8,
    )
}
