package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChallengeBackupCodec
import com.example.data.ChallengeSchedule
import com.example.data.ThemePreferences
import com.example.data.model.ChallengeReminder
import com.example.data.model.DailyLog
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.example.data.repository.ChallengeRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayBarStat(
  val dayNumber: Int,
  val shortLabel: String,
  val dateLabel: String,
  val percent: Int,
  val completedCount: Int,
  val totalCount: Int,
)

data class WeeklySummary(
  val weekNumber: Int,
  val title: String,
  val dateRange: String,
  val avgPercent: Int,
  val completedChecks: Int,
  val possibleChecks: Int,
  val bestDayLabel: String,
  val studyHours: Double,
  val fiverrHours: Double,
  val outreachTotal: Long,
)

data class HabitConsistencyStat(
  val habit: Habit,
  val completedDays: Int,
  val percent: Int,
)

data class CategoryStat(
  val category: HabitCategory,
  val percent: Int,
  val completedChecks: Int,
  val possibleChecks: Int,
)

data class ChallengeAnalytics(
  val overallPercent: Int = 0,
  val selectedDayPercent: Int = 0,
  val selectedDayCompletedCount: Int = 0,
  val totalHabitsCount: Int = 0,
  val currentStreak: Int = 0,
  val bestStreak: Int = 0,
  val totalCompletedChecks: Int = 0,
  val totalStudyHours: Double = 0.0,
  val totalFiverrHours: Double = 0.0,
  val totalWaterGlasses: Long = 0L,
  val averageSleepHours: Double = 0.0,
  val totalLinkedinConnections: Long = 0L,
  val totalInstagramDms: Long = 0L,
  val last7Days: List<DayBarStat> = emptyList(),
  val weeklySummaries: List<WeeklySummary> = emptyList(),
  val habitConsistency: List<HabitConsistencyStat> = emptyList(),
  val categoryMastery: List<CategoryStat> = emptyList(),
)

sealed interface ChallengeUiState {
  data object Loading : ChallengeUiState
  data class Ready(
    val profile: UserProfile?,
    val habits: List<Habit>,
    val logsByDay: Map<Int, DailyLog>,
    val reminders: List<ChallengeReminder>,
    val selectedDayNumber: Int,
    val selectedCategory: HabitCategory?,
    val showPendingOnly: Boolean,
    val reorderMode: Boolean,
    val themeMode: ThemeMode,
    val analytics: ChallengeAnalytics,
    val statusMessage: String?,
    val isBusy: Boolean,
  ) : ChallengeUiState

  data class Error(val message: String) : ChallengeUiState
}

class ChallengeViewModel(
  private val repository: ChallengeRepository,
  private val themePreferences: ThemePreferences,
  private val currentUserId: String,
  private val displayName: String,
) : ViewModel() {

  private val selectedDayFlow = MutableStateFlow(ChallengeSchedule.currentStatus().activeDayNumber)
  private val selectedCategoryFlow = MutableStateFlow<HabitCategory?>(null)
  private val showPendingOnlyFlow = MutableStateFlow(false)
  private val reorderModeFlow = MutableStateFlow(false)
  private val statusMessageFlow = MutableStateFlow<String?>(null)
  private val isBusyFlow = MutableStateFlow(false)

  private data class FilterState(
    val dayNumber: Int,
    val category: HabitCategory?,
    val pendingOnly: Boolean,
    val reorderMode: Boolean,
    val statusMessage: String?,
    val isBusy: Boolean,
  )

  private val filterStateFlow = combine(
    selectedDayFlow,
    selectedCategoryFlow,
    showPendingOnlyFlow,
    reorderModeFlow,
    statusMessageFlow,
    isBusyFlow,
  ) { arr ->
    FilterState(
      dayNumber = arr[0] as Int,
      category = arr[1] as HabitCategory?,
      pendingOnly = arr[2] as Boolean,
      reorderMode = arr[3] as Boolean,
      statusMessage = arr[4] as String?,
      isBusy = arr[5] as Boolean,
    )
  }

  val uiState: StateFlow<ChallengeUiState> = combine(
    repository.observeUserProfile(currentUserId),
    repository.observeHabits(currentUserId),
    repository.observeDailyLogs(currentUserId),
    repository.observeReminders(currentUserId),
    themePreferences.themeModeFlow,
    filterStateFlow,
  ) { arr ->
    val profile = arr[0] as UserProfile?
    @Suppress("UNCHECKED_CAST")
    val habits = arr[1] as List<Habit>
    @Suppress("UNCHECKED_CAST")
    val logs = arr[2] as List<DailyLog>
    @Suppress("UNCHECKED_CAST")
    val reminders = arr[3] as List<ChallengeReminder>
    val localTheme = arr[4] as ThemeMode
    val filters = arr[5] as FilterState

    val logsByDay = logs.associateBy { it.dayNumber.toInt() }
    val resolvedTheme = if (localTheme != ThemeMode.SYSTEM) {
      localTheme
    } else {
      ThemeMode.fromWire(profile?.themeMode)
    }

    val analytics = computeAnalytics(
      habits = habits,
      logsByDay = logsByDay,
      selectedDayNumber = filters.dayNumber,
    )

    ChallengeUiState.Ready(
      profile = profile,
      habits = habits,
      logsByDay = logsByDay,
      reminders = reminders,
      selectedDayNumber = filters.dayNumber,
      selectedCategory = filters.category,
      showPendingOnly = filters.pendingOnly,
      reorderMode = filters.reorderMode,
      themeMode = resolvedTheme,
      analytics = analytics,
      statusMessage = filters.statusMessage,
      isBusy = filters.isBusy,
    ) as ChallengeUiState
  }.catch { error ->
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val rootError = firestoreError ?: error
    Log.w(TAG, "Error observing challenge data", rootError)
    emit(ChallengeUiState.Error(rootError.message ?: "Failed to sync challenge data."))
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = ChallengeUiState.Loading,
  )

  init {
    viewModelScope.launch {
      repository.ensureUserSeeded(displayName = displayName.ifBlank { "Nasrullah" })
        .onFailure { err ->
          Log.w(TAG, "Initial seed check failed", err)
          statusMessageFlow.value = err.localizedMessage ?: "Failed to initialize challenge defaults."
        }
    }
  }

  fun selectDay(dayNumber: Int) {
    selectedDayFlow.value = dayNumber.coerceIn(1, ChallengeSchedule.TOTAL_DAYS)
  }

  fun selectCategory(category: HabitCategory?) {
    selectedCategoryFlow.value = category
  }

  fun togglePendingOnly() {
    showPendingOnlyFlow.value = !showPendingOnlyFlow.value
  }

  fun toggleReorderMode() {
    reorderModeFlow.value = !reorderModeFlow.value
  }

  fun clearStatusMessage() {
    statusMessageFlow.value = null
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      themePreferences.setThemeMode(mode)
      repository.updateThemeMode(mode)
    }
  }

  fun toggleHabitCompletion(habitId: String) {
    val state = uiState.value as? ChallengeUiState.Ready ?: return
    val day = state.selectedDayNumber
    val existing = state.logsByDay[day]
    val currentSet = existing?.completedHabitIds?.toMutableSet() ?: mutableSetOf()
    if (currentSet.contains(habitId)) {
      currentSet.remove(habitId)
    } else {
      currentSet.add(habitId)
    }

    viewModelScope.launch {
      repository.saveDailyLog(
        dayNumber = day,
        existingLog = existing,
        completedHabitIds = currentSet.toList(),
        totalHabitsSnapshot = state.habits.size.coerceAtLeast(1),
        notes = existing?.notes ?: "",
        studyHours = existing?.studyHours ?: 0.0,
        fiverrHours = existing?.fiverrHours ?: 0.0,
        waterGlasses = existing?.waterGlasses?.toInt() ?: 0,
        sleepHours = existing?.sleepHours ?: 0.0,
        linkedinConnections = existing?.linkedinConnections?.toInt() ?: 0,
        instagramDms = existing?.instagramDms?.toInt() ?: 0,
      ).onFailure { err ->
        statusMessageFlow.value = err.localizedMessage ?: "Could not update habit status."
      }
    }
  }

  fun saveDailyMetricsAndNotes(
    notes: String,
    studyHours: Double,
    fiverrHours: Double,
    waterGlasses: Int,
    sleepHours: Double,
    linkedinConnections: Int,
    instagramDms: Int,
  ) {
    val state = uiState.value as? ChallengeUiState.Ready ?: return
    val day = state.selectedDayNumber
    val existing = state.logsByDay[day]

    viewModelScope.launch {
      repository.saveDailyLog(
        dayNumber = day,
        existingLog = existing,
        completedHabitIds = existing?.completedHabitIds ?: emptyList(),
        totalHabitsSnapshot = state.habits.size.coerceAtLeast(1),
        notes = notes,
        studyHours = studyHours,
        fiverrHours = fiverrHours,
        waterGlasses = waterGlasses,
        sleepHours = sleepHours,
        linkedinConnections = linkedinConnections,
        instagramDms = instagramDms,
      ).onSuccess {
        statusMessageFlow.value = "Day $day metrics & notes saved"
      }.onFailure { err ->
        statusMessageFlow.value = err.localizedMessage ?: "Could not save metrics."
      }
    }
  }

  fun addHabit(title: String, category: HabitCategory, targetDetail: String) {
    val state = uiState.value as? ChallengeUiState.Ready ?: return
    if (title.isBlank()) return
    val nextOrder = (state.habits.maxOfOrNull { it.sortOrder } ?: -1L) + 1L
    viewModelScope.launch {
      repository.addHabit(title, category, targetDetail, nextOrder)
        .onSuccess { statusMessageFlow.value = "Habit added" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to add habit" }
    }
  }

  fun updateHabit(habitId: String, title: String, category: HabitCategory, targetDetail: String) {
    if (title.isBlank()) return
    viewModelScope.launch {
      repository.updateHabit(habitId, title, category, targetDetail)
        .onSuccess { statusMessageFlow.value = "Habit updated" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to update habit" }
    }
  }

  fun deleteHabit(habitId: String) {
    viewModelScope.launch {
      repository.deleteHabit(habitId)
        .onSuccess { statusMessageFlow.value = "Habit deleted" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to delete habit" }
    }
  }

  fun moveHabit(habitId: String, moveUp: Boolean) {
    val state = uiState.value as? ChallengeUiState.Ready ?: return
    val list = state.habits.sortedBy { it.sortOrder }.toMutableList()
    val index = list.indexOfFirst { it.id == habitId }
    if (index == -1) return
    val targetIndex = if (moveUp) index - 1 else index + 1
    if (targetIndex !in list.indices) return

    val item = list.removeAt(index)
    list.add(targetIndex, item)
    viewModelScope.launch {
      repository.reorderHabits(list)
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to reorder habits" }
    }
  }

  fun resetDefaultHabits() {
    viewModelScope.launch {
      isBusyFlow.value = true
      repository.resetDefaultHabits()
        .onSuccess { statusMessageFlow.value = "Restored 21 default habits" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Reset failed" }
      isBusyFlow.value = false
    }
  }

  fun addReminder(title: String, targetText: String, hour: Int, minute: Int) {
    if (title.isBlank()) return
    val amPm = if (hour >= 12) "PM" else "AM"
    val h12 = when {
      hour == 0 -> 12
      hour > 12 -> hour - 12
      else -> hour
    }
    val timeLabel = "${h12.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $amPm"
    viewModelScope.launch {
      repository.addReminder(title, targetText, timeLabel, hour, minute)
        .onSuccess { statusMessageFlow.value = "Reminder added ($timeLabel)" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to add reminder" }
    }
  }

  fun toggleReminder(reminder: ChallengeReminder, enabled: Boolean) {
    viewModelScope.launch {
      repository.toggleReminder(reminder, enabled)
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to update reminder" }
    }
  }

  fun deleteReminder(reminderId: String) {
    viewModelScope.launch {
      repository.deleteReminder(reminderId)
        .onSuccess { statusMessageFlow.value = "Reminder removed" }
        .onFailure { statusMessageFlow.value = it.localizedMessage ?: "Failed to remove reminder" }
    }
  }

  fun buildBackupJson(): String? {
    val state = uiState.value as? ChallengeUiState.Ready ?: return null
    return ChallengeBackupCodec.buildBackupJson(
      profile = state.profile,
      habits = state.habits,
      logsByDay = state.logsByDay,
      reminders = state.reminders,
    )
  }

  fun restoreBackupJson(jsonContent: String) {
    viewModelScope.launch {
      isBusyFlow.value = true
      repository.restoreFromBackupJson(jsonContent)
        .onSuccess { count ->
          statusMessageFlow.value = "Restored $count records from JSON backup"
        }
        .onFailure { err ->
          statusMessageFlow.value = "Restore failed: ${err.localizedMessage ?: "Invalid backup JSON"}"
        }
      isBusyFlow.value = false
    }
  }

  fun buildCsvReport(): String? {
    val state = uiState.value as? ChallengeUiState.Ready ?: return null
    return ChallengeBackupCodec.buildCsvReport(state.habits, state.logsByDay)
  }

  fun buildPrintableHtml(): String? {
    val state = uiState.value as? ChallengeUiState.Ready ?: return null
    return ChallengeBackupCodec.buildPrintableHtmlReport(
      habits = state.habits,
      logsByDay = state.logsByDay,
      overallCompletionPercent = state.analytics.overallPercent,
      currentStreak = state.analytics.currentStreak,
      bestStreak = state.analytics.bestStreak,
    )
  }

  fun notifyMessage(message: String) {
    statusMessageFlow.value = message
  }

  private fun computeAnalytics(
    habits: List<Habit>,
    logsByDay: Map<Int, DailyLog>,
    selectedDayNumber: Int,
  ): ChallengeAnalytics {
    val activeHabitIds = habits.map { it.id }.toSet()
    val habitCount = habits.size.coerceAtLeast(1)

    fun dayPercent(day: Int): Pair<Int, Int> {
      val log = logsByDay[day] ?: return 0 to 0
      val done = log.completedHabitIds.count { it in activeHabitIds }
      val pct = ((done.toDouble() / habitCount.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
      return done to pct
    }

    val (selDone, selPct) = dayPercent(selectedDayNumber)

    var totalCompleted = 0
    val dayPercents = (1..ChallengeSchedule.TOTAL_DAYS).map { d ->
      val (done, pct) = dayPercent(d)
      totalCompleted += done
      pct
    }

    // Overall progress across all 30 days
    val overallPct = ((totalCompleted.toDouble() / (ChallengeSchedule.TOTAL_DAYS * habitCount).toDouble()) * 100.0)
      .roundToInt()
      .coerceIn(0, 100)

    // Streak: consecutive days with >= 50% completion (or at least 1 habit if habits < 2)
    var bestStreak = 0
    var running = 0
    dayPercents.forEach { pct ->
      if (pct >= 50) {
        running++
        if (running > bestStreak) bestStreak = running
      } else {
        running = 0
      }
    }
    val activeDay = ChallengeSchedule.currentStatus().activeDayNumber
    var currentStreak = 0
    for (d in activeDay downTo 1) {
      if (dayPercents[d - 1] >= 50) currentStreak++ else if (d < activeDay) break
    }

    // Last 7 days performance chart ending at selectedDayNumber (or Day 7 minimum window)
    val chartEnd = selectedDayNumber.coerceAtLeast(7)
    val chartStart = (chartEnd - 6).coerceAtLeast(1)
    val last7 = (chartStart..chartEnd).map { d ->
      val info = ChallengeSchedule.dayInfoFor(d)
      val (done, pct) = dayPercent(d)
      DayBarStat(
        dayNumber = d,
        shortLabel = "D$d",
        dateLabel = info.shortDateLabel,
        percent = pct,
        completedCount = done,
        totalCount = habitCount,
      )
    }

    // Weekly summaries: W1(1-7), W2(8-14), W3(15-21), W4(22-28), W5(29-30 Final Stretch)
    val weekRanges = listOf(
      1 to (1..7),
      2 to (8..14),
      3 to (15..21),
      4 to (22..28),
      5 to (29..30),
    )
    val weeklySummaries = weekRanges.map { (weekIdx, range) ->
      val startInfo = ChallengeSchedule.dayInfoFor(range.first)
      val endInfo = ChallengeSchedule.dayInfoFor(range.last)
      val daysInWeek = range.count()
      var checks = 0
      var bestDay = range.first
      var bestPct = -1
      var studyH = 0.0
      var fiverrH = 0.0
      var outreach = 0L

      range.forEach { d ->
        val (done, pct) = dayPercent(d)
        checks += done
        if (pct > bestPct) {
          bestPct = pct
          bestDay = d
        }
        val log = logsByDay[d]
        studyH += log?.studyHours ?: 0.0
        fiverrH += log?.fiverrHours ?: 0.0
        outreach += (log?.linkedinConnections ?: 0L) + (log?.instagramDms ?: 0L)
      }

      val possible = daysInWeek * habitCount
      val avgPct = ((checks.toDouble() / possible.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
      val title = if (weekIdx <= 4) "Week $weekIdx" else "Final Sprint (Days 29–30)"

      WeeklySummary(
        weekNumber = weekIdx,
        title = title,
        dateRange = "${startInfo.shortDateLabel} – ${endInfo.shortDateLabel}",
        avgPercent = avgPct,
        completedChecks = checks,
        possibleChecks = possible,
        bestDayLabel = "Day $bestDay ($bestPct%)",
        studyHours = studyH,
        fiverrHours = fiverrH,
        outreachTotal = outreach,
      )
    }

    // Habit consistency
    val habitConsistency = habits.map { h ->
      val completedDays = (1..ChallengeSchedule.TOTAL_DAYS).count { d ->
        logsByDay[d]?.completedHabitIds?.contains(h.id) == true
      }
      val pct = ((completedDays.toDouble() / ChallengeSchedule.TOTAL_DAYS.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
      HabitConsistencyStat(habit = h, completedDays = completedDays, percent = pct)
    }

    // Category mastery
    val categoryMastery = HabitCategory.entries.map { cat ->
      val catHabits = habits.filter { it.category == cat.wireValue }
      val possible = (catHabits.size * ChallengeSchedule.TOTAL_DAYS).coerceAtLeast(1)
      val checks = catHabits.sumOf { h ->
        (1..ChallengeSchedule.TOTAL_DAYS).count { d ->
          logsByDay[d]?.completedHabitIds?.contains(h.id) == true
        }
      }
      val pct = if (catHabits.isEmpty()) 0 else ((checks.toDouble() / possible.toDouble()) * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
      CategoryStat(
        category = cat,
        percent = pct,
        completedChecks = checks,
        possibleChecks = if (catHabits.isEmpty()) 0 else possible,
      )
    }

    val totalStudy = logsByDay.values.sumOf { it.studyHours }
    val totalFiverr = logsByDay.values.sumOf { it.fiverrHours }
    val totalWater = logsByDay.values.sumOf { it.waterGlasses }
    val sleepLogs = logsByDay.values.filter { it.sleepHours > 0.0 }
    val avgSleep = if (sleepLogs.isEmpty()) 0.0 else sleepLogs.sumOf { it.sleepHours } / sleepLogs.size
    val totalLinkedin = logsByDay.values.sumOf { it.linkedinConnections }
    val totalInsta = logsByDay.values.sumOf { it.instagramDms }

    return ChallengeAnalytics(
      overallPercent = overallPct,
      selectedDayPercent = selPct,
      selectedDayCompletedCount = selDone,
      totalHabitsCount = habits.size,
      currentStreak = currentStreak,
      bestStreak = bestStreak,
      totalCompletedChecks = totalCompleted,
      totalStudyHours = totalStudy,
      totalFiverrHours = totalFiverr,
      totalWaterGlasses = totalWater,
      averageSleepHours = avgSleep,
      totalLinkedinConnections = totalLinkedin,
      totalInstagramDms = totalInsta,
      last7Days = last7,
      weeklySummaries = weeklySummaries,
      habitConsistency = habitConsistency,
      categoryMastery = categoryMastery,
    )
  }

  private companion object {
    const val TAG = "ChallengeVM"
  }
}
