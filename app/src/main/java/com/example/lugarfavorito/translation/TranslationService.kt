package com.example.lugarfavorito.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

interface TranslationService {
    suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String

    fun close()
}

class MlKitTranslationService : TranslationService {
    private var translator: Translator? = null
    private var languagePair: Pair<String, String>? = null

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): String {
        if (sourceLanguage == targetLanguage) return text

        val source = requireNotNull(TranslateLanguage.fromLanguageTag(sourceLanguage))
        val target = requireNotNull(TranslateLanguage.fromLanguageTag(targetLanguage))
        val pair = source to target
        if (pair != languagePair) {
            translator?.close()
            translator = Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build(),
            )
            languagePair = pair
        }

        val activeTranslator = requireNotNull(translator)
        activeTranslator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
        return activeTranslator.translate(text).await()
    }

    override fun close() {
        translator?.close()
        translator = null
        languagePair = null
    }
}
