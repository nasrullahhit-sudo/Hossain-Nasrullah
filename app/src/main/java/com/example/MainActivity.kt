package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ThemePreferences
import com.example.data.model.ThemeMode
import com.example.data.repository.ChallengeRepository
import com.example.notifications.ReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.printChallengeReportHtml
import com.example.ui.screens.ChecklistScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsBackupScreen
import com.example.ui.state.ChallengeUiState
import com.example.ui.state.ChallengeViewModel
import com.example.ui.theme.ChallengeTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

enum class ChallengeTab(val label: String, val icon: ImageVector) {
  DASHBOARD("Dashboard", Icons.Rounded.Dashboard),
  CHECKLIST("Checklist", Icons.Rounded.Checklist),
  REPORTS("Reports", Icons.Rounded.Assessment),
  SETTINGS("Settings", Icons.Rounded.Settings),
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ChallengeRootApp()
    }
  }
}

@Composable
private fun ChallengeRootApp() {
  val context = LocalContext.current
  val themePrefs = remember(context) { ThemePreferences(context.applicationContext) }
  val localThemeMode by themePrefs.themeModeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
  val coroutineScope = rememberCoroutineScope()

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    ChallengeTheme(themeMode = localThemeMode) {
      AuthScreen(
        themeMode = localThemeMode,
        onToggleTheme = {
          val next = if (localThemeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
          coroutineScope.launch { themePrefs.setThemeMode(next) }
        },
        onAuthSuccess = { currentUser = Firebase.auth.currentUser },
      )
    }
  } else {
    key(user.uid) {
      val viewModel: ChallengeViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val repo = ChallengeRepository(db)
            val prefs = ThemePreferences(app)
            ChallengeViewModel(
              repository = repo,
              themePreferences = prefs,
              currentUserId = user.uid,
              displayName = "Nasrullah",
            )
          }
        },
      )

      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      val activeTheme = (uiState as? ChallengeUiState.Ready)?.themeMode ?: localThemeMode

      ChallengeTheme(themeMode = activeTheme) {
        AuthenticatedChallengeShell(
          uiState = uiState,
          viewModel = viewModel,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedChallengeShell(
  uiState: ChallengeUiState,
  viewModel: ChallengeViewModel,
) {
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  var currentTab by rememberSaveable { mutableStateOf(ChallengeTab.DASHBOARD) }

  if (currentTab != ChallengeTab.DASHBOARD) {
    BackHandler { currentTab = ChallengeTab.DASHBOARD }
  }

  // SAF Export JSON Backup
  val jsonExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri ->
    if (uri != null) {
      val json = viewModel.buildBackupJson()
      if (json != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
          }
          viewModel.notifyMessage("JSON backup saved successfully")
        } catch (e: Exception) {
          viewModel.notifyMessage("Backup failed: ${e.localizedMessage}")
        }
      }
    }
  }

  // SAF Restore JSON Backup
  val jsonImportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      try {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (!text.isNullOrBlank()) {
          viewModel.restoreBackupJson(text)
        }
      } catch (e: Exception) {
        viewModel.notifyMessage("Could not read backup file: ${e.localizedMessage}")
      }
    }
  }

  // SAF Export CSV Report
  val csvExportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv"),
  ) { uri ->
    if (uri != null) {
      val csv = viewModel.buildCsvReport()
      if (csv != null) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(csv.toByteArray(Charsets.UTF_8))
          }
          viewModel.notifyMessage("CSV report exported successfully")
        } catch (e: Exception) {
          viewModel.notifyMessage("CSV export failed: ${e.localizedMessage}")
        }
      }
    }
  }

  val readyState = uiState as? ChallengeUiState.Ready
  LaunchedEffect(readyState?.reminders) {
    val reminders = readyState?.reminders
    if (reminders != null) {
      ReminderScheduler.syncReminders(context, reminders)
    }
  }

  LaunchedEffect(readyState?.statusMessage) {
    val msg = readyState?.statusMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  val isDark = (readyState?.themeMode ?: ThemeMode.SYSTEM) == ThemeMode.DARK

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 640.dp

    Scaffold(
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
          ),
          title = {
            Column {
              Text(
                text = "Nasrullah's 30-Day Challenge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "Discipline is Power | Keep Going!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          },
          actions = {
            IconButton(
              onClick = {
                val next = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                viewModel.setThemeMode(next)
              },
              modifier = Modifier.testTag("topbar_theme_toggle"),
            ) {
              Icon(
                imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Toggle Light or Dark Theme",
              )
            }
          },
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar {
            ChallengeTab.entries.forEach { tab ->
              NavigationBarItem(
                selected = currentTab == tab,
                onClick = { currentTab = tab },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
      ) {
        if (useRail) {
          NavigationRail(modifier = Modifier.fillMaxHeight()) {
            ChallengeTab.entries.forEach { tab ->
              NavigationRailItem(
                selected = currentTab == tab,
                onClick = { currentTab = tab },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (uiState) {
            ChallengeUiState.Loading -> {
              Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                CircularProgressIndicator()
                Text(
                  text = "Syncing Nasrullah's 30-Day Challenge...",
                  style = MaterialTheme.typography.bodyMedium,
                  modifier = Modifier.padding(top = 12.dp),
                )
              }
            }

            is ChallengeUiState.Error -> {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Text(
                  text = "Unable to sync challenge data",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = uiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }
            }

            is ChallengeUiState.Ready -> {
              val triggerPrint = {
                val html = viewModel.buildPrintableHtml()
                if (html != null) {
                  printChallengeReportHtml(context, html) { err ->
                    viewModel.notifyMessage(err)
                  }
                }
              }

              when (currentTab) {
                ChallengeTab.DASHBOARD -> DashboardScreen(
                  state = uiState,
                  onNavigateToDay = { day ->
                    viewModel.selectDay(day)
                    currentTab = ChallengeTab.CHECKLIST
                  },
                  onOpenChecklist = { currentTab = ChallengeTab.CHECKLIST },
                  onToggleReminder = viewModel::toggleReminder,
                )

                ChallengeTab.CHECKLIST -> ChecklistScreen(
                  state = uiState,
                  onSelectDay = viewModel::selectDay,
                  onSelectCategory = viewModel::selectCategory,
                  onTogglePendingOnly = viewModel::togglePendingOnly,
                  onToggleReorderMode = viewModel::toggleReorderMode,
                  onToggleHabit = viewModel::toggleHabitCompletion,
                  onAddHabit = viewModel::addHabit,
                  onUpdateHabit = viewModel::updateHabit,
                  onDeleteHabit = viewModel::deleteHabit,
                  onMoveHabit = viewModel::moveHabit,
                  onSaveMetricsAndNotes = viewModel::saveDailyMetricsAndNotes,
                )

                ChallengeTab.REPORTS -> ReportsScreen(
                  state = uiState,
                  onPrintReport = triggerPrint,
                  onExportCsv = { csvExportLauncher.launch("nasrullah_30day_challenge.csv") },
                )

                ChallengeTab.SETTINGS -> SettingsBackupScreen(
                  state = uiState,
                  onSetThemeMode = viewModel::setThemeMode,
                  onAddReminder = viewModel::addReminder,
                  onToggleReminder = viewModel::toggleReminder,
                  onDeleteReminder = viewModel::deleteReminder,
                  onExportJson = { jsonExportLauncher.launch("nasrullah_30day_backup.json") },
                  onImportJson = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain")) },
                  onExportCsv = { csvExportLauncher.launch("nasrullah_30day_challenge.csv") },
                  onPrintReport = triggerPrint,
                  onResetDefaults = viewModel::resetDefaultHabits,
                  onSignOut = { signOut(credentialManager, coroutineScope) },
                )
              }
            }
          }
        }
      }
    }
  }
}
