package com.sncreator.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sncreator.app.ui.screens.CreateScreen
import com.sncreator.app.ui.screens.HelpScreen
import com.sncreator.app.ui.screens.HomeScreen
import com.sncreator.app.ui.screens.LoginScreen
import com.sncreator.app.ui.screens.ResultScreen
import com.sncreator.app.ui.screens.SettingsScreen
import com.sncreator.app.ui.screens.SplashScreen
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.DeepRoyalViolet
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.ObsidianSurface
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.ui.theme.VoidBlack
import com.sncreator.app.viewmodel.MainTab
import com.sncreator.app.viewmodel.SnCreatorViewModel

private enum class RootScreenStage {
    SPLASH,
    LOGIN,
    AUTHENTICATED
}

@Composable
fun SnCreatorApp(
    viewModel: SnCreatorViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val stage = when {
        uiState.isSplashVisible -> RootScreenStage.SPLASH
        uiState.currentUser == null -> RootScreenStage.LOGIN
        else -> RootScreenStage.AUTHENTICATED
    }

    Crossfade(
        targetState = stage,
        animationSpec = tween(durationMillis = 320),
        label = "root_navigation_transition"
    ) { currentStage ->
        when (currentStage) {
            RootScreenStage.SPLASH -> {
                SplashScreen(
                    onSplashFinished = viewModel::onSplashFinished
                )
            }

            RootScreenStage.LOGIN -> {
                LoginScreen(
                    isLoading = uiState.isAuthLoading,
                    authError = uiState.authError,
                    onContinueWithGoogle = viewModel::signInWithGoogle
                )
            }

            RootScreenStage.AUTHENTICATED -> {
                val activeResult = uiState.activeResult
                if (uiState.isShowingResultScreen && activeResult != null) {
                    BackHandler(enabled = !uiState.isGenerating) {
                        viewModel.closeResultScreen()
                    }
                    Scaffold(containerColor = VoidBlack) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            ResultScreen(
                                session = activeResult,
                                quota = uiState.quota,
                                formattedResetTime = uiState.formattedResetTime,
                                isLoading = uiState.isGenerating,
                                isOnline = uiState.isOnline,
                                resultError = uiState.resultError,
                                onBack = viewModel::closeResultScreen,
                                onRegenerate = viewModel::regenerateActiveResult,
                                onSwitchLanguage = viewModel::switchResultLanguage,
                                onRetry = viewModel::retryLastAction
                            )
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = VoidBlack,
                        bottomBar = {
                            NavigationBar(
                                containerColor = ObsidianSurface,
                                tonalElevation = 0.dp
                            ) {
                                MainTab.entries.forEach { tab ->
                                    val selected = uiState.selectedTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { viewModel.selectTab(tab) },
                                        icon = {
                                            val icon = when (tab) {
                                                MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                                MainTab.CREATE -> if (selected) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline
                                                MainTab.GET_HELP -> if (selected) Icons.Filled.Help else Icons.Outlined.HelpOutline
                                                MainTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
                                            }
                                            Icon(imageVector = icon, contentDescription = tab.label)
                                        },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = CrispWhite,
                                            selectedTextColor = SoftLavenderGlow,
                                            indicatorColor = DeepRoyalViolet,
                                            unselectedIconColor = MutedSlate,
                                            unselectedTextColor = MutedSlate
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Crossfade(
                                targetState = uiState.selectedTab,
                                animationSpec = tween(durationMillis = 220),
                                label = "bottom_tab_transition"
                            ) { activeTab ->
                                when (activeTab) {
                                    MainTab.HOME -> HomeScreen(
                                        uiState = uiState,
                                        onCreateContentClick = { viewModel.selectTab(MainTab.CREATE) },
                                        onSelectGameQuick = viewModel::quickStartWithGame,
                                        onSelectToolQuick = viewModel::quickStartWithTool,
                                        onOpenHelpClick = { viewModel.selectTab(MainTab.GET_HELP) },
                                        onRefreshQuota = viewModel::refreshQuota
                                    )

                                    MainTab.CREATE -> CreateScreen(
                                        uiState = uiState,
                                        onSelectGame = viewModel::selectGame,
                                        onUpdateCustomGame = viewModel::updateCustomGameName,
                                        onUpdateGameSearch = viewModel::updateGameSearchQuery,
                                        onSelectTool = viewModel::selectTool,
                                        onUpdateTopic = viewModel::updateTopicInput,
                                        onUpdatePlatformHint = viewModel::updatePlatformHint,
                                        onUpdateAspectRatioHint = viewModel::updateAspectRatioHint,
                                        onSelectLanguage = viewModel::selectCreateOutputLanguage,
                                        onSelectDuration = viewModel::selectVoiceOverDuration,
                                        onGenerateClick = viewModel::generateFromCreateForm,
                                        onRetryClick = viewModel::retryLastAction
                                    )

                                    MainTab.GET_HELP -> HelpScreen()

                                    MainTab.SETTINGS -> SettingsScreen(
                                        user = uiState.currentUser,
                                        preferredOutputLanguage = uiState.preferredOutputLanguage,
                                        onChangePreferredLanguage = viewModel::updatePreferredOutputLanguage,
                                        onSignOut = viewModel::signOut
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
