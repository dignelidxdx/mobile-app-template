package com.example.lugarfavorito.data

import com.example.lugarfavorito.model.FavoritePlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritePlaceCodecTest {
    @Test
    fun `round trip preserves place data`() {
        val place = FavoritePlace(
            id = "41.902800:12.496400",
            name = "Roma | Centro",
            latitude = 41.9028,
            longitude = 12.4964,
            countryCode = "IT",
            savedAtEpochMillis = 1234L,
        )

        assertEquals(place, FavoritePlaceCodec.decode(FavoritePlaceCodec.encode(place)))
    }

    @Test
    fun `malformed value is ignored`() {
        assertNull(FavoritePlaceCodec.decode("not-a-place"))
    }
}
