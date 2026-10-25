package com.sncreator.app.data

import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.ApiErrorPayload
import com.sncreator.app.model.AppError
import com.sncreator.app.model.GenerateApiRequest
import com.sncreator.app.model.GenerateApiResponse
import com.sncreator.app.model.OutputLanguage
import com.sncreator.app.model.QuotaApiResponse
import com.sncreator.app.model.QuotaDto
import com.sncreator.app.model.TranslateApiRequest
import com.sncreator.app.model.VoiceOverDuration
import com.sncreator.app.util.PakistanTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class ApiOutcome<out T> {
    data class Success<T>(val data: T) : ApiOutcome<T>()
    data class Error(val error: AppError, val latestQuota: QuotaDto? = null) : ApiOutcome<Nothing>()
}

interface ContentGenerationRepository {
    val isBackendConfigured: Boolean
    suspend fun fetchDailyQuota(): ApiOutcome<QuotaDto>
    suspend fun generateContent(
        tool: AiToolType,
        resolvedGameName: String,
        topic: String,
        language: OutputLanguage,
        duration: VoiceOverDuration?,
        platformHint: String = "",
        aspectRatio: String = "",
        requestId: String = UUID.randomUUID().toString()
    ): ApiOutcome<GenerateApiResponse>

    suspend fun translateContent(
        tool: AiToolType,
        resolvedGameName: String,
        currentContent: String,
        targetLanguage: OutputLanguage,
        duration: VoiceOverDuration?,
        requestId: String = UUID.randomUUID().toString()
    ): ApiOutcome<GenerateApiResponse>
}

class NetworkContentGenerationRepository(
    private val backendBaseUrl: String,
    private val authRepository: AuthRepository,
    private val connectivityMonitor: ConnectivityMonitor,
    private val httpClient: OkHttpClient = defaultOkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ContentGenerationRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Client-side mutex prevents simultaneous duplicate taps before a second request even hits the network.
    // Server-side requestId reservation in Cloudflare D1 also enforces idempotency and atomic 10/day limits.
    private val generationMutex = Mutex()

    private val normalizedBaseUrl: String = backendBaseUrl.trim().trimEnd('/')

    override val isBackendConfigured: Boolean
        get() = normalizedBaseUrl.isNotEmpty() &&
            (normalizedBaseUrl.startsWith("https://") || normalizedBaseUrl.startsWith("http://")) &&
            !normalizedBaseUrl.contains("YOUR_WORKER_SUBDOMAIN", ignoreCase = true)

    override suspend fun fetchDailyQuota(): ApiOutcome<QuotaDto> {
        if (!isBackendConfigured) {
            return ApiOutcome.Error(
                AppError.BackendMisconfigured(
                    "Backend endpoint is not configured yet. Set SN_BACKEND_BASE_URL to your deployed Cloudflare Worker URL (see README.md)."
                )
            )
        }
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return ApiOutcome.Error(AppError.Offline)
        }

        val token = authRepository.getValidIdToken(forceRefresh = false).getOrElse {
            return ApiOutcome.Error(AppError.AuthExpiredOrInvalid)
        }

        return withContext(ioDispatcher) {
            executeAuthenticatedRequest(
                method = "GET",
                path = "/v1/quota",
                bearerToken = token,
                requestId = UUID.randomUUID().toString(),
                jsonBody = null
            ) { body ->
                json.decodeFromString<QuotaApiResponse>(body).quota
            }
        }
    }

    override suspend fun generateContent(
        tool: AiToolType,
        resolvedGameName: String,
        topic: String,
        language: OutputLanguage,
        duration: VoiceOverDuration?,
        platformHint: String,
        aspectRatio: String,
        requestId: String
    ): ApiOutcome<GenerateApiResponse> {
        if (!isBackendConfigured) {
            return ApiOutcome.Error(
                AppError.BackendMisconfigured(
                    "SN Creator backend URL is missing. Configure SN_BACKEND_BASE_URL with your Cloudflare Worker endpoint as described in README.md."
                )
            )
        }
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return ApiOutcome.Error(AppError.Offline)
        }
        if (!generationMutex.tryLock()) {
            return ApiOutcome.Error(AppError.DuplicateRequestInFlight)
        }

        try {
            val token = authRepository.getValidIdToken(forceRefresh = false).getOrElse {
                return ApiOutcome.Error(AppError.AuthExpiredOrInvalid)
            }

            val payload = GenerateApiRequest(
                requestId = requestId,
                tool = tool.apiValue,
                game = resolvedGameName,
                topic = topic,
                language = language.apiValue,
                duration = if (tool == AiToolType.VOICEOVER_SCRIPT) {
                    (duration ?: VoiceOverDuration.SEC_60).apiValue
                } else {
                    null
                },
                platformHint = platformHint.takeIf { it.isNotBlank() },
                aspectRatio = aspectRatio.takeIf { it.isNotBlank() }
            )

            val encodedBody = json.encodeToString(GenerateApiRequest.serializer(), payload)

            return withContext(ioDispatcher) {
                executeAuthenticatedRequest(
                    method = "POST",
                    path = "/v1/generate",
                    bearerToken = token,
                    requestId = requestId,
                    jsonBody = encodedBody
                ) { responseText ->
                    json.decodeFromString<GenerateApiResponse>(responseText)
                }
            }
        } finally {
            generationMutex.unlock()
        }
    }

    override suspend fun translateContent(
        tool: AiToolType,
        resolvedGameName: String,
        currentContent: String,
        targetLanguage: OutputLanguage,
        duration: VoiceOverDuration?,
        requestId: String
    ): ApiOutcome<GenerateApiResponse> {
        if (!isBackendConfigured) {
            return ApiOutcome.Error(
                AppError.BackendMisconfigured(
                    "SN Creator backend URL is missing. Configure SN_BACKEND_BASE_URL with your Cloudflare Worker endpoint as described in README.md."
                )
            )
        }
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return ApiOutcome.Error(AppError.Offline)
        }
        if (!generationMutex.tryLock()) {
            return ApiOutcome.Error(AppError.DuplicateRequestInFlight)
        }

        try {
            val token = authRepository.getValidIdToken(forceRefresh = false).getOrElse {
                return ApiOutcome.Error(AppError.AuthExpiredOrInvalid)
            }

            val payload = TranslateApiRequest(
                requestId = requestId,
                tool = tool.apiValue,
                game = resolvedGameName,
                currentContent = currentContent,
                targetLanguage = targetLanguage.apiValue,
                duration = duration?.apiValue
            )

            val encodedBody = json.encodeToString(TranslateApiRequest.serializer(), payload)

            return withContext(ioDispatcher) {
                executeAuthenticatedRequest(
                    method = "POST",
                    path = "/v1/translate",
                    bearerToken = token,
                    requestId = requestId,
                    jsonBody = encodedBody
                ) { responseText ->
                    json.decodeFromString<GenerateApiResponse>(responseText)
                }
            }
        } finally {
            generationMutex.unlock()
        }
    }

    private fun <T> executeAuthenticatedRequest(
        method: String,
        path: String,
        bearerToken: String,
        requestId: String,
        jsonBody: String?,
        decodeSuccess: (String) -> T
    ): ApiOutcome<T> {
        val url = "$normalizedBaseUrl$path"
        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $bearerToken")
            .header("X-Request-Id", requestId)
            .header("Accept", "application/json")

        if (method == "POST") {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            builder.post((jsonBody ?: "{}").toRequestBody(mediaType))
        } else {
            builder.get()
        }

        return try {
            httpClient.newCall(builder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val parsed = decodeSuccess(rawBody)
                    ApiOutcome.Success(parsed)
                } else {
                    mapHttpError(response.code, rawBody)
                }
            }
        } catch (_: SocketTimeoutException) {
            ApiOutcome.Error(AppError.RequestTimeout)
        } catch (_: IOException) {
            if (!connectivityMonitor.isCurrentlyOnline()) {
                ApiOutcome.Error(AppError.Offline)
            } else {
                ApiOutcome.Error(AppError.ServerUnavailable)
            }
        } catch (e: Exception) {
            ApiOutcome.Error(
                AppError.Unknown("Unexpected response from server: ${e.localizedMessage.orEmpty()}")
            )
        }
    }

    private fun mapHttpError(statusCode: Int, rawBody: String): ApiOutcome.Error {
        val parsed = runCatching {
            json.decodeFromString<ApiErrorPayload>(rawBody)
        }.getOrNull()

        val code = parsed?.errorCode.orEmpty()
        val message = parsed?.message.orEmpty()
        val quota = parsed?.quota

        val appError = when {
            code == "DAILY_QUOTA_EXCEEDED" -> {
                val resetFormatted = PakistanTime.formatResetDisplay(quota?.resetsAtIso)
                AppError.DailyQuotaExhausted(resetFormatted)
            }
            code == "DUPLICATE_REQUEST" || statusCode == 409 -> AppError.DuplicateRequestInFlight
            code == "GEMINI_QUOTA_EXHAUSTED" -> AppError.ApiQuotaExhausted
            code == "GEMINI_FREE_TIER_UNAVAILABLE" -> AppError.GeminiFreeTierUnavailable
            code == "SERVER_MISCONFIGURED" -> AppError.BackendMisconfigured(
                message.ifBlank { "Backend secrets (GEMINI_API_KEY or FIREBASE_PROJECT_ID) are not configured on the server." }
            )
            code == "INVALID_INPUT" || statusCode == 400 -> AppError.InvalidInput(
                message.ifBlank { "Please check your input fields and try again." }
            )
            statusCode == 401 || statusCode == 403 || code == "AUTH_INVALID" -> AppError.AuthExpiredOrInvalid
            statusCode == 408 || statusCode == 504 || code == "UPSTREAM_TIMEOUT" -> AppError.RequestTimeout
            statusCode == 429 -> AppError.ApiQuotaExhausted
            statusCode in 500..599 -> AppError.ServerUnavailable
            else -> AppError.Unknown(message)
        }

        return ApiOutcome.Error(appError, latestQuota = quota)
    }

    companion object {
        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(28, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(32, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false) // Prevent automatic silent duplicate charges
                .build()
        }
    }
}
