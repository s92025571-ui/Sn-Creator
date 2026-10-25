package com.sncreator.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.AppError
import com.sncreator.app.model.InputValidator
import com.sncreator.app.model.OutputLanguage
import com.sncreator.app.model.SupportedGame
import com.sncreator.app.model.VoiceOverDuration
import com.sncreator.app.ui.components.ErrorAndOfflineBanner
import com.sncreator.app.ui.components.GlassSurfaceCard
import com.sncreator.app.ui.components.NeonPrimaryButton
import com.sncreator.app.ui.components.ToolBadgeIcon
import com.sncreator.app.ui.theme.CrimsonError
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.DeepRoyalViolet
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.ElevatedCardBg
import com.sncreator.app.ui.theme.GlassBorderViolet
import com.sncreator.app.ui.theme.GlassCardBg
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.ObsidianSurface
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.viewmodel.SnCreatorUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    uiState: SnCreatorUiState,
    onSelectGame: (SupportedGame) -> Unit,
    onUpdateCustomGame: (String) -> Unit,
    onUpdateGameSearch: (String) -> Unit,
    onSelectTool: (AiToolType) -> Unit,
    onUpdateTopic: (String) -> Unit,
    onUpdatePlatformHint: (String) -> Unit,
    onUpdateAspectRatioHint: (String) -> Unit,
    onSelectLanguage: (OutputLanguage) -> Unit,
    onSelectDuration: (VoiceOverDuration) -> Unit,
    onGenerateClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    var showGamePickerDialog by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ElectricViolet,
        unfocusedBorderColor = GlassBorderViolet,
        focusedContainerColor = ObsidianSurface,
        unfocusedContainerColor = ObsidianSurface,
        focusedTextColor = CrispWhite,
        unfocusedTextColor = CrispWhite,
        cursorColor = SoftLavenderGlow,
        focusedLabelColor = SoftLavenderGlow,
        unfocusedLabelColor = MutedSlate
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header + Remaining Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create Gaming Content",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrispWhite
                )
                Text(
                    text = "${uiState.quota.remaining}/${uiState.quota.limit} generations left today (PKT)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isQuotaExhausted) CrimsonError else SoftLavenderGlow
                )
            }
        }

        // 1. Game Selector + Conditional Custom Game Name
        GlassSurfaceCard {
            Text(
                text = "1. Select Game",
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !uiState.isGenerating) {
                        onUpdateGameSearch("")
                        showGamePickerDialog = true
                    },
                shape = RoundedCornerShape(14.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, GlassBorderViolet)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SportsEsports,
                            contentDescription = null,
                            tint = SoftLavenderGlow
                        )
                        Column {
                            Text(
                                text = uiState.selectedGame.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite
                            )
                            Text(
                                text = uiState.selectedGame.genreHint,
                                style = MaterialTheme.typography.labelMedium,
                                color = MutedSlate
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Choose game",
                        tint = SoftLavenderGlow
                    )
                }
            }

            if (uiState.selectedGame.requiresCustomName) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.customGameName,
                    onValueChange = onUpdateCustomGame,
                    enabled = !uiState.isGenerating,
                    label = { Text("Custom Game Name (Required)") },
                    placeholder = { Text("e.g., Valorant Mobile, Call of Duty Mobile, Minecraft") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 2. Five AI Tool Cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "2. Choose AI Tool",
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )

            AiToolType.entries.forEach { tool ->
                val isSelected = uiState.selectedTool == tool
                GlassSurfaceCard(
                    highlighted = isSelected,
                    onClick = if (uiState.isGenerating) null else ({ onSelectTool(tool) }),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ToolBadgeIcon(tool = tool, selected = isSelected)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tool.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondarySlate
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Selected",
                                tint = SoftLavenderGlow,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Conditional Voice-Over Duration Selector (ONLY shown for Voice-over Scripts)
        if (uiState.selectedTool == AiToolType.VOICEOVER_SCRIPT) {
            GlassSurfaceCard(highlighted = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = SoftLavenderGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Voice-over Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispWhite
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiState.voiceOverDuration.pacingSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondarySlate
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceOverDuration.entries.forEach { preset ->
                        val selected = uiState.voiceOverDuration == preset
                        FilterChip(
                            selected = selected,
                            onClick = { if (!uiState.isGenerating) onSelectDuration(preset) },
                            label = { Text(preset.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepRoyalViolet,
                                selectedLabelColor = CrispWhite,
                                containerColor = ObsidianSurface,
                                labelColor = SecondarySlate
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlassBorderViolet,
                                selectedBorderColor = ElectricViolet
                            )
                        )
                    }
                }
            }
        }

        // 4. Topic / Context Input (+ tool-specific optional hints)
        GlassSurfaceCard {
            Text(
                text = "3. Topic or Context",
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.topicInput,
                onValueChange = onUpdateTopic,
                enabled = !uiState.isGenerating,
                label = { Text("Describe your gameplay, video idea, or challenge") },
                placeholder = { Text(uiState.selectedTool.topicPlaceholder) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                supportingText = {
                    Text(
                        text = "${uiState.topicInput.length}/${InputValidator.MAX_TOPIC_LENGTH}",
                        color = MutedSlate
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.selectedTool == AiToolType.HASHTAGS) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = uiState.platformHint,
                    onValueChange = onUpdatePlatformHint,
                    enabled = !uiState.isGenerating,
                    label = { Text("Intended Platform") },
                    placeholder = { Text("e.g., YouTube Shorts, TikTok, Instagram Reels") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.selectedTool == AiToolType.THUMBNAIL_PROMPT) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Requested Aspect Ratio (Prompt Text Only — Does Not Render Images)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("16:9", "9:16", "1:1", "4:3").forEach { ratio ->
                        val active = uiState.aspectRatioHint == ratio
                        FilterChip(
                            selected = active,
                            onClick = { if (!uiState.isGenerating) onUpdateAspectRatioHint(ratio) },
                            label = { Text(ratio) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DeepRoyalViolet,
                                selectedLabelColor = CrispWhite,
                                containerColor = ObsidianSurface,
                                labelColor = SecondarySlate
                            )
                        )
                    }
                }
            }
        }

        // 5. Output Language Selector (English vs Roman Urdu)
        GlassSurfaceCard {
            Text(
                text = "4. Output Language",
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Applies only to AI-generated text. App menus remain in English.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutputLanguage.entries.forEach { lang ->
                    val selected = uiState.createOutputLanguage == lang
                    Surface(
                        onClick = { if (!uiState.isGenerating) onSelectLanguage(lang) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) ElevatedCardBg else ObsidianSurface,
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) ElectricViolet else GlassBorderViolet
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (lang == OutputLanguage.ROMAN_URDU) "Latin letters" else "Standard English",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftLavenderGlow
                            )
                        }
                    }
                }
            }
        }

        // Offline / Validation / Quota Errors
        val displayedError = when {
            !uiState.isOnline -> AppError.Offline
            uiState.generationError != null -> uiState.generationError
            uiState.isQuotaExhausted -> AppError.DailyQuotaExhausted(uiState.formattedResetTime)
            else -> null
        }

        if (displayedError != null) {
            ErrorAndOfflineBanner(
                error = displayedError,
                onRetry = if (displayedError.canRetry) onRetryClick else null
            )
        }

        // Generate Action Button
        NeonPrimaryButton(
            text = if (uiState.isQuotaExhausted) {
                "Daily Quota Reached (0/${uiState.quota.limit})"
            } else {
                "Generate ${uiState.selectedTool.shortTitle}"
            },
            onClick = onGenerateClick,
            enabled = !uiState.isQuotaExhausted && !uiState.isGenerating,
            loading = uiState.isGenerating
        )

        Spacer(modifier = Modifier.height(10.dp))
    }

    if (showGamePickerDialog) {
        val filteredGames = SupportedGame.filterByQuery(uiState.gameSearchQuery)
        AlertDialog(
            onDismissRequest = { showGamePickerDialog = false },
            containerColor = GlassCardBg,
            titleContentColor = CrispWhite,
            title = { Text("Select Supported Game") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.gameSearchQuery,
                        onValueChange = onUpdateGameSearch,
                        placeholder = { Text("Search games...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = SoftLavenderGlow
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    filteredGames.forEach { game ->
                        val active = uiState.selectedGame == game
                        Surface(
                            onClick = {
                                onSelectGame(game)
                                showGamePickerDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) ElevatedCardBg else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (active) ElectricViolet else GlassBorderViolet
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = game.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrispWhite
                                )
                                Text(
                                    text = game.genreHint,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGamePickerDialog = false }) {
                    Text("Close", color = SoftLavenderGlow)
                }
            }
        )
    }
}
