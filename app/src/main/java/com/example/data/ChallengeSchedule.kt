package com.example.data

import com.example.data.model.ChallengeReminder
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ChallengeSchedule {
  const val TOTAL_DAYS = 30
  const val START_DATE_ISO = "2026-11-17"
  const val END_DATE_ISO = "2026-12-16"
  const val START_DISPLAY = "17 Nov 2026"
  const val END_DISPLAY = "16 Dec 2026"

  private const val START_YEAR = 2026
  private const val START_MONTH_ZERO_BASED = Calendar.NOVEMBER // 10
  private const val START_DAY = 17

  data class ChallengeDayInfo(
    val dayNumber: Int,
    val dayKey: String,
    val dateIso: String,
    val shortDateLabel: String,
    val fullDateLabel: String,
    val dayOfWeekLabel: String,
    val weekIndex: Int,
  )

  data class ChallengeStatusSummary(
    val phaseLabel: String,
    val countdownLabel: String,
    val activeDayNumber: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
  )

  val allDays: List<ChallengeDayInfo> by lazy {
    val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val shortFmt = SimpleDateFormat("MMM d", Locale.US)
    val fullFmt = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
    val dowFmt = SimpleDateFormat("EEE", Locale.US)

    (1..TOTAL_DAYS).map { day ->
      val cal = Calendar.getInstance().apply {
        clear()
        set(START_YEAR, START_MONTH_ZERO_BASED, START_DAY, 12, 0, 0)
        add(Calendar.DAY_OF_MONTH, day - 1)
      }
      val date = cal.time
      ChallengeDayInfo(
        dayNumber = day,
        dayKey = dayKeyFor(day),
        dateIso = isoFmt.format(date),
        shortDateLabel = shortFmt.format(date),
        fullDateLabel = fullFmt.format(date),
        dayOfWeekLabel = dowFmt.format(date),
        weekIndex = ((day - 1) / 7) + 1, // Weeks 1..4 (Days 1-28), Week 5 = Final Days 29-30
      )
    }
  }

  fun dayKeyFor(dayNumber: Int): String {
    val clamped = dayNumber.coerceIn(1, TOTAL_DAYS)
    return "day_${clamped.toString().padStart(2, '0')}"
  }

  fun dayInfoFor(dayNumber: Int): ChallengeDayInfo =
    allDays[dayNumber.coerceIn(1, TOTAL_DAYS) - 1]

  fun currentStatus(nowMillis: Long = System.currentTimeMillis()): ChallengeStatusSummary {
    val todayCal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startCal = Calendar.getInstance().apply {
      clear()
      set(START_YEAR, START_MONTH_ZERO_BASED, START_DAY, 0, 0, 0)
    }
    val millisPerDay = 86_400_000L
    // Use timezone offset adjustment to avoid DST 23h/25h off-by-one
    val todayMidnightUtc = todayCal.timeInMillis + todayCal.timeZone.getOffset(todayCal.timeInMillis)
    val startMidnightUtc = startCal.timeInMillis + startCal.timeZone.getOffset(startCal.timeInMillis)
    val diffDays = ((todayMidnightUtc - startMidnightUtc) / millisPerDay).toInt()

    return when {
      diffDays < 0 -> {
        val daysUntil = -diffDays
        ChallengeStatusSummary(
          phaseLabel = "Starting Soon",
          countdownLabel = "Starts in $daysUntil day${if (daysUntil == 1) "" else "s"} • $START_DISPLAY",
          activeDayNumber = 1,
          elapsedDays = 0,
          remainingDays = TOTAL_DAYS,
        )
      }
      diffDays in 0 until TOTAL_DAYS -> {
        val dayNum = diffDays + 1
        val remaining = TOTAL_DAYS - dayNum
        ChallengeStatusSummary(
          phaseLabel = "Day $dayNum of $TOTAL_DAYS",
          countdownLabel = if (remaining == 0) {
            "Final Day • Ends today ($END_DISPLAY)"
          } else {
            "$remaining day${if (remaining == 1) "" else "s"} remaining • Ends $END_DISPLAY"
          },
          activeDayNumber = dayNum,
          elapsedDays = dayNum,
          remainingDays = remaining,
        )
      }
      else -> {
        ChallengeStatusSummary(
          phaseLabel = "Challenge Complete",
          countdownLabel = "30 / 30 days completed ($START_DISPLAY – $END_DISPLAY)",
          activeDayNumber = TOTAL_DAYS,
          elapsedDays = TOTAL_DAYS,
          remainingDays = 0,
        )
      }
    }
  }

  /** Bangladesh Standard Time (UTC+6) helper string for LinkedIn 8 PM reminder. */
  fun bangladeshTimeSummary(): String {
    val bdCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka"))
    val fmt = SimpleDateFormat("hh:mm a", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("Asia/Dhaka")
    }
    return "${fmt.format(bdCal.time)} BD"
  }
}

object ChallengeDefaults {
  data class DefaultHabitSpec(
    val docId: String,
    val title: String,
    val category: HabitCategory,
    val targetDetail: String,
  )

  val defaultHabitSpecs = listOf(
    DefaultHabitSpec("habit_01", "Fajr Prayer", HabitCategory.SPIRITUAL, "Daily Dawn Prayer"),
    DefaultHabitSpec("habit_02", "Zuhr Prayer", HabitCategory.SPIRITUAL, "Daily Noon Prayer"),
    DefaultHabitSpec("habit_03", "Asr Prayer", HabitCategory.SPIRITUAL, "Daily Afternoon Prayer"),
    DefaultHabitSpec("habit_04", "Maghrib Prayer", HabitCategory.SPIRITUAL, "Daily Sunset Prayer"),
    DefaultHabitSpec("habit_05", "Isha Prayer", HabitCategory.SPIRITUAL, "Daily Night Prayer"),
    DefaultHabitSpec("habit_06", "Quran and Hadith Study", HabitCategory.SPIRITUAL, "Daily Reflection"),
    DefaultHabitSpec("habit_07", "CST Self-Study — 3 Hours", HabitCategory.STUDY, "3 Hours Focused Study"),
    DefaultHabitSpec("habit_08", "English Course — 20 Minutes", HabitCategory.STUDY, "20 Minutes Lesson"),
    DefaultHabitSpec("habit_09", "Typing Practice — 15 Minutes", HabitCategory.STUDY, "15 Minutes Speed & Accuracy"),
    DefaultHabitSpec("habit_10", "Exercise — 20 Minutes", HabitCategory.HEALTH, "20 Minutes Workout"),
    DefaultHabitSpec("habit_11", "Drink 10 Glasses of Water", HabitCategory.HEALTH, "10 Glasses Hydration"),
    DefaultHabitSpec("habit_12", "Sleep for 8 Hours", HabitCategory.HEALTH, "8 Hours Recovery"),
    DefaultHabitSpec("habit_13", "Fiverr Online Availability — 7–8 Hours", HabitCategory.OUTREACH, "7–8 Hours Active"),
    DefaultHabitSpec("habit_14", "LinkedIn Video Post — 8 PM Bangladesh Time", HabitCategory.OUTREACH, "8:00 PM BD Time (UTC+6)"),
    DefaultHabitSpec("habit_15", "Make 10 New LinkedIn Connections", HabitCategory.OUTREACH, "10 Connections Daily"),
    DefaultHabitSpec("habit_16", "Send 20 Personalized Instagram Outreach DMs", HabitCategory.OUTREACH, "20 Personalized DMs"),
    DefaultHabitSpec("habit_17", "Content Research — 15–20 Minutes", HabitCategory.CONTENT, "15–20 Minutes Ideation"),
    DefaultHabitSpec("habit_18", "Complete Planned YouTube Uploads / Scheduling", HabitCategory.CONTENT, "Upload & Schedule"),
    DefaultHabitSpec("habit_19", "Follow YouTube Batch Production Plan", HabitCategory.CONTENT, "Batch Filming / Editing"),
    DefaultHabitSpec("habit_20", "AI Influencer Batch Production on Planned Days", HabitCategory.CONTENT, "Planned Batch Days"),
    DefaultHabitSpec("habit_21", "Personal Time — 40 Minutes", HabitCategory.CONTENT, "40 Minutes Unwind"),
  )

  fun defaultHabits(userId: String): List<Habit> =
    defaultHabitSpecs.mapIndexed { index, spec ->
      Habit(
        id = spec.docId,
        userId = userId,
        title = spec.title,
        category = spec.category.wireValue,
        targetDetail = spec.targetDetail,
        sortOrder = index.toLong(),
        isActive = true,
      )
    }

  fun defaultReminders(userId: String): List<ChallengeReminder> = listOf(
    ChallengeReminder(
      id = "rem_fajr",
      userId = userId,
      title = "Fajr & Morning Launch",
      targetText = "Fajr Prayer + Quran & Hadith Study",
      timeLabel = "05:15 AM",
      hour = 5,
      minute = 15,
      enabled = true,
    ),
    ChallengeReminder(
      id = "rem_cst_fiverr",
      userId = userId,
      title = "CST Study & Fiverr Online Shift",
      targetText = "3h CST Study + 7–8h Fiverr Online Availability",
      timeLabel = "09:00 AM",
      hour = 9,
      minute = 0,
      enabled = true,
    ),
    ChallengeReminder(
      id = "rem_outreach",
      userId = userId,
      title = "Instagram & LinkedIn Outreach",
      targetText = "20 Instagram DMs + 10 LinkedIn Connections",
      timeLabel = "04:00 PM",
      hour = 16,
      minute = 0,
      enabled = true,
    ),
    ChallengeReminder(
      id = "rem_linkedin_post",
      userId = userId,
      title = "LinkedIn Video Post",
      targetText = "Publish at 8:00 PM Bangladesh Time",
      timeLabel = "08:00 PM BD",
      hour = 20,
      minute = 0,
      enabled = true,
    ),
    ChallengeReminder(
      id = "rem_evening_wrap",
      userId = userId,
      title = "Evening Wrap-Up & 8h Sleep Target",
      targetText = "10 Glasses Water + Daily Notes + Sleep 8h",
      timeLabel = "10:30 PM",
      hour = 22,
      minute = 30,
      enabled = true,
    ),
  )
}
