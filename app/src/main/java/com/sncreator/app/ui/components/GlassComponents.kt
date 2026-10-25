package com.sncreator.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.model.AiToolType
import com.sncreator.app.model.AppError
import com.sncreator.app.ui.theme.CrimsonError
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.DeepRoyalViolet
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.ElevatedCardBg
import com.sncreator.app.ui.theme.ErrorContainerDark
import com.sncreator.app.ui.theme.GlassBorderViolet
import com.sncreator.app.ui.theme.GlassCardBg
import com.sncreator.app.ui.theme.NeonPurple
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = if (highlighted) ElectricViolet else GlassBorderViolet
    val containerColor = if (highlighted) ElevatedCardBg else GlassCardBg

    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = containerColor,
        border = BorderStroke(width = if (highlighted) 1.5.dp else 1.dp, color = borderColor),
        shadowElevation = if (highlighted) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun ToolBadgeIcon(
    tool: AiToolType,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector = when (tool) {
        AiToolType.GAMING_TITLES -> Icons.Outlined.Title
        AiToolType.HASHTAGS -> Icons.Outlined.Tag
        AiToolType.VOICEOVER_SCRIPT -> Icons.Outlined.GraphicEq
        AiToolType.THUMBNAIL_PROMPT -> Icons.Outlined.Image
        AiToolType.VIDEO_IDEAS -> Icons.Outlined.Lightbulb
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (selected) {
                        listOf(ElectricViolet, DeepRoyalViolet)
                    } else {
                        listOf(Color(0xFF231942), Color(0xFF141126))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tool.title,
            tint = if (selected) CrispWhite else SoftLavenderGlow,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun NeonPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = Icons.Outlined.AutoAwesome
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = NeonPurple,
            contentColor = CrispWhite,
            disabledContainerColor = Color(0xFF28223B),
            disabledContentColor = Color(0xFF7B7494)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = CrispWhite,
                strokeWidth = 2.2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Generating with AI...",
                style = MaterialTheme.typography.labelLarge
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ErrorAndOfflineBanner(
    error: AppError,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isOffline = error is AppError.Offline
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ErrorContainerDark,
        border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Outlined.WifiOff else Icons.Outlined.WarningAmber,
                    contentDescription = if (isOffline) "Offline" else "Error",
                    tint = CrimsonError,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = if (isOffline) "No Internet Connection" else "Action Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispWhite
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondarySlate
            )
            if (error.canRetry && onRetry != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRetry,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SoftLavenderGlow.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrispWhite)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}
