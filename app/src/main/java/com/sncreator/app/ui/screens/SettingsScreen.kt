package com.sncreator.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.BuildConfig
import com.sncreator.app.model.AuthenticatedUser
import com.sncreator.app.model.OutputLanguage
import com.sncreator.app.ui.components.GlassSurfaceCard
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
import com.sncreator.app.ui.theme.SubtleDivider
import com.sncreator.app.util.SupportLinks

private enum class LegalDocModal {
    PRIVACY_POLICY,
    TERMS_OF_USE,
    CONTACT_PLACEHOLDER_INFO
}

@Composable
fun SettingsScreen(
    user: AuthenticatedUser?,
    preferredOutputLanguage: OutputLanguage,
    onChangePreferredLanguage: (OutputLanguage) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var activeModal by remember { mutableStateOf<LegalDocModal?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = CrispWhite
        )

        // Signed-in Google Profile Card
        GlassSurfaceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = DeepRoyalViolet,
                    border = BorderStroke(1.dp, ElectricViolet),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = SoftLavenderGlow,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user?.greetingName ?: "Google Account",
                        style = MaterialTheme.typography.titleLarge,
                        color = CrispWhite
                    )
                    Text(
                        text = user?.email ?: "Verified Firebase Google Session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondarySlate
                    )
                }
            }
        }

        // Generated-Content Language Preference (Output only, never changes UI language)
        GlassSurfaceCard {
            Text(
                text = "Generated-Content Language",
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sets the default output language for AI-generated titles, scripts, and ideas. The app interface always remains in English.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutputLanguage.entries.forEach { language ->
                    val selected = preferredOutputLanguage == language
                    Surface(
                        onClick = { onChangePreferredLanguage(language) },
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
                                text = language.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = CrispWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == OutputLanguage.ROMAN_URDU) "Latin Urdu output" else "English output",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftLavenderGlow
                            )
                        }
                    }
                }
            }
        }

        // Legal, Contact & Version Info
        GlassSurfaceCard(contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp)) {
            SettingsRowItem(
                icon = Icons.Outlined.Shield,
                title = "Privacy Policy",
                subtitle = "How authentication, quotas, and zero-history work",
                onClick = { activeModal = LegalDocModal.PRIVACY_POLICY }
            )
            HorizontalDivider(color = SubtleDivider)
            SettingsRowItem(
                icon = Icons.Outlined.Description,
                title = "Terms of Use",
                subtitle = "Usage rules, AI content disclaimers, and daily limits",
                onClick = { activeModal = LegalDocModal.TERMS_OF_USE }
            )
            HorizontalDivider(color = SubtleDivider)
            SettingsRowItem(
                icon = Icons.Outlined.ContactSupport,
                title = "Contact SN Creator",
                subtitle = if (SupportLinks.resolveContactSnCreatorUrl(BuildConfig.CONTACT_WHATSAPP_NUMBER) != null) {
                    "Message official SN Creator contact on WhatsApp"
                } else {
                    "WhatsApp number placeholder (not assigned yet)"
                },
                onClick = {
                    val contactUrl = SupportLinks.resolveContactSnCreatorUrl(BuildConfig.CONTACT_WHATSAPP_NUMBER)
                    if (contactUrl != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contactUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { context.startActivity(intent) }
                    } else {
                        activeModal = LegalDocModal.CONTACT_PLACEHOLDER_INFO
                    }
                }
            )
            HorizontalDivider(color = SubtleDivider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = SoftLavenderGlow,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "App Version",
                        style = MaterialTheme.typography.titleMedium,
                        color = CrispWhite
                    )
                }
                Text(
                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }
        }

        // Sign Out Action
        OutlinedButton(
            onClick = onSignOut,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonError),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    when (activeModal) {
        LegalDocModal.PRIVACY_POLICY -> {
            LegalModalDialog(
                title = "Privacy Policy",
                body = LegalTexts.PRIVACY_POLICY_TEXT.trim(),
                onDismiss = { activeModal = null }
            )
        }
        LegalDocModal.TERMS_OF_USE -> {
            LegalModalDialog(
                title = "Terms of Use",
                body = LegalTexts.TERMS_OF_USE_TEXT.trim(),
                onDismiss = { activeModal = null }
            )
        }
        LegalDocModal.CONTACT_PLACEHOLDER_INFO -> {
            LegalModalDialog(
                title = "Contact SN Creator",
                body = "The official Contact SN Creator WhatsApp number has not been finalized yet and is configured as SN_CONTACT_WHATSAPP_NUMBER (${SupportLinks.UNCONFIGURED_CONTACT_PLACEHOLDER}).\n\nFor app assistance right now, open the 'Get Help' tab and tap 'Get Help on WhatsApp'.",
                onDismiss = { activeModal = null }
            )
        }
        null -> Unit
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SoftLavenderGlow,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CrispWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MutedSlate
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MutedSlate
        )
    }
}

@Composable
private fun LegalModalDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassCardBg,
        titleContentColor = CrispWhite,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .height(340.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondarySlate
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = SoftLavenderGlow)
            }
        }
    )
}
