package com.sncreator.app

import android.content.Context
import com.sncreator.app.data.ApiOutcome
import com.sncreator.app.data.AuthRepository
import com.sncreator.app.data.AuthResult
import com.sncreator.app.data.ConnectivityMonitor
import com.sncreator.app.data.ContentGenerationRepository
import com.sncreator.app.data.NetworkContentGenerationRepository
import com.sncreator.app.data.PreferencesRepository
import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.AppError
import com.sncreator.app.model.AuthenticatedUser
import com.sncreator.app.model.GenerateApiResponse
import com.sncreator.app.model.InputValidator
import com.sncreator.app.model.OutputLanguage
import com.sncreator.app.model.QuotaDto
import com.sncreator.app.model.SupportedGame
import com.sncreator.app.model.VoiceOverDuration
import com.sncreator.app.util.PakistanTime
import com.sncreator.app.util.SupportLinks
import com.sncreator.app.viewmodel.MainTab
import com.sncreator.app.viewmodel.SnCreatorViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SnCreatorUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- Fakes for MVVM & Repository Isolation ---

    private class FakeConnectivityMonitor(initialOnline: Boolean = true) : ConnectivityMonitor {
        val state = MutableStateFlow(initialOnline)
        override val isOnlineFlow: Flow<Boolean> = state
        override fun isCurrentlyOnline(): Boolean = state.value
    }

    private class FakePreferencesRepository(
        initial: OutputLanguage = OutputLanguage.ENGLISH
    ) : PreferencesRepository {
        val langFlow = MutableStateFlow(initial)
        override val preferredOutputLanguage: Flow<OutputLanguage> = langFlow
        override suspend fun setPreferredOutputLanguage(language: OutputLanguage) {
            langFlow.value = language
        }
    }

    private class FakeAuthRepository(
        initialUser: AuthenticatedUser? = null
    ) : AuthRepository {
        val userFlow = MutableStateFlow(initialUser)
        var nextSignInResult: AuthResult = AuthResult.Success(
            AuthenticatedUser("uid_123", "Ali Gaming", "ali@example.com")
        )
        var tokenResult: Result<String> = Result.success("verified-firebase-jwt-token")

        override val authStateFlow: Flow<AuthenticatedUser?> = userFlow
        override fun getCurrentUser(): AuthenticatedUser? = userFlow.value

        override suspend fun signInWithGoogle(activityContext: Context): AuthResult {
            val res = nextSignInResult
            if (res is AuthResult.Success) {
                userFlow.value = res.user
            }
            return res
        }

        override suspend fun getValidIdToken(forceRefresh: Boolean): Result<String> = tokenResult

        override suspend fun signOut() {
            userFlow.value = null
        }
    }

    private class FakeContentRepository(
        override val isBackendConfigured: Boolean = true
    ) : ContentGenerationRepository {
        var remainingQuota: Int = 10
        var nextGenerateOutcome: ((AiToolType, String, String, OutputLanguage, VoiceOverDuration?) -> ApiOutcome<GenerateApiResponse>)? = null
        var nextTranslateOutcome: ((OutputLanguage) -> ApiOutcome<GenerateApiResponse>)? = null
        var recordedGenerateCalls = mutableListOf<RecordedCall>()
        var gateDeferred: CompletableDeferred<Unit>? = null

        data class RecordedCall(
            val tool: AiToolType,
            val game: String,
            val topic: String,
            val language: OutputLanguage,
            val duration: VoiceOverDuration?
        )

        override suspend fun fetchDailyQuota(): ApiOutcome<QuotaDto> {
            return ApiOutcome.Success(
                QuotaDto(
                    limit = 10,
                    used = 10 - remainingQuota,
                    remaining = remainingQuota,
                    pktDate = "2026-10-25",
                    resetsAtIso = "2026-10-25T19:00:00Z"
                )
            )
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
            recordedGenerateCalls.add(RecordedCall(tool, resolvedGameName, topic, language, duration))
            gateDeferred?.await()
            val custom = nextGenerateOutcome
            if (custom != null) {
                return custom(tool, resolvedGameName, topic, language, duration)
            }
            if (remainingQuota <= 0) {
                return ApiOutcome.Error(
                    AppError.DailyQuotaExhausted("Oct 26, 12:00 AM PKT"),
                    QuotaDto(limit = 10, used = 10, remaining = 0)
                )
            }
            remainingQuota -= 1
            return ApiOutcome.Success(
                GenerateApiResponse(
                    content = "Generated ${tool.apiValue} for $resolvedGameName in ${language.apiValue}",
                    tool = tool.apiValue,
                    game = resolvedGameName,
                    language = language.apiValue,
                    duration = duration?.apiValue,
                    model = "gemini-3.8-flash",
                    quota = QuotaDto(
                        limit = 10,
                        used = 10 - remainingQuota,
                        remaining = remainingQuota,
                        pktDate = "2026-10-25",
                        resetsAtIso = "2026-10-25T19:00:00Z"
                    )
                )
            )
        }

        override suspend fun translateContent(
            tool: AiToolType,
            resolvedGameName: String,
            currentContent: String,
            targetLanguage: OutputLanguage,
            duration: VoiceOverDuration?,
            requestId: String
        ): ApiOutcome<GenerateApiResponse> {
            val custom = nextTranslateOutcome
            if (custom != null) return custom(targetLanguage)
            if (remainingQuota <= 0) {
                return ApiOutcome.Error(
                    AppError.DailyQuotaExhausted("Oct 26, 12:00 AM PKT"),
                    QuotaDto(limit = 10, used = 10, remaining = 0)
                )
            }
            remainingQuota -= 1
            return ApiOutcome.Success(
                GenerateApiResponse(
                    content = "Translated [${targetLanguage.apiValue}]: $currentContent",
                    tool = tool.apiValue,
                    game = resolvedGameName,
                    language = targetLanguage.apiValue,
                    duration = duration?.apiValue,
                    model = "gemini-3.8-flash",
                    quota = QuotaDto(
                        limit = 10,
                        used = 10 - remainingQuota,
                        remaining = remainingQuota,
                        pktDate = "2026-10-25",
                        resetsAtIso = "2026-10-25T19:00:00Z"
                    )
                )
            )
        }
    }

    @Test
    fun `launch splash transition and bottom tab navigation work properly`() = runTest {
        val user = AuthenticatedUser("u1", "Hamza Plays", "hamza@example.com")
        val vm = SnCreatorViewModel(
            authRepository = FakeAuthRepository(user),
            contentRepository = FakeContentRepository(),
            preferencesRepository = FakePreferencesRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true)
        )
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSplashVisible)
        vm.onSplashFinished()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSplashVisible)
        assertEquals("Hamza Plays", vm.uiState.value.currentUser?.greetingName)
        assertEquals(MainTab.HOME, vm.uiState.value.selectedTab)

        vm.selectTab(MainTab.CREATE)
        assertEquals(MainTab.CREATE, vm.uiState.value.selectedTab)
        vm.selectTab(MainTab.GET_HELP)
        assertEquals(MainTab.GET_HELP, vm.uiState.value.selectedTab)
        vm.selectTab(MainTab.SETTINGS)
        assertEquals(MainTab.SETTINGS, vm.uiState.value.selectedTab)
    }

    @Test
    fun `google sign in cancellation and error surface clean retryable errors and sign out clears session`() = runTest {
        val authRepo = FakeAuthRepository(null)
        authRepo.nextSignInResult = AuthResult.Failure(AppError.AuthCancelled)
        val dummyContext = android.content.ContextWrapper(null)

        val vm = SnCreatorViewModel(
            authRepository = authRepo,
            contentRepository = FakeContentRepository(),
            preferencesRepository = FakePreferencesRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true)
        )
        vm.onSplashFinished()
        vm.signInWithGoogle(dummyContext)
        advanceUntilIdle()

        assertNull(vm.uiState.value.currentUser)
        assertEquals(AppError.AuthCancelled, vm.uiState.value.authError)
        assertTrue(vm.uiState.value.authError?.canRetry == true)

        // Now succeed on retry
        authRepo.nextSignInResult = AuthResult.Success(
            AuthenticatedUser("uid_99", "Zainab YT", "zainab@example.com")
        )
        vm.signInWithGoogle(dummyContext)
        advanceUntilIdle()

        assertEquals("Zainab YT", vm.uiState.value.currentUser?.greetingName)
        assertNull(vm.uiState.value.authError)

        // Sign out clears user and active result
        vm.signOut()
        advanceUntilIdle()
        assertNull(vm.uiState.value.currentUser)
        assertNull(vm.uiState.value.activeResult)
    }

    @Test
    fun `game search and custom game validation enforce required name for Other Game`() {
        val matches = SupportedGame.filterByQuery("Asylum")
        assertTrue(matches.contains(SupportedGame.LAST_ASYLUM_PLAGUE))
        assertTrue(matches.contains(SupportedGame.OTHER_GAME))

        val missingCustom = InputValidator.resolveGameName(SupportedGame.OTHER_GAME, " ")
        assertTrue(missingCustom.isFailure)

        val validCustom = InputValidator.resolveGameName(SupportedGame.OTHER_GAME, "Valorant Mobile")
        assertEquals("Valorant Mobile", validCustom.getOrNull())

        val standardGame = InputValidator.resolveGameName(SupportedGame.PUBG_MOBILE, "")
        assertEquals("PUBG Mobile", standardGame.getOrNull())
    }

    @Test
    fun `all five AI tools and all six voice-over duration presets generate accurately`() = runTest {
        val contentRepo = FakeContentRepository()
        contentRepo.remainingQuota = 10
        val vm = SnCreatorViewModel(
            authRepository = FakeAuthRepository(AuthenticatedUser("u1", "Creator", "c@example.com")),
            contentRepository = contentRepo,
            preferencesRepository = FakePreferencesRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true)
        )
        advanceUntilIdle()

        vm.updateTopicInput("Aggressive squad wipe tips in ranked mode")

        // Test non-voiceover tools (duration must be null)
        listOf(
            AiToolType.GAMING_TITLES,
            AiToolType.HASHTAGS,
            AiToolType.THUMBNAIL_PROMPT,
            AiToolType.VIDEO_IDEAS
        ).forEach { tool ->
            vm.selectTool(tool)
            vm.generateFromCreateForm()
            advanceUntilIdle()
            val lastCall = contentRepo.recordedGenerateCalls.last()
            assertEquals(tool, lastCall.tool)
            assertNull(lastCall.duration)
            assertTrue(vm.uiState.value.isShowingResultScreen)
            vm.closeResultScreen()
        }

        // Reset fake repo quota to test all 6 VoiceOverDuration presets
        contentRepo.remainingQuota = 10
        vm.refreshQuota()
        advanceUntilIdle()

        vm.selectTool(AiToolType.VOICEOVER_SCRIPT)
        VoiceOverDuration.entries.forEach { preset ->
            vm.selectVoiceOverDuration(preset)
            vm.generateFromCreateForm()
            advanceUntilIdle()
            val lastCall = contentRepo.recordedGenerateCalls.last()
            assertEquals(AiToolType.VOICEOVER_SCRIPT, lastCall.tool)
            assertEquals(preset, lastCall.duration)
        }
    }

    @Test
    fun `roman urdu selection, regenerate, and switch language count against 10 daily quota and failed requests restore quota`() = runTest {
        val contentRepo = FakeContentRepository()
        contentRepo.remainingQuota = 2
        val prefsRepo = FakePreferencesRepository(OutputLanguage.ENGLISH)

        val vm = SnCreatorViewModel(
            authRepository = FakeAuthRepository(AuthenticatedUser("u1", "Saad", "saad@example.com")),
            contentRepository = contentRepo,
            preferencesRepository = prefsRepo,
            connectivityMonitor = FakeConnectivityMonitor(true)
        )
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.quota.remaining)

        // Switch preferred output language to Roman Urdu
        vm.updatePreferredOutputLanguage(OutputLanguage.ROMAN_URDU)
        advanceUntilIdle()
        assertEquals(OutputLanguage.ROMAN_URDU, vm.uiState.value.createOutputLanguage)

        vm.selectGame(SupportedGame.FREE_FIRE)
        vm.updateTopicInput("One tap headshot sensitivity guide")
        vm.generateFromCreateForm()
        advanceUntilIdle()

        // 1st generation used -> 1 remaining
        assertEquals(1, vm.uiState.value.quota.remaining)
        assertEquals(OutputLanguage.ROMAN_URDU, vm.uiState.value.activeResult?.language)

        // Simulate failed upstream call: quota MUST remain 1 and active content MUST remain intact
        val previousContent = vm.uiState.value.activeResult?.content
        contentRepo.nextGenerateOutcome = { _, _, _, _, _ ->
            ApiOutcome.Error(AppError.RequestTimeout)
        }
        vm.regenerateActiveResult()
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.quota.remaining)
        assertEquals(previousContent, vm.uiState.value.activeResult?.content)
        assertEquals(AppError.RequestTimeout, vm.uiState.value.resultError)
        contentRepo.nextGenerateOutcome = null

        // Switch language to English -> uses last remaining generation (0 remaining)
        vm.switchResultLanguage()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.quota.remaining)
        assertEquals(OutputLanguage.ENGLISH, vm.uiState.value.activeResult?.language)
        assertTrue(vm.uiState.value.isQuotaExhausted)

        // Attempting another regenerate at 0 remaining is blocked with reset explanation
        vm.regenerateActiveResult()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resultError is AppError.DailyQuotaExhausted)
    }

    @Test
    fun `pakistan time Asia_Karachi date boundary resets accurately at 19_00 UTC`() {
        // 2026-10-25 18:59:59 UTC == 2026-10-25 23:59:59 PKT (still Oct 25 in Pakistan)
        val beforeMidnightPkt = Instant.parse("2026-10-25T18:59:59Z")
        assertEquals("2026-10-25", PakistanTime.currentPktDateString(beforeMidnightPkt))

        // 2026-10-25 19:00:01 UTC == 2026-10-26 00:00:01 PKT (new Pakistan calendar day Oct 26!)
        val afterMidnightPkt = Instant.parse("2026-10-25T19:00:01Z")
        assertEquals("2026-10-26", PakistanTime.currentPktDateString(afterMidnightPkt))

        val nextMidnight = PakistanTime.nextMidnightPkt(beforeMidnightPkt)
        assertEquals(Instant.parse("2026-10-25T19:00:00Z"), nextMidnight.toInstant())
    }

    @Test
    fun `duplicate tap protection prevents simultaneous requests`() = runTest {
        val contentRepo = FakeContentRepository()
        val gate = CompletableDeferred<Unit>()
        contentRepo.gateDeferred = gate

        val vm = SnCreatorViewModel(
            authRepository = FakeAuthRepository(AuthenticatedUser("u1", "Bilal", "b@example.com")),
            contentRepository = contentRepo,
            preferencesRepository = FakePreferencesRepository(),
            connectivityMonitor = FakeConnectivityMonitor(true)
        )
        advanceUntilIdle()

        vm.updateTopicInput("Gangstar Mirage City fastest car unlock")
        vm.generateFromCreateForm()
        // Immediate duplicate tap while first request is still suspended
        vm.generateFromCreateForm()
        vm.generateFromCreateForm()

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, contentRepo.recordedGenerateCalls.size)
        assertEquals(9, vm.uiState.value.quota.remaining)
    }

    @Test
    fun `offline detection shows exact required message and retry succeeds once online`() = runTest {
        val connectivity = FakeConnectivityMonitor(initialOnline = false)
        val contentRepo = FakeContentRepository()
        val vm = SnCreatorViewModel(
            authRepository = FakeAuthRepository(AuthenticatedUser("u1", "Usman", "u@example.com")),
            contentRepository = contentRepo,
            preferencesRepository = FakePreferencesRepository(),
            connectivityMonitor = connectivity
        )
        advanceUntilIdle()

        vm.updateTopicInput("Last Asylum: Plague base defense layout")
        vm.generateFromCreateForm()
        advanceUntilIdle()

        assertEquals(AppError.Offline, vm.uiState.value.generationError)
        assertEquals(
            "Internet connection required for AI generation.",
            vm.uiState.value.generationError?.userMessage
        )
        assertEquals(0, contentRepo.recordedGenerateCalls.size)

        // Restore connectivity and invoke Retry
        connectivity.state.value = true
        advanceUntilIdle()
        vm.retryLastAction()
        advanceUntilIdle()

        assertEquals(1, contentRepo.recordedGenerateCalls.size)
        assertNotNull(vm.uiState.value.activeResult)
    }

    @Test
    fun `whatsapp support link and contact placeholder behave accurately`() {
        val helpUrl = SupportLinks.buildGetHelpWhatsAppUrl()
        assertEquals(
            "https://wa.me/923288501635?text=Hello%20SN%20Creator%20team%2C%20I%20need%20help%20with%20the%20app.",
            helpUrl
        )

        // Contact SN Creator must return null when unconfigured or placeholder (never invent a phone number)
        assertNull(SupportLinks.resolveContactSnCreatorUrl(""))
        assertNull(SupportLinks.resolveContactSnCreatorUrl(SupportLinks.UNCONFIGURED_CONTACT_PLACEHOLDER))
        assertNull(SupportLinks.resolveContactSnCreatorUrl("TBD_LATER"))

        // Returns valid wa.me link once owner configures a real number
        assertEquals(
            "https://wa.me/923001234567",
            SupportLinks.resolveContactSnCreatorUrl("+92 300 1234567")
        )
    }

    @Test
    fun `network repository handles missing backend config and MockWebServer HTTP responses`() = runTest {
        val auth = FakeAuthRepository(AuthenticatedUser("u1", "Test", "t@example.com"))
        val online = FakeConnectivityMonitor(true)

        val unconfiguredRepo = NetworkContentGenerationRepository(
            backendBaseUrl = "",
            authRepository = auth,
            connectivityMonitor = online,
            ioDispatcher = testDispatcher
        )
        assertFalse(unconfiguredRepo.isBackendConfigured)
        val missingOutcome = unconfiguredRepo.generateContent(
            tool = AiToolType.GAMING_TITLES,
            resolvedGameName = "PUBG Mobile",
            topic = "Erangel bridge camp strategy",
            language = OutputLanguage.ENGLISH,
            duration = null
        )
        assertTrue(missingOutcome is ApiOutcome.Error && (missingOutcome.error is AppError.BackendMisconfigured))

        val server = MockWebServer()
        server.start()
        try {
            val configuredRepo = NetworkContentGenerationRepository(
                backendBaseUrl = server.url("/").toString(),
                authRepository = auth,
                connectivityMonitor = online,
                ioDispatcher = testDispatcher
            )
            assertTrue(configuredRepo.isBackendConfigured)

            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
                        """
                        {
                          "content": "1. Zero Recoil M416 Guide",
                          "tool": "GAMING_TITLES",
                          "game": "PUBG Mobile",
                          "language": "ENGLISH",
                          "model": "gemini-3.8-flash",
                          "quota": {
                            "limit": 10,
                            "used": 3,
                            "remaining": 7,
                            "pktDate": "2026-10-25",
                            "resetsAtIso": "2026-10-25T19:00:00Z"
                          }
                        }
                        """.trimIndent()
                    )
            )

            val okOutcome = configuredRepo.generateContent(
                tool = AiToolType.GAMING_TITLES,
                resolvedGameName = "PUBG Mobile",
                topic = "M416 zero recoil spray",
                language = OutputLanguage.ENGLISH,
                duration = null,
                requestId = "req-fixed-1"
            )
            assertTrue(okOutcome is ApiOutcome.Success)
            val recordedReq = server.takeRequest()
            assertEquals("Bearer verified-firebase-jwt-token", recordedReq.getHeader("Authorization"))
            assertEquals("req-fixed-1", recordedReq.getHeader("X-Request-Id"))
            assertEquals("/v1/generate", recordedReq.path)
        } finally {
            server.shutdown()
        }
    }
}
