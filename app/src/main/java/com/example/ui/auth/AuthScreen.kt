package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.ChallengeSchedule
import com.example.data.model.ThemeMode

@Composable
fun AuthScreen(
  themeMode: ThemeMode,
  onToggleTheme: () -> Unit,
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .padding(20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50),
          ) {
            Text(
              text = "${ChallengeSchedule.START_DISPLAY} – ${ChallengeSchedule.END_DISPLAY}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier.testTag("auth_theme_toggle"),
          ) {
            Icon(
              imageVector = if (themeMode == ThemeMode.DARK) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
              contentDescription = "Toggle Light or Dark Mode",
            )
          }
        }

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_challenge_hero),
                contentDescription = "Nasrullah's 30-Day Challenge Journey Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, Color(0xCC061C1A)),
                    )
                  ),
              )
              Column(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .padding(20.dp),
              ) {
                Text(
                  text = "Nasrullah's 30-Day Challenge",
                  style = MaterialTheme.typography.headlineMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Discipline is Power | Keep Going!",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFFFBBF24),
                )
              }
            }

            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "Welcome, Nasrullah!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "Sign in with Google to sync your 21 daily habits, prayer & study logs, Fiverr hours, outreach metrics, and 30-day reports securely across devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureHighlightRow(
                  icon = Icons.Rounded.CalendarMonth,
                  text = "30-Day Window: 17 Nov 2026 – 16 Dec 2026",
                )
                FeatureHighlightRow(
                  icon = Icons.Rounded.CheckCircle,
                  text = "21 Daily Habits, Metrics, Notes & Weekly Reports",
                )
                FeatureHighlightRow(
                  icon = Icons.Rounded.Shield,
                  text = "Cloud Firestore Sync + JSON Backup & CSV Export",
                )
              }

              if (errorMessage != null) {
                Surface(
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                  shape = MaterialTheme.shapes.small,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))

              Button(
                onClick = {
                  errorMessage = null
                  isLoading = true
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onAuthError = { msg ->
                      isLoading = false
                      errorMessage = msg
                    },
                    scope = coroutineScope,
                    onAuthCancelled = { isLoading = false },
                  )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("google_sign_in_button"),
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text("Signing in...", style = MaterialTheme.typography.labelLarge)
                } else {
                  Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
                }
              }
            }
          }
        }

        Text(
          text = "Designed for Nasrullah",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun FeatureHighlightRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  text: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(18.dp),
    )
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
