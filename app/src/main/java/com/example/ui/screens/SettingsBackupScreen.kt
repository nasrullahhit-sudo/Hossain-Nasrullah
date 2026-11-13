package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ChallengeReminder
import com.example.data.model.ThemeMode
import com.example.ui.components.AddReminderDialog
import com.example.ui.components.ChallengeFooter
import com.example.ui.state.ChallengeUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SettingsBackupScreen(
  state: ChallengeUiState.Ready,
  onSetThemeMode: (ThemeMode) -> Unit,
  onAddReminder: (title: String, targetText: String, hour: Int, minute: Int) -> Unit,
  onToggleReminder: (ChallengeReminder, Boolean) -> Unit,
  onDeleteReminder: (String) -> Unit,
  onExportJson: () -> Unit,
  onImportJson: () -> Unit,
  onExportCsv: () -> Unit,
  onPrintReport: () -> Unit,
  onResetDefaults: () -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  var showAddReminder by remember { mutableStateOf(false) }
  var showResetConfirm by remember { mutableStateOf(false) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = {},
  )

  val userEmail = Firebase.auth.currentUser?.email ?: "Signed in with Google"

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Theme Mode
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("Appearance (Dark & Light Mode)", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = state.themeMode == mode,
                  onClick = { onSetThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(index = idx, count = ThemeMode.entries.size),
                  modifier = Modifier.testTag("theme_mode_${mode.wireValue}"),
                ) {
                  Text(mode.label)
                }
              }
            }
          }
        }
      }

      // Daily Targets & Notification Reminders
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Targets & Daily Reminders", style = MaterialTheme.typography.titleMedium)
              OutlinedButton(
                onClick = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  showAddReminder = true
                },
                modifier = Modifier.testTag("add_reminder_button"),
              ) {
                Icon(Icons.Rounded.AddAlarm, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add")
              }
            }

            state.reminders.forEach { reminder ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceContainerLow)
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(reminder.title, style = MaterialTheme.typography.titleSmall)
                  Text(
                    "${reminder.timeLabel} • ${reminder.targetText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = reminder.enabled,
                  onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onToggleReminder(reminder, enabled)
                  },
                )
                IconButton(onClick = { onDeleteReminder(reminder.id) }) {
                  Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }

      // Backup, Restore, CSV & Print
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("Backup, Export & Print", style = MaterialTheme.typography.titleMedium)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = onExportJson,
                modifier = Modifier
                  .weight(1f)
                  .testTag("json_backup_button"),
              ) {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("JSON Backup")
              }
              OutlinedButton(
                onClick = onImportJson,
                modifier = Modifier
                  .weight(1f)
                  .testTag("json_restore_button"),
              ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore JSON")
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = onExportCsv,
                modifier = Modifier
                  .weight(1f)
                  .testTag("csv_export_button"),
              ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export CSV")
              }
              OutlinedButton(
                onClick = onPrintReport,
                modifier = Modifier
                  .weight(1f)
                  .testTag("settings_print_button"),
              ) {
                Icon(Icons.Rounded.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Report")
              }
            }

            OutlinedButton(
              onClick = { showResetConfirm = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_defaults_button"),
            ) {
              Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Reset 21 Default Habits")
            }
          }
        }
      }

      // Account Card
      item {
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Nasrullah's Cloud Sync", style = MaterialTheme.typography.titleMedium)
              Text(
                userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedButton(
              onClick = onSignOut,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sign Out")
            }
          }
        }
      }

      item {
        ChallengeFooter()
      }
    }
  }

  if (showAddReminder) {
    AddReminderDialog(
      onDismiss = { showAddReminder = false },
      onConfirm = { title, target, h, m ->
        onAddReminder(title, target, h, m)
        showAddReminder = false
      },
    )
  }

  if (showResetConfirm) {
    AlertDialog(
      onDismissRequest = { showResetConfirm = false },
      title = { Text("Reset 21 Default Habits?") },
      text = {
        Text("This restores Nasrullah's 21 default habits in their original order. Your daily metrics and notes will remain intact.")
      },
      confirmButton = {
        Button(onClick = {
          onResetDefaults()
          showResetConfirm = false
        }) {
          Text("Restore Defaults")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
      },
    )
  }
}
