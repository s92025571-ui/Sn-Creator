package com.sncreator.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supported games in SN Creator with searchable filtering and custom game entry support.
 */
enum class SupportedGame(
    val id: String,
    val displayName: String,
    val genreHint: String,
    val requiresCustomName: Boolean = false
) {
    PUBG_MOBILE(
        id = "pubg_mobile",
        displayName = "PUBG Mobile",
        genreHint = "Battle Royale · Tactical Squads"
    ),
    FREE_FIRE(
        id = "free_fire",
        displayName = "Free Fire",
        genreHint = "Fast-Paced Battle Royale · Clash Squad"
    ),
    LAST_ASYLUM_PLAGUE(
        id = "last_asylum_plague",
        displayName = "Last Asylum: Plague",
        genreHint = "Survival Strategy · Outbreak Defense"
    ),
    GANGSTAR_MIRAGE_CITY(
        id = "gangstar_mirage_city",
        displayName = "Gangstar Mirage City",
        genreHint = "Open-World Action · Street Missions"
    ),
    OTHER_GAME(
        id = "other_game",
        displayName = "Other Game",
        genreHint = "Enter any custom mobile, PC, or console game",
        requiresCustomName = true
    );

    companion object {
        fun filterByQuery(query: String): List<SupportedGame> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return entries
            return entries.filter { game ->
                game.displayName.contains(trimmed, ignoreCase = true) ||
                    game.genreHint.contains(trimmed, ignoreCase = true) ||
                    game.requiresCustomName
            }
        }
    }
}

/**
 * The five real AI content tools supported by SN Creator.
 */
@Serializable
enum class AiToolType(
    val apiValue: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val topicPlaceholder: String
) {
    @SerialName("GAMING_TITLES")
    GAMING_TITLES(
        apiValue = "GAMING_TITLES",
        title = "Gaming Titles",
        shortTitle = "Titles",
        subtitle = "High-retention, game-specific video title angles without generic repetition.",
        topicPlaceholder = "e.g., Solo vs Squad 24 kills clutch in final Erangel zone with M416"
    ),

    @SerialName("HASHTAGS")
    HASHTAGS(
        apiValue = "HASHTAGS",
        title = "Hashtags",
        shortTitle = "Hashtags",
        subtitle = "Relevant broad, niche, and discovery gaming tags tailored to your platform.",
        topicPlaceholder = "e.g., Ranked Clash Squad sniper montage for YouTube Shorts and TikTok"
    ),

    @SerialName("VOICEOVER_SCRIPT")
    VOICEOVER_SCRIPT(
        apiValue = "VOICEOVER_SCRIPT",
        title = "Voice-over Scripts",
        shortTitle = "Voice-over",
        subtitle = "Ready-to-record creator scripts paced accurately to your chosen duration.",
        topicPlaceholder = "e.g., Top 5 sensitivity and recoil settings after the new update"
    ),

    @SerialName("THUMBNAIL_PROMPT")
    THUMBNAIL_PROMPT(
        apiValue = "THUMBNAIL_PROMPT",
        title = "AI Thumbnail Prompts",
        shortTitle = "Thumbnails",
        subtitle = "Copy-ready image prompts detailing focal subject, lighting, text hierarchy, and ratio.",
        topicPlaceholder = "e.g., Intense night survival ambush against mutant horde, 16:9 YouTube thumbnail"
    ),

    @SerialName("VIDEO_IDEAS")
    VIDEO_IDEAS(
        apiValue = "VIDEO_IDEAS",
        title = "Gaming Video Ideas",
        shortTitle = "Video Ideas",
        subtitle = "Distinct video concepts with opening hooks, recording plans, and viewer appeal.",
        topicPlaceholder = "e.g., Fresh challenge ideas for growing a new gaming channel"
    );
}

/**
 * Output language for generated AI content.
 * Note: Changing this preference affects ONLY generated AI output, never the fixed app UI language.
 */
@Serializable
enum class OutputLanguage(
    val apiValue: String,
    val displayName: String,
    val description: String
) {
    @SerialName("ENGLISH")
    ENGLISH(
        apiValue = "ENGLISH",
        displayName = "English",
        description = "Clean, natural English gaming creator style"
    ),

    @SerialName("ROMAN_URDU")
    ROMAN_URDU(
        apiValue = "ROMAN_URDU",
        displayName = "Roman Urdu",
        description = "Urdu written in Latin script (no Hindi/Devanagari or Nastaliq script)"
    );

    fun opposite(): OutputLanguage = when (this) {
        ENGLISH -> ROMAN_URDU
        ROMAN_URDU -> ENGLISH
    }
}

/**
 * Voice-over duration presets with realistic spoken word-count pacing targets.
 */
@Serializable
enum class VoiceOverDuration(
    val apiValue: String,
    val label: String,
    val pacingSummary: String,
    val approximateWords: IntRange
) {
    @SerialName("15s")
    SEC_15("15s", "15 seconds", "Rapid hook + 1 punchy point + quick CTA (~35–45 words)", 35..45),

    @SerialName("30s")
    SEC_30("30s", "30 seconds", "Fast Shorts/Reels hook, setup, payoff, CTA (~70–85 words)", 70..85),

    @SerialName("60s")
    SEC_60("60s", "60 seconds", "Full 1-minute vertical script with 3 crisp beats (~140–165 words)", 140..165),

    @SerialName("2m")
    MIN_2("2m", "2 minutes", "Structured mid-short video with intro, 3 segments & CTA (~280–330 words)", 280..330),

    @SerialName("5m")
    MIN_5("5m", "5 minutes", "Deep-dive multi-chapter commentary with retention resets (~700–825 words)", 700..825),

    @SerialName("10m")
    MIN_10("10m", "10 minutes", "Full flagship episode script with timestamped chapters (~1350–1600 words)", 1350..1600);
}

data class AuthenticatedUser(
    val uid: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String? = null
) {
    val greetingName: String
        get() = displayName.trim().ifEmpty {
            email?.substringBefore("@")?.takeIf { it.isNotBlank() } ?: "Creator"
        }
}

@Serializable
data class QuotaDto(
    val limit: Int = DAILY_QUOTA_LIMIT,
    val used: Int = 0,
    val remaining: Int = DAILY_QUOTA_LIMIT,
    val pktDate: String = "",
    val resetsAtIso: String = "",
    val timezone: String = "Asia/Karachi"
) {
    companion object {
        const val DAILY_QUOTA_LIMIT = 10
    }
}

@Serializable
data class GenerateApiRequest(
    val requestId: String,
    val tool: String,
    val game: String,
    val topic: String,
    val language: String,
    val duration: String? = null,
    val platformHint: String? = null,
    val aspectRatio: String? = null
)

@Serializable
data class TranslateApiRequest(
    val requestId: String,
    val tool: String,
    val game: String,
    val currentContent: String,
    val targetLanguage: String,
    val duration: String? = null
)

@Serializable
data class GenerateApiResponse(
    val content: String,
    val tool: String,
    val game: String,
    val language: String,
    val duration: String? = null,
    val model: String = "",
    val quota: QuotaDto
)

@Serializable
data class QuotaApiResponse(
    val quota: QuotaDto
)

@Serializable
data class ApiErrorPayload(
    val errorCode: String = "UNKNOWN_ERROR",
    val message: String = "Something went wrong. Please try again.",
    val quota: QuotaDto? = null
)

/**
 * Active generation session result held ONLY in transient memory (never saved as history).
 */
data class ActiveResultSession(
    val tool: AiToolType,
    val selectedGame: SupportedGame,
    val resolvedGameName: String,
    val topic: String,
    val language: OutputLanguage,
    val duration: VoiceOverDuration?,
    val platformHint: String,
    val aspectRatio: String,
    val content: String
)

/**
 * Strongly typed application errors with user-understandable messages and retry guidance.
 */
sealed class AppError(
    val userMessage: String,
    val canRetry: Boolean
) {
    data object Offline : AppError(
        userMessage = "Internet connection required for AI generation.",
        canRetry = true
    )

    data class DailyQuotaExhausted(val resetDescription: String) : AppError(
        userMessage = "You have used all 10 daily generations. Your quota resets at $resetDescription.",
        canRetry = false
    )

    data object ApiQuotaExhausted : AppError(
        userMessage = "The AI service free-tier rate limit is temporarily busy. Your daily SN Creator quota was not charged. Please wait a moment and tap Retry.",
        canRetry = true
    )

    data object GeminiFreeTierUnavailable : AppError(
        userMessage = "The Gemini free-tier model is currently unavailable or misconfigured on the backend. Your daily quota was not charged.",
        canRetry = true
    )

    data object RequestTimeout : AppError(
        userMessage = "The AI request timed out before completing. Your daily quota was not charged. Please tap Retry.",
        canRetry = true
    )

    data object ServerUnavailable : AppError(
        userMessage = "SN Creator backend is temporarily unreachable. Please check your connection and tap Retry.",
        canRetry = true
    )

    data object AuthExpiredOrInvalid : AppError(
        userMessage = "Your sign-in session expired or could not be verified. Please sign in with Google again.",
        canRetry = false
    )

    data object AuthCancelled : AppError(
        userMessage = "Google sign-in was cancelled. Sign in with Google to continue.",
        canRetry = true
    )

    data class AuthMisconfigured(val detail: String) : AppError(
        userMessage = detail,
        canRetry = true
    )

    data class BackendMisconfigured(val detail: String) : AppError(
        userMessage = detail,
        canRetry = false
    )

    data class InvalidInput(val detail: String) : AppError(
        userMessage = detail,
        canRetry = false
    )

    data object DuplicateRequestInFlight : AppError(
        userMessage = "A generation request is already in progress. Please wait for it to finish.",
        canRetry = false
    )

    data class Unknown(val detail: String) : AppError(
        userMessage = detail.ifBlank { "An unexpected error occurred. Your quota was not charged. Please try again." },
        canRetry = true
    )
}

/**
 * Input validation rules enforced prior to sending authenticated generation requests.
 */
object InputValidator {
    const val MIN_TOPIC_LENGTH = 3
    const val MAX_TOPIC_LENGTH = 600
    const val MAX_CUSTOM_GAME_LENGTH = 80
    const val MAX_HINT_LENGTH = 60

    fun resolveGameName(game: SupportedGame, customGameName: String): Result<String> {
        return if (game.requiresCustomName) {
            val cleaned = customGameName.trim()
            when {
                cleaned.length < 2 -> Result.failure(
                    IllegalArgumentException("Please enter your custom game name (at least 2 characters).")
                )
                cleaned.length > MAX_CUSTOM_GAME_LENGTH -> Result.failure(
                    IllegalArgumentException("Custom game name must be $MAX_CUSTOM_GAME_LENGTH characters or fewer.")
                )
                else -> Result.success(cleaned)
            }
        } else {
            Result.success(game.displayName)
        }
    }

    fun validateTopic(topic: String): Result<String> {
        val cleaned = topic.trim()
        return when {
            cleaned.length < MIN_TOPIC_LENGTH -> Result.failure(
                IllegalArgumentException("Please enter a gaming topic or context (at least $MIN_TOPIC_LENGTH characters).")
            )
            cleaned.length > MAX_TOPIC_LENGTH -> Result.failure(
                IllegalArgumentException("Topic is too long (maximum $MAX_TOPIC_LENGTH characters).")
            )
            else -> Result.success(cleaned)
        }
    }

    fun sanitizeOptionalHint(hint: String): String {
        return hint.trim().take(MAX_HINT_LENGTH)
    }
}
