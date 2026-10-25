package com.sncreator.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.SupportedGame
import com.sncreator.app.ui.components.ErrorAndOfflineBanner
import com.sncreator.app.ui.components.GlassSurfaceCard
import com.sncreator.app.ui.components.NeonPrimaryButton
import com.sncreator.app.ui.components.SnLogo3D
import com.sncreator.app.ui.components.ToolBadgeIcon
import com.sncreator.app.ui.theme.AmberWarning
import com.sncreator.app.ui.theme.CrimsonError
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.GlassBorderViolet
import com.sncreator.app.ui.theme.GlassCardBg
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.viewmodel.SnCreatorUiState

@Composable
fun HomeScreen(
    uiState: SnCreatorUiState,
    onCreateContentClick: () -> Unit,
    onSelectGameQuick: (SupportedGame) -> Unit,
    onSelectToolQuick: (AiToolType) -> Unit,
    onOpenHelpClick: () -> Unit,
    onRefreshQuota: () -> Unit
) {
    val greetingName = uiState.currentUser?.greetingName ?: "Creator"
    val remaining = uiState.quota.remaining.coerceIn(0, uiState.quota.limit)
    val progress = (remaining.toFloat() / uiState.quota.limit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Greeting Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SN Creator Studio",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftLavenderGlow
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome back, $greetingName",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CrispWhite
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            SnLogo3D(size = 48.dp, animate3D = false)
        }

        // Daily Generation Quota Card
        GlassSurfaceCard(highlighted = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = SoftLavenderGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Daily AI Quota",
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispWhite
                    )
                }

                if (uiState.isQuotaLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = SoftLavenderGlow,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onRefreshQuota,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh quota",
                            tint = MutedSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$remaining/${uiState.quota.limit} generations left",
                style = MaterialTheme.typography.headlineMedium,
                color = when {
                    remaining == 0 -> CrimsonError
                    remaining <= 3 -> AmberWarning
                    else -> CrispWhite
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = when {
                    remaining == 0 -> CrimsonError
                    remaining <= 3 -> AmberWarning
                    else -> ElectricViolet
                },
                trackColor = Color(0xFF28223D)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (remaining == 0) {
                    "Daily limit reached. Resets at ${uiState.formattedResetTime} (Asia/Karachi)."
                } else {
                    "Resets daily at midnight Pakistan time (${uiState.formattedResetTime}). Failed requests never deduct quota."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeonPrimaryButton(
                text = "Create Content",
                onClick = onCreateContentClick,
                leadingIcon = Icons.Outlined.AutoAwesome
            )
        }

        if (!uiState.isOnline) {
            ErrorAndOfflineBanner(
                error = com.sncreator.app.model.AppError.Offline,
                onRetry = onRefreshQuota
            )
        } else if (uiState.quotaError != null) {
            ErrorAndOfflineBanner(
                error = uiState.quotaError,
                onRetry = onRefreshQuota
            )
        }

        // Quick Access: Supported Games
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Supported Games",
                style = MaterialTheme.typography.titleLarge,
                color = CrispWhite
            )
            Text(
                text = "Tap a game to jump straight into content creation",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SupportedGame.entries.forEach { game ->
                    Surface(
                        onClick = { onSelectGameQuick(game) },
                        shape = RoundedCornerShape(14.dp),
                        color = GlassCardBg,
                        border = BorderStroke(1.dp, GlassBorderViolet)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SportsEsports,
                                contentDescription = null,
                                tint = SoftLavenderGlow,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = game.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = CrispWhite
                            )
                        }
                    }
                }
            }
        }

        // Quick Access: 5 AI Tools
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Five AI Creator Tools",
                style = MaterialTheme.typography.titleLarge,
                color = CrispWhite
            )

            AiToolType.entries.forEach { tool ->
                GlassSurfaceCard(
                    onClick = { onSelectToolQuick(tool) },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ToolBadgeIcon(tool = tool)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tool.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = tool.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondarySlate
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Open ${tool.title}",
                            tint = SoftLavenderGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Contact / Get Help Shortcut
        GlassSurfaceCard(contentPadding = PaddingValues(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = SoftLavenderGlow,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "Need help with SN Creator?",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite
                        )
                        Text(
                            text = "View creator tips or message support on WhatsApp.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedButton(
                    onClick = onOpenHelpClick,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlassBorderViolet)
                ) {
                    Text("Get Help", color = CrispWhite)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
