package com.sncreator.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sncreator.app.ui.components.GlassSurfaceCard
import com.sncreator.app.ui.theme.CrimsonError
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.EmeraldSuccess
import com.sncreator.app.ui.theme.ErrorContainerDark
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.util.SupportLinks

@Composable
fun HelpScreen() {
    val context = LocalContext.current
    var launchError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Get Help & Creator Guide",
            style = MaterialTheme.typography.headlineMedium,
            color = CrispWhite
        )
        Text(
            text = "Learn how to get the best gaming content out of SN Creator or message our support team directly on WhatsApp.",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondarySlate
        )

        // Direct WhatsApp Support Card
        GlassSurfaceCard(highlighted = true) {
            Text(
                text = "Need Direct Support?",
                style = MaterialTheme.typography.titleLarge,
                color = CrispWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap below to open WhatsApp with a pre-filled support message to the SN Creator team.",
                style = MaterialTheme.typography.bodyMedium,
                color = SecondarySlate
            )
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    launchError = null
                    val url = SupportLinks.buildGetHelpWhatsAppUrl()
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching {
                        context.startActivity(intent)
                    }.onFailure {
                        launchError = "Could not open WhatsApp or a web browser on this device. Support link: ${SupportLinks.HELP_WHATSAPP_BASE_URL}"
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSuccess,
                    contentColor = Color(0xFF04140A)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Get Help on WhatsApp",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (launchError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ErrorContainerDark,
                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = launchError.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispWhite,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // How to Use Guide
        Text(
            text = "How to Use SN Creator",
            style = MaterialTheme.typography.titleLarge,
            color = CrispWhite
        )

        HelpStepCard(
            icon = Icons.Outlined.SportsEsports,
            title = "1. Pick Your Game or Enter a Custom Title",
            body = "Choose PUBG Mobile, Free Fire, Last Asylum: Plague, Gangstar Mirage City, or select 'Other Game' to type any custom mobile, PC, or console game."
        )

        HelpStepCard(
            icon = Icons.Outlined.AutoAwesome,
            title = "2. Select One of the 5 AI Tools",
            body = "Generate Gaming Titles, platform Hashtags, duration-paced Voice-over Scripts (15s to 10m), copy-ready AI Thumbnail Prompts (text prompts only, not rendered images), or full Gaming Video Ideas."
        )

        HelpStepCard(
            icon = Icons.Outlined.Translate,
            title = "3. Choose English or Roman Urdu",
            body = "Select English or Roman Urdu (written in Latin letters) before generating, or use 'To Roman Urdu / To English' on the result screen to translate your active output. App menus always stay in English."
        )

        HelpStepCard(
            icon = Icons.Outlined.ContentCopy,
            title = "4. Copy Your Output Immediately",
            body = "For your privacy, SN Creator does not store generated-content history in the cloud or on your phone. Tap 'Copy Output' to save scripts, titles, or prompts to your notes."
        )

        HelpStepCard(
            icon = Icons.Outlined.Bolt,
            title = "5. Daily 10-Generation Quota (PKT)",
            body = "Every signed-in account gets 10 successful generations per day, resetting at 12:00 AM Pakistan Standard Time (Asia/Karachi). Regenerations and AI language switches each count as 1 generation. Failed or timed-out requests never deduct from your quota."
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun HelpStepCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    GlassSurfaceCard(contentPadding = PaddingValues(16.dp)) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SoftLavenderGlow,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )
            }
        }
    }
}
