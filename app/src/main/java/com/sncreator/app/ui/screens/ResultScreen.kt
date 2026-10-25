package com.sncreator.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.model.ActiveResultSession
import com.sncreator.app.model.AppError
import com.sncreator.app.model.QuotaDto
import com.sncreator.app.ui.components.ErrorAndOfflineBanner
import com.sncreator.app.ui.components.GlassSurfaceCard
import com.sncreator.app.ui.components.ToolBadgeIcon
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.EmeraldSuccess
import com.sncreator.app.ui.theme.GlassBorderViolet
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.NeonPurple
import com.sncreator.app.ui.theme.ObsidianSurface
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow
import kotlinx.coroutines.delay

@Composable
fun ResultScreen(
    session: ActiveResultSession,
    quota: QuotaDto,
    formattedResetTime: String,
    isLoading: Boolean,
    isOnline: Boolean,
    resultError: AppError?,
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    onSwitchLanguage: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    var copiedConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(copiedConfirmation) {
        if (copiedConfirmation) {
            delay(2200L)
            copiedConfirmation = false
        }
    }

    val quotaExhausted = quota.remaining <= 0
    val oppositeLanguage = session.language.opposite()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back to Create",
                    tint = CrispWhite
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            ToolBadgeIcon(tool = session.tool, selected = true)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.tool.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispWhite
                )
                val metaParts = buildList {
                    add(session.resolvedGameName)
                    add(session.language.displayName)
                    if (session.duration != null) add(session.duration.label)
                }.joinToString(" · ")
                Text(
                    text = metaParts,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftLavenderGlow
                )
            }
        }

        // Quota & Privacy Ephemeral Notice
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, GlassBorderViolet),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${quota.remaining}/${quota.limit} generations left today",
                    style = MaterialTheme.typography.labelLarge,
                    color = CrispWhite
                )
                Text(
                    text = "Active session only · Not saved",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSlate
                )
            }
        }

        // Loading State Banner for Regenerate / Switch Language
        if (isLoading) {
            GlassSurfaceCard(highlighted = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = SoftLavenderGlow,
                        strokeWidth = 2.4.dp
                    )
                    Column {
                        Text(
                            text = "Processing new AI request...",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite
                        )
                        Text(
                            text = "Please wait. Your previous result stays visible until ready.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondarySlate
                        )
                    }
                }
            }
        }

        val activeError = when {
            !isOnline -> AppError.Offline
            resultError != null -> resultError
            else -> null
        }

        if (activeError != null) {
            ErrorAndOfflineBanner(
                error = activeError,
                onRetry = if (activeError.canRetry) onRetry else null
            )
        }

        // Copy Confirmation Banner
        if (copiedConfirmation) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldSuccess.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.65f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = EmeraldSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Copied to clipboard. Save or paste it before leaving — history is not stored.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispWhite
                    )
                }
            }
        }

        // Selectable Output Content Card
        GlassSurfaceCard(highlighted = true) {
            SelectionContainer {
                Text(
                    text = session.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CrispWhite
                )
            }
        }

        // Action Controls: Copy, Regenerate, Switch Language
        Button(
            onClick = {
                copyTextToClipboard(
                    context = context,
                    label = "SN Creator - ${session.tool.title}",
                    text = session.content
                )
                copiedConfirmation = true
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonPurple,
                contentColor = CrispWhite
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (copiedConfirmation) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (copiedConfirmation) "Copied!" else "Copy Output",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onRegenerate,
                enabled = !isLoading && !quotaExhausted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, GlassBorderViolet),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Regenerate")
            }

            OutlinedButton(
                onClick = onSwitchLanguage,
                enabled = !isLoading && !quotaExhausted,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ElectricViolet),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("To ${oppositeLanguage.displayName}")
            }
        }

        Text(
            text = if (quotaExhausted) {
                "No generations remaining today. Regenerate and Switch Language re-enable at $formattedResetTime."
            } else {
                "Regenerate and Switch Language each use 1 generation from your daily 10-generation quota."
            },
            style = MaterialTheme.typography.labelMedium,
            color = MutedSlate
        )

        Spacer(modifier = Modifier.height(10.dp))
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
