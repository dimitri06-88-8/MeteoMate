package com.example.meteomate.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language_code"

    fun getPersistedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun persistLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun wrapContext(context: Context, language: String): Context {
        val locale = localeFromCode(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun localeFromCode(code: String): Locale {
        return when (code) {
            "en" -> Locale("en")
            "ru" -> Locale("ru")
            "es" -> Locale("es")
            "fr" -> Locale("fr")
            "de" -> Locale("de")
            "it" -> Locale("it")
            "pt" -> Locale("pt")
            "zh" -> Locale("zh", "CN")
            "ja" -> Locale("ja")
            "ko" -> Locale("ko")
            "ar" -> Locale("ar")
            "hi" -> Locale("hi")
            "tr" -> Locale("tr")
            "nl" -> Locale("nl")
            "pl" -> Locale("pl")
            "uk" -> Locale("uk")
            "el" -> Locale("el")
            "th" -> Locale("th")
            "vi" -> Locale("vi")
            "sv" -> Locale("sv")
            "da" -> Locale("da")
            "fi" -> Locale("fi")
            "nb" -> Locale("nb")
            "cs" -> Locale("cs")
            "hu" -> Locale("hu")
            "ro" -> Locale("ro")
            "bg" -> Locale("bg")
            "sr" -> Locale("sr")
            "hr" -> Locale("hr")
            "sk" -> Locale("sk")
            "sl" -> Locale("sl")
            "lt" -> Locale("lt")
            "lv" -> Locale("lv")
            "et" -> Locale("et")
            "he" -> Locale("he")
            "id" -> Locale("id")
            "ms" -> Locale("ms")
            "bn" -> Locale("bn")
            "ta" -> Locale("ta")
            "te" -> Locale("te")
            "mr" -> Locale("mr")
            "gu" -> Locale("gu")
            "kn" -> Locale("kn")
            "ml" -> Locale("ml")
            "pa" -> Locale("pa")
            "ur" -> Locale("ur")
            "fa" -> Locale("fa")
            else -> Locale(code)
        }
    }

    fun languageDisplayName(code: String, displayLocale: Locale): String {
        val locale = localeFromCode(code)
        return locale.getDisplayName(displayLocale)
            .replaceFirstChar { it.uppercase() }
    }

    val supportedLanguages = listOf(
        "en", "ru", "es", "fr", "de", "it", "pt", "zh", "ja", "ko",
        "ar", "hi", "tr", "nl", "pl", "uk", "el", "th", "vi", "sv",
        "da", "fi", "nb", "cs", "hu", "ro", "bg", "sr", "hr", "sk",
        "sl", "lt", "lv", "et", "he", "id", "ms", "bn", "ta", "te",
        "mr", "gu", "kn", "ml", "pa", "ur", "fa"
    )
}
