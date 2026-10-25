package com.sncreator.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sncreator.app.data.ApiOutcome
import com.sncreator.app.data.AuthRepository
import com.sncreator.app.data.AuthResult
import com.sncreator.app.data.ConnectivityMonitor
import com.sncreator.app.data.ContentGenerationRepository
import com.sncreator.app.data.PreferencesRepository
import com.sncreator.app.model.ActiveResultSession
import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.AppError
import com.sncreator.app.model.AuthenticatedUser
import com.sncreator.app.model.InputValidator
import com.sncreator.app.model.OutputLanguage
import com.sncreator.app.model.QuotaDto
import com.sncreator.app.model.SupportedGame
import com.sncreator.app.model.VoiceOverDuration
import com.sncreator.app.util.PakistanTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class MainTab(val label: String) {
    HOME("Home"),
    CREATE("Create"),
    GET_HELP("Get Help"),
    SETTINGS("Settings")
}

sealed class RetryableAction {
    data object RefreshQuota : RetryableAction()
    data object Generate : RetryableAction()
    data object Regenerate : RetryableAction()
    data class SwitchLanguage(val targetLanguage: OutputLanguage) : RetryableAction()
}

data class SnCreatorUiState(
    val isSplashVisible: Boolean = true,
    val currentUser: AuthenticatedUser? = null,
    val isAuthLoading: Boolean = false,
    val authError: AppError? = null,

    val isOnline: Boolean = true,
    val selectedTab: MainTab = MainTab.HOME,

    // Quota state (trusted source is backend; client reflects latest verified server state)
    val quota: QuotaDto = QuotaDto(
        limit = QuotaDto.DAILY_QUOTA_LIMIT,
        used = 0,
        remaining = QuotaDto.DAILY_QUOTA_LIMIT,
        pktDate = PakistanTime.currentPktDateString()
    ),
    val isQuotaLoading: Boolean = false,
    val quotaError: AppError? = null,

    // Create form state
    val selectedGame: SupportedGame = SupportedGame.PUBG_MOBILE,
    val customGameName: String = "",
    val gameSearchQuery: String = "",
    val selectedTool: AiToolType = AiToolType.GAMING_TITLES,
    val topicInput: String = "",
    val platformHint: String = "YouTube & Shorts",
    val aspectRatioHint: String = "16:9",
    val createOutputLanguage: OutputLanguage = OutputLanguage.ENGLISH,
    val preferredOutputLanguage: OutputLanguage = OutputLanguage.ENGLISH,
    val voiceOverDuration: VoiceOverDuration = VoiceOverDuration.SEC_60,

    // Generation & transient active result state (never persisted as history)
    val isGenerating: Boolean = false,
    val generationError: AppError? = null,
    val activeResult: ActiveResultSession? = null,
    val isShowingResultScreen: Boolean = false,
    val resultError: AppError? = null,
    val lastRetryableAction: RetryableAction? = null
) {
    val isQuotaExhausted: Boolean
        get() = quota.remaining <= 0

    val formattedResetTime: String
        get() = PakistanTime.formatResetDisplay(quota.resetsAtIso)
}

class SnCreatorViewModel(
    private val authRepository: AuthRepository,
    private val contentRepository: ContentGenerationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SnCreatorUiState(
            currentUser = authRepository.getCurrentUser(),
            isOnline = connectivityMonitor.isCurrentlyOnline()
        )
    )
    val uiState: StateFlow<SnCreatorUiState> = _uiState.asStateFlow()

    // Secondary synchronous guard against rapid duplicate UI clicks in the same frame
    private var inFlightFlag = false

    init {
        observeConnectivity()
        observeLanguagePreference()
        observeAuthenticationState()
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityMonitor.isOnlineFlow.collect { online ->
                _uiState.update { state ->
                    val clearedOfflineGenError =
                        if (online && state.generationError is AppError.Offline) null else state.generationError
                    val clearedOfflineResultError =
                        if (online && state.resultError is AppError.Offline) null else state.resultError
                    state.copy(
                        isOnline = online,
                        generationError = clearedOfflineGenError,
                        resultError = clearedOfflineResultError
                    )
                }
            }
        }
    }

    private fun observeLanguagePreference() {
        viewModelScope.launch {
            preferencesRepository.preferredOutputLanguage.collect { preferred ->
                _uiState.update { state ->
                    state.copy(
                        preferredOutputLanguage = preferred,
                        createOutputLanguage = preferred
                    )
                }
            }
        }
    }

    private fun observeAuthenticationState() {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                val previousUid = _uiState.value.currentUser?.uid
                _uiState.update { state ->
                    state.copy(
                        currentUser = user,
                        authError = if (user != null) null else state.authError,
                        // Clear any transient active result if user changes or signs out
                        activeResult = if (user == null || user.uid != previousUid) null else state.activeResult,
                        isShowingResultScreen = if (user == null || user.uid != previousUid) false else state.isShowingResultScreen
                    )
                }
                if (user != null && user.uid != previousUid) {
                    refreshQuota()
                }
            }
        }
    }

    fun onSplashFinished() {
        _uiState.update { it.copy(isSplashVisible = false) }
        if (_uiState.value.currentUser != null) {
            refreshQuota()
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        if (_uiState.value.isAuthLoading) return
        if (!connectivityMonitor.isCurrentlyOnline()) {
            _uiState.update { it.copy(authError = AppError.Offline, isOnline = false) }
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authError = null) }
        viewModelScope.launch {
            when (val result = authRepository.signInWithGoogle(activityContext)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            currentUser = result.user,
                            isAuthLoading = false,
                            authError = null,
                            selectedTab = MainTab.HOME
                        )
                    }
                    refreshQuota()
                }
                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isAuthLoading = false,
                            authError = result.error
                        )
                    }
                }
            }
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            inFlightFlag = false
            _uiState.update { state ->
                SnCreatorUiState(
                    isSplashVisible = false,
                    currentUser = null,
                    isOnline = connectivityMonitor.isCurrentlyOnline(),
                    preferredOutputLanguage = state.preferredOutputLanguage,
                    createOutputLanguage = state.preferredOutputLanguage
                )
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                isShowingResultScreen = false,
                generationError = null
            )
        }
    }

    fun quickStartWithGame(game: SupportedGame) {
        _uiState.update {
            it.copy(
                selectedGame = game,
                selectedTab = MainTab.CREATE,
                isShowingResultScreen = false,
                generationError = null
            )
        }
    }

    fun quickStartWithTool(tool: AiToolType) {
        _uiState.update {
            it.copy(
                selectedTool = tool,
                selectedTab = MainTab.CREATE,
                isShowingResultScreen = false,
                generationError = null
            )
        }
    }

    fun selectGame(game: SupportedGame) {
        _uiState.update {
            it.copy(
                selectedGame = game,
                generationError = null
            )
        }
    }

    fun updateCustomGameName(name: String) {
        _uiState.update {
            it.copy(
                customGameName = name.take(InputValidator.MAX_CUSTOM_GAME_LENGTH),
                generationError = null
            )
        }
    }

    fun updateGameSearchQuery(query: String) {
        _uiState.update { it.copy(gameSearchQuery = query) }
    }

    fun selectTool(tool: AiToolType) {
        _uiState.update {
            it.copy(
                selectedTool = tool,
                generationError = null
            )
        }
    }

    fun updateTopicInput(topic: String) {
        _uiState.update {
            it.copy(
                topicInput = topic.take(InputValidator.MAX_TOPIC_LENGTH),
                generationError = null
            )
        }
    }

    fun updatePlatformHint(platform: String) {
        _uiState.update {
            it.copy(platformHint = InputValidator.sanitizeOptionalHint(platform))
        }
    }

    fun updateAspectRatioHint(aspectRatio: String) {
        _uiState.update {
            it.copy(aspectRatioHint = InputValidator.sanitizeOptionalHint(aspectRatio))
        }
    }

    fun selectCreateOutputLanguage(language: OutputLanguage) {
        _uiState.update { it.copy(createOutputLanguage = language, generationError = null) }
    }

    /**
     * Updates default generated-content language preference in Settings.
     * Does NOT alter the English app interface language.
     */
    fun updatePreferredOutputLanguage(language: OutputLanguage) {
        viewModelScope.launch {
            preferencesRepository.setPreferredOutputLanguage(language)
        }
    }

    fun selectVoiceOverDuration(duration: VoiceOverDuration) {
        _uiState.update { it.copy(voiceOverDuration = duration, generationError = null) }
    }

    fun closeResultScreen() {
        _uiState.update {
            it.copy(
                isShowingResultScreen = false,
                resultError = null
            )
        }
    }

    fun refreshQuota() {
        if (_uiState.value.currentUser == null) return
        if (!connectivityMonitor.isCurrentlyOnline()) {
            _uiState.update {
                it.copy(
                    isOnline = false,
                    isQuotaLoading = false,
                    quotaError = AppError.Offline,
                    lastRetryableAction = RetryableAction.RefreshQuota
                )
            }
            return
        }

        _uiState.update { it.copy(isQuotaLoading = true, quotaError = null) }
        viewModelScope.launch {
            when (val outcome = contentRepository.fetchDailyQuota()) {
                is ApiOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            quota = outcome.data,
                            isQuotaLoading = false,
                            quotaError = null
                        )
                    }
                }
                is ApiOutcome.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            quota = outcome.latestQuota ?: state.quota,
                            isQuotaLoading = false,
                            quotaError = outcome.error,
                            lastRetryableAction = RetryableAction.RefreshQuota
                        )
                    }
                    if (outcome.error is AppError.AuthExpiredOrInvalid) {
                        signOut()
                    }
                }
            }
        }
    }

    fun generateFromCreateForm() {
        if (inFlightFlag || _uiState.value.isGenerating) return

        val snapshot = _uiState.value
        if (!connectivityMonitor.isCurrentlyOnline()) {
            _uiState.update {
                it.copy(
                    isOnline = false,
                    generationError = AppError.Offline,
                    lastRetryableAction = RetryableAction.Generate
                )
            }
            return
        }

        if (snapshot.isQuotaExhausted) {
            _uiState.update {
                it.copy(
                    generationError = AppError.DailyQuotaExhausted(snapshot.formattedResetTime),
                    lastRetryableAction = null
                )
            }
            return
        }

        val resolvedGameResult = InputValidator.resolveGameName(
            game = snapshot.selectedGame,
            customGameName = snapshot.customGameName
        )
        if (resolvedGameResult.isFailure) {
            _uiState.update {
                it.copy(
                    generationError = AppError.InvalidInput(
                        resolvedGameResult.exceptionOrNull()?.message ?: "Please enter a valid game name."
                    )
                )
            }
            return
        }

        val validatedTopicResult = InputValidator.validateTopic(snapshot.topicInput)
        if (validatedTopicResult.isFailure) {
            _uiState.update {
                it.copy(
                    generationError = AppError.InvalidInput(
                        validatedTopicResult.exceptionOrNull()?.message ?: "Please enter a valid topic."
                    )
                )
            }
            return
        }

        val resolvedGameName = resolvedGameResult.getOrThrow()
        val cleanTopic = validatedTopicResult.getOrThrow()
        val duration = if (snapshot.selectedTool == AiToolType.VOICEOVER_SCRIPT) {
            snapshot.voiceOverDuration
        } else {
            null
        }

        inFlightFlag = true
        _uiState.update {
            it.copy(
                isGenerating = true,
                generationError = null,
                resultError = null,
                lastRetryableAction = RetryableAction.Generate
            )
        }

        viewModelScope.launch {
            try {
                val outcome = contentRepository.generateContent(
                    tool = snapshot.selectedTool,
                    resolvedGameName = resolvedGameName,
                    topic = cleanTopic,
                    language = snapshot.createOutputLanguage,
                    duration = duration,
                    platformHint = if (snapshot.selectedTool == AiToolType.HASHTAGS) snapshot.platformHint else "",
                    aspectRatio = if (snapshot.selectedTool == AiToolType.THUMBNAIL_PROMPT) snapshot.aspectRatioHint else "",
                    requestId = UUID.randomUUID().toString()
                )

                when (outcome) {
                    is ApiOutcome.Success -> {
                        val active = ActiveResultSession(
                            tool = snapshot.selectedTool,
                            selectedGame = snapshot.selectedGame,
                            resolvedGameName = resolvedGameName,
                            topic = cleanTopic,
                            language = snapshot.createOutputLanguage,
                            duration = duration,
                            platformHint = snapshot.platformHint,
                            aspectRatio = snapshot.aspectRatioHint,
                            content = outcome.data.content
                        )
                        _uiState.update {
                            it.copy(
                                isGenerating = false,
                                quota = outcome.data.quota,
                                activeResult = active,
                                isShowingResultScreen = true,
                                generationError = null,
                                resultError = null,
                                lastRetryableAction = null
                            )
                        }
                    }
                    is ApiOutcome.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                quota = outcome.latestQuota ?: state.quota,
                                generationError = outcome.error
                            )
                        }
                        if (outcome.error is AppError.AuthExpiredOrInvalid) {
                            signOut()
                        }
                    }
                }
            } finally {
                inFlightFlag = false
            }
        }
    }

    /**
     * Regenerates a brand-new AI response for the active result session.
     * Counts as 1 generation on success; failed requests never reduce quota.
     */
    fun regenerateActiveResult() {
        if (inFlightFlag || _uiState.value.isGenerating) return
        val session = _uiState.value.activeResult ?: return

        if (!connectivityMonitor.isCurrentlyOnline()) {
            _uiState.update {
                it.copy(
                    isOnline = false,
                    resultError = AppError.Offline,
                    lastRetryableAction = RetryableAction.Regenerate
                )
            }
            return
        }

        if (_uiState.value.isQuotaExhausted) {
            _uiState.update {
                it.copy(
                    resultError = AppError.DailyQuotaExhausted(_uiState.value.formattedResetTime),
                    lastRetryableAction = null
                )
            }
            return
        }

        inFlightFlag = true
        _uiState.update {
            it.copy(
                isGenerating = true,
                resultError = null,
                lastRetryableAction = RetryableAction.Regenerate
            )
        }

        viewModelScope.launch {
            try {
                val outcome = contentRepository.generateContent(
                    tool = session.tool,
                    resolvedGameName = session.resolvedGameName,
                    topic = session.topic,
                    language = session.language,
                    duration = session.duration,
                    platformHint = if (session.tool == AiToolType.HASHTAGS) session.platformHint else "",
                    aspectRatio = if (session.tool == AiToolType.THUMBNAIL_PROMPT) session.aspectRatio else "",
                    requestId = UUID.randomUUID().toString()
                )

                when (outcome) {
                    is ApiOutcome.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                quota = outcome.data.quota,
                                activeResult = session.copy(content = outcome.data.content),
                                resultError = null,
                                lastRetryableAction = null
                            )
                        }
                    }
                    is ApiOutcome.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                quota = outcome.latestQuota ?: state.quota,
                                resultError = outcome.error
                            )
                        }
                        if (outcome.error is AppError.AuthExpiredOrInvalid) {
                            signOut()
                        }
                    }
                }
            } finally {
                inFlightFlag = false
            }
        }
    }

    /**
     * Requests a genuine AI translation of the active result between English and Roman Urdu.
     * Counts as 1 generation on success; failed requests never reduce quota.
     */
    fun switchResultLanguage() {
        if (inFlightFlag || _uiState.value.isGenerating) return
        val session = _uiState.value.activeResult ?: return
        val targetLang = session.language.opposite()

        if (!connectivityMonitor.isCurrentlyOnline()) {
            _uiState.update {
                it.copy(
                    isOnline = false,
                    resultError = AppError.Offline,
                    lastRetryableAction = RetryableAction.SwitchLanguage(targetLang)
                )
            }
            return
        }

        if (_uiState.value.isQuotaExhausted) {
            _uiState.update {
                it.copy(
                    resultError = AppError.DailyQuotaExhausted(_uiState.value.formattedResetTime),
                    lastRetryableAction = null
                )
            }
            return
        }

        inFlightFlag = true
        _uiState.update {
            it.copy(
                isGenerating = true,
                resultError = null,
                lastRetryableAction = RetryableAction.SwitchLanguage(targetLang)
            )
        }

        viewModelScope.launch {
            try {
                val outcome = contentRepository.translateContent(
                    tool = session.tool,
                    resolvedGameName = session.resolvedGameName,
                    currentContent = session.content,
                    targetLanguage = targetLang,
                    duration = session.duration,
                    requestId = UUID.randomUUID().toString()
                )

                when (outcome) {
                    is ApiOutcome.Success -> {
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                quota = outcome.data.quota,
                                activeResult = session.copy(
                                    language = targetLang,
                                    content = outcome.data.content
                                ),
                                resultError = null,
                                lastRetryableAction = null
                            )
                        }
                    }
                    is ApiOutcome.Error -> {
                        _uiState.update { state ->
                            state.copy(
                                isGenerating = false,
                                quota = outcome.latestQuota ?: state.quota,
                                resultError = outcome.error
                            )
                        }
                        if (outcome.error is AppError.AuthExpiredOrInvalid) {
                            signOut()
                        }
                    }
                }
            } finally {
                inFlightFlag = false
            }
        }
    }

    fun retryLastAction() {
        when (val action = _uiState.value.lastRetryableAction) {
            is RetryableAction.RefreshQuota -> refreshQuota()
            is RetryableAction.Generate -> generateFromCreateForm()
            is RetryableAction.Regenerate -> regenerateActiveResult()
            is RetryableAction.SwitchLanguage -> {
                val current = _uiState.value.activeResult
                if (current != null && current.language != action.targetLanguage) {
                    switchResultLanguage()
                }
            }
            null -> {
                if (_uiState.value.isShowingResultScreen) {
                    regenerateActiveResult()
                } else {
                    generateFromCreateForm()
                }
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val contentRepository: ContentGenerationRepository,
        private val preferencesRepository: PreferencesRepository,
        private val connectivityMonitor: ConnectivityMonitor
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SnCreatorViewModel(
                authRepository = authRepository,
                contentRepository = contentRepository,
                preferencesRepository = preferencesRepository,
                connectivityMonitor = connectivityMonitor
            ) as T
        }
    }
}
