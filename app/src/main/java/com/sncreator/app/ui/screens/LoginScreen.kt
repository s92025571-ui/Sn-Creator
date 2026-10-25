package com.sncreator.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sncreator.app.model.AppError
import com.sncreator.app.ui.components.ErrorAndOfflineBanner
import com.sncreator.app.ui.components.GlassSurfaceCard
import com.sncreator.app.ui.components.SnLogo3D
import com.sncreator.app.ui.theme.CrispWhite
import com.sncreator.app.ui.theme.DeepRoyalViolet
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.GlassBorderViolet
import com.sncreator.app.ui.theme.MutedSlate
import com.sncreator.app.ui.theme.SecondarySlate
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.ui.theme.VoidBlack

@Composable
fun LoginScreen(
    isLoading: Boolean,
    authError: AppError?,
    onContinueWithGoogle: (android.content.Context) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140B29),
                        VoidBlack,
                        VoidBlack
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SnLogo3D(
                size = 118.dp,
                animate3D = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to SN Creator",
                style = MaterialTheme.typography.headlineMedium,
                color = CrispWhite,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Craft game-specific video titles, hashtags, paced voice-over scripts, thumbnail prompts, and fresh video ideas in English or Roman Urdu.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecondarySlate,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            GlassSurfaceCard {
                Text(
                    text = "Sign in to access your studio",
                    style = MaterialTheme.typography.titleMedium,
                    color = CrispWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Includes 10 AI generations per day (resets daily at midnight Pakistan time). Google authentication is required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onContinueWithGoogle(context) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrispWhite,
                        contentColor = VoidBlack,
                        disabledContainerColor = Color(0xFF332D47),
                        disabledContentColor = SecondarySlate
                    ),
                    border = BorderStroke(1.dp, GlassBorderViolet),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ElectricViolet,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Signing in with Google...",
                            style = MaterialTheme.typography.labelLarge,
                            color = CrispWhite
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = DeepRoyalViolet,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SoftLavenderGlow
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(18.dp))
                ErrorAndOfflineBanner(
                    error = authError,
                    onRetry = { onContinueWithGoogle(context) }
                )
            }
        }
    }
}
