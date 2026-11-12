package com.example.lugarfavorito.location

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryLanguageResolverTest {
    @Test
    fun `uses italian for Italy`() {
        assertEquals("it", CountryLanguageResolver.languageFor("IT").code)
    }

    @Test
    fun `uses japanese for Japan`() {
        assertEquals("ja", CountryLanguageResolver.languageFor("jp").code)
    }

    @Test
    fun `keeps Spanish in Argentina`() {
        assertEquals("es", CountryLanguageResolver.languageFor("AR").code)
    }

    @Test
    fun `falls back to English for unknown countries`() {
        assertEquals("en", CountryLanguageResolver.languageFor("XX").code)
        assertEquals(false, CountryLanguageResolver.languageFor("XX").wasDetected)
    }

    @Test
    fun `detects languages for Czechia Israel and Saudi Arabia`() {
        assertEquals("cs", CountryLanguageResolver.languageFor("CZ").code)
        assertEquals("he", CountryLanguageResolver.languageFor("IL").code)
        assertEquals("ar", CountryLanguageResolver.languageFor("SA").code)
    }
}
