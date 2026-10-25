package com.sncreator.app.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SupportLinks {
    const val HELP_WHATSAPP_BASE_URL = "https://wa.me/923288501635"
    const val HELP_PREFILL_MESSAGE = "Hello SN Creator team, I need help with the app."
    const val UNCONFIGURED_CONTACT_PLACEHOLDER = "UNCONFIGURED_CONTACT_NUMBER"

    /**
     * Builds the WhatsApp Get Help URL with URL-encoded prefilled support message.
     */
    fun buildGetHelpWhatsAppUrl(
        baseUrl: String = HELP_WHATSAPP_BASE_URL,
        prefillMessage: String = HELP_PREFILL_MESSAGE
    ): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val encoded = URLEncoder.encode(prefillMessage, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
        return "$cleanBase?text=$encoded"
    }

    /**
     * Resolves the Settings "Contact SN Creator" WhatsApp URL only when a real phone number
     * has been configured by the app owner. Never invents or guesses a phone number.
     */
    fun resolveContactSnCreatorUrl(configuredNumber: String): String? {
        val trimmed = configuredNumber.trim()
        if (
            trimmed.isEmpty() ||
            trimmed.equals(UNCONFIGURED_CONTACT_PLACEHOLDER, ignoreCase = true) ||
            trimmed.contains("PLACEHOLDER", ignoreCase = true) ||
            trimmed.contains("TBD", ignoreCase = true)
        ) {
            return null
        }
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 8) return null
        return "https://wa.me/$digitsOnly"
    }
}
