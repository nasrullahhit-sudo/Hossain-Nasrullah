package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Reorder
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.ui.components.AddEditHabitDialog
import com.example.ui.components.ChallengeFooter
import com.example.ui.components.CompletionRing
import com.example.ui.components.DayNavigatorCard
import com.example.ui.state.ChallengeUiState
import java.util.Locale

@Composable
fun ChecklistScreen(
  state: ChallengeUiState.Ready,
  onSelectDay: (Int) -> Unit,
  onSelectCategory: (HabitCategory?) -> Unit,
  onTogglePendingOnly: () -> Unit,
  onToggleReorderMode: () -> Unit,
  onToggleHabit: (String) -> Unit,
  onAddHabit: (String, HabitCategory, String) -> Unit,
  onUpdateHabit: (String, String, HabitCategory, String) -> Unit,
  onDeleteHabit: (String) -> Unit,
  onMoveHabit: (String, Boolean) -> Unit,
  onSaveMetricsAndNotes: (
    notes: String,
    studyHours: Double,
    fiverrHours: Double,
    waterGlasses: Int,
    sleepHours: Double,
    linkedinConnections: Int,
    instagramDms: Int,
  ) -> Unit,
) {
  var showAddDialog by remember { mutableStateOf(false) }
  var editingHabit by remember { mutableStateOf<Habit?>(null) }

  val currentLog = state.logsByDay[state.selectedDayNumber]
  val completedSet = currentLog?.completedHabitIds?.toSet() ?: emptySet()

  val filteredHabits = remember(
    state.habits,
    state.selectedCategory,
    state.showPendingOnly,
    completedSet,
  ) {
    state.habits.filter { habit ->
      val catMatch = state.selectedCategory == null || habit.category == state.selectedCategory.wireValue
      val pendingMatch = !state.showPendingOnly || !completedSet.contains(habit.id)
      catMatch && pendingMatch
    }
  }

  // Local editable metrics & notes synced to selected day log
  var notesText by remember(state.selectedDayNumber, currentLog?.notes) {
    mutableStateOf(currentLog?.notes ?: "")
  }
  var studyHours by remember(state.selectedDayNumber, currentLog?.studyHours) {
    mutableDoubleStateOf(currentLog?.studyHours ?: 0.0)
  }
  var fiverrHours by remember(state.selectedDayNumber, currentLog?.fiverrHours) {
    mutableDoubleStateOf(currentLog?.fiverrHours ?: 0.0)
  }
  var waterGlasses by remember(state.selectedDayNumber, currentLog?.waterGlasses) {
    mutableIntStateOf(currentLog?.waterGlasses?.toInt() ?: 0)
  }
  var sleepHours by remember(state.selectedDayNumber, currentLog?.sleepHours) {
    mutableDoubleStateOf(currentLog?.sleepHours ?: 0.0)
  }
  var linkedinCount by remember(state.selectedDayNumber, currentLog?.linkedinConnections) {
    mutableIntStateOf(currentLog?.linkedinConnections?.toInt() ?: 0)
  }
  var instagramDms by remember(state.selectedDayNumber, currentLog?.instagramDms) {
    mutableIntStateOf(currentLog?.instagramDms?.toInt() ?: 0)
  }

  LaunchedEffect(currentLog) {
    if (currentLog != null) {
      notesText = currentLog.notes
      studyHours = currentLog.studyHours
      fiverrHours = currentLog.fiverrHours
      waterGlasses = currentLog.waterGlasses.toInt()
      sleepHours = currentLog.sleepHours
      linkedinCount = currentLog.linkedinConnections.toInt()
      instagramDms = currentLog.instagramDms.toInt()
    }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 840.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Day Navigator
      item {
        DayNavigatorCard(
          selectedDayNumber = state.selectedDayNumber,
          logsByDay = state.logsByDay,
          totalHabitsCount = state.habits.size,
          onSelectDay = onSelectDay,
        )
      }

      // Daily Completion Banner
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
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            CompletionRing(
              percent = state.analytics.selectedDayPercent,
              size = 76.dp,
              strokeWidth = 7.dp,
            )
            Column(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Day ${state.selectedDayNumber} Completion",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "${state.analytics.selectedDayCompletedCount} of ${state.analytics.totalHabitsCount} habits completed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              LinearProgressIndicator(
                progress = { (state.analytics.selectedDayPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp)),
              )
            }
          }
        }
      }

      // Filter Chips & Reorder Mode Toggle
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = state.selectedCategory == null,
              onClick = { onSelectCategory(null) },
              label = { Text("All (${state.habits.size})") },
            )
            HabitCategory.entries.forEach { cat ->
              FilterChip(
                selected = state.selectedCategory == cat,
                onClick = {
                  onSelectCategory(if (state.selectedCategory == cat) null else cat)
                },
                label = { Text(cat.shortLabel) },
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = state.showPendingOnly,
              onClick = onTogglePendingOnly,
              label = { Text("Pending Only") },
              modifier = Modifier.testTag("pending_only_chip"),
            )
            OutlinedButton(
              onClick = onToggleReorderMode,
              modifier = Modifier.testTag("reorder_mode_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Reorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (state.reorderMode) "Done Reordering" else "Reorder / Edit")
            }
          }
        }
      }

      // Empty State
      if (filteredHabits.isEmpty()) {
        item {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = if (state.showPendingOnly) "All habits in this view are completed!" else "No habits match this filter.",
                style = MaterialTheme.typography.titleSmall,
              )
              TextButton(onClick = {
                onSelectCategory(null)
                if (state.showPendingOnly) onTogglePendingOnly()
              }) {
                Text("Show All Habits")
              }
            }
          }
        }
      } else {
        itemsIndexed(
          items = filteredHabits,
          key = { _, habit -> habit.id },
        ) { index, habit ->
          val isChecked = completedSet.contains(habit.id)
          HabitChecklistRow(
            orderDisplay = index + 1,
            habit = habit,
            isChecked = isChecked,
            reorderMode = state.reorderMode,
            canMoveUp = state.habits.indexOfFirst { it.id == habit.id } > 0,
            canMoveDown = state.habits.indexOfFirst { it.id == habit.id } < state.habits.lastIndex,
            onToggle = { onToggleHabit(habit.id) },
            onEdit = { editingHabit = habit },
            onDelete = { onDeleteHabit(habit.id) },
            onMoveUp = { onMoveHabit(habit.id, true) },
            onMoveDown = { onMoveHabit(habit.id, false) },
          )
        }
      }

      // Daily Metrics & Notes Card
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = "Day ${state.selectedDayNumber} Metrics & Journal Notes",
              style = MaterialTheme.typography.titleMedium,
            )

            MetricStepperRow(
              label = "CST & Study Hours",
              valueText = "${String.format(Locale.US, "%.1f", studyHours)} h",
              onMinus = { studyHours = (studyHours - 0.5).coerceAtLeast(0.0) },
              onPlus = { studyHours = (studyHours + 0.5).coerceAtMost(24.0) },
            )
            MetricStepperRow(
              label = "Fiverr Online Availability",
              valueText = "${String.format(Locale.US, "%.1f", fiverrHours)} h",
              onMinus = { fiverrHours = (fiverrHours - 0.5).coerceAtLeast(0.0) },
              onPlus = { fiverrHours = (fiverrHours + 0.5).coerceAtMost(24.0) },
            )
            MetricStepperRow(
              label = "Water Glasses (Target 10)",
              valueText = "$waterGlasses / 10",
              onMinus = { waterGlasses = (waterGlasses - 1).coerceAtLeast(0) },
              onPlus = { waterGlasses = (waterGlasses + 1).coerceAtMost(50) },
            )
            MetricStepperRow(
              label = "Sleep Hours (Target 8h)",
              valueText = "${String.format(Locale.US, "%.1f", sleepHours)} h",
              onMinus = { sleepHours = (sleepHours - 0.5).coerceAtLeast(0.0) },
              onPlus = { sleepHours = (sleepHours + 0.5).coerceAtMost(24.0) },
            )
            MetricStepperRow(
              label = "New LinkedIn Connections (Target 10)",
              valueText = "$linkedinCount",
              onMinus = { linkedinCount = (linkedinCount - 1).coerceAtLeast(0) },
              onPlus = { linkedinCount = (linkedinCount + 1).coerceAtMost(500) },
            )
            MetricStepperRow(
              label = "Instagram Outreach DMs (Target 20)",
              valueText = "$instagramDms",
              onMinus = { instagramDms = (instagramDms - 2).coerceAtLeast(0) },
              onPlus = { instagramDms = (instagramDms + 2).coerceAtMost(500) },
            )

            OutlinedTextField(
              value = notesText,
              onValueChange = { notesText = it },
              label = { Text("Daily Notes & Reflections") },
              placeholder = { Text("Wins, blockers, Fiverr orders, YouTube batch progress...") },
              minLines = 3,
              maxLines = 6,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_notes_input"),
            )

            Button(
              onClick = {
                onSaveMetricsAndNotes(
                  notesText,
                  studyHours,
                  fiverrHours,
                  waterGlasses,
                  sleepHours,
                  linkedinCount,
                  instagramDms,
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_metrics_notes_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Save,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Save Day ${state.selectedDayNumber} Metrics & Notes")
            }
          }
        }
      }

      item {
        ChallengeFooter()
      }
    }

    FloatingActionButton(
      onClick = { showAddDialog = true },
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
        .testTag("add_habit_fab"),
    ) {
      Icon(imageVector = Icons.Rounded.Add, contentDescription = "Add New Habit")
    }
  }

  if (showAddDialog) {
    AddEditHabitDialog(
      initialHabit = null,
      onDismiss = { showAddDialog = false },
      onConfirm = { title, cat, detail ->
        onAddHabit(title, cat, detail)
        showAddDialog = false
      },
    )
  }

  val habitToEdit = editingHabit
  if (habitToEdit != null) {
    AddEditHabitDialog(
      initialHabit = habitToEdit,
      onDismiss = { editingHabit = null },
      onConfirm = { title, cat, detail ->
        onUpdateHabit(habitToEdit.id, title, cat, detail)
        editingHabit = null
      },
    )
  }
}

@Composable
private fun HabitChecklistRow(
  orderDisplay: Int,
  habit: Habit,
  isChecked: Boolean,
  reorderMode: Boolean,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
) {
  val bgColor = if (isChecked) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = bgColor,
    tonalElevation = if (isChecked) 0.dp else 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onToggle)
      .testTag("habit_row_${habit.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = isChecked,
        onCheckedChange = { onToggle() },
        modifier = Modifier.testTag("habit_checkbox_${habit.id}"),
      )
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "$orderDisplay. ${habit.title}",
          style = MaterialTheme.typography.titleSmall,
          textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
          color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = habit.category,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          if (habit.targetDetail.isNotBlank()) {
            Text(
              text = habit.targetDetail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      if (reorderMode) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up")
          }
          IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down")
          }
          IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit Habit")
          }
          IconButton(onClick = onDelete) {
            Icon(
              Icons.Rounded.DeleteOutline,
              contentDescription = "Delete Habit",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricStepperRow(
  label: String,
  valueText: String,
  onMinus: () -> Unit,
  onPlus: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.weight(1f),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextButton(onClick = onMinus) {
        Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
      Text(
        text = valueText,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp),
      )
      TextButton(onClick = onPlus) {
        Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }
    }
  }
}
