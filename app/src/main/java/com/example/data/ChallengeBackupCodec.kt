package com.example.data

import com.example.data.model.ChallengeReminder
import com.example.data.model.DailyLog
import com.example.data.model.Habit
import com.example.data.model.UserProfile
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

object ChallengeBackupCodec {

  fun buildBackupJson(
    profile: UserProfile?,
    habits: List<Habit>,
    logsByDay: Map<Int, DailyLog>,
    reminders: List<ChallengeReminder>,
  ): String {
    val root = JSONObject()
    root.put("app", "Nasrullah's 30-Day Challenge")
    root.put("version", 1)
    root.put("challengeStartDate", ChallengeSchedule.START_DATE_ISO)
    root.put("challengeEndDate", ChallengeSchedule.END_DATE_ISO)
    root.put("exportedAtMillis", System.currentTimeMillis())

    val profileObj = JSONObject().apply {
      put("displayName", profile?.displayName ?: "Nasrullah")
      put("themeMode", profile?.themeMode ?: "system")
    }
    root.put("profile", profileObj)

    val habitsArr = JSONArray()
    habits.sortedBy { it.sortOrder }.forEach { h ->
      habitsArr.put(
        JSONObject().apply {
          put("id", h.id)
          put("title", h.title)
          put("category", h.category)
          put("targetDetail", h.targetDetail)
          put("sortOrder", h.sortOrder)
          put("isActive", h.isActive)
        }
      )
    }
    root.put("habits", habitsArr)

    val logsArr = JSONArray()
    (1..ChallengeSchedule.TOTAL_DAYS).forEach { day ->
      val log = logsByDay[day] ?: return@forEach
      val completedArr = JSONArray().apply {
        log.completedHabitIds.forEach { put(it) }
      }
      logsArr.put(
        JSONObject().apply {
          put("dayNumber", log.dayNumber)
          put("dateIso", log.dateIso)
          put("completedHabitIds", completedArr)
          put("totalHabitsSnapshot", log.totalHabitsSnapshot)
          put("notes", log.notes)
          put("studyHours", log.studyHours)
          put("fiverrHours", log.fiverrHours)
          put("waterGlasses", log.waterGlasses)
          put("sleepHours", log.sleepHours)
          put("linkedinConnections", log.linkedinConnections)
          put("instagramDms", log.instagramDms)
        }
      )
    }
    root.put("dailyLogs", logsArr)

    val remindersArr = JSONArray()
    reminders.forEach { r ->
      remindersArr.put(
        JSONObject().apply {
          put("id", r.id)
          put("title", r.title)
          put("targetText", r.targetText)
          put("timeLabel", r.timeLabel)
          put("hour", r.hour)
          put("minute", r.minute)
          put("enabled", r.enabled)
        }
      )
    }
    root.put("reminders", remindersArr)

    return root.toString(2)
  }

  fun buildCsvReport(
    habits: List<Habit>,
    logsByDay: Map<Int, DailyLog>,
  ): String {
    val activeHabitCount = habits.count { it.isActive }.coerceAtLeast(1)
    val sb = StringBuilder()
    sb.appendLine(
      "Day,Date,Week,CompletedHabits,TotalHabits,CompletionPercent,StudyHours,FiverrHours,WaterGlasses,SleepHours,LinkedInConnections,InstagramDMs,Notes"
    )
    ChallengeSchedule.allDays.forEach { dayInfo ->
      val log = logsByDay[dayInfo.dayNumber]
      val completed = log?.completedHabitIds?.size ?: 0
      val total = (log?.totalHabitsSnapshot?.toInt() ?: activeHabitCount).coerceAtLeast(activeHabitCount)
      val pct = ((completed.toDouble() / total.toDouble()) * 100.0).coerceIn(0.0, 100.0)
      val safeNotes = (log?.notes ?: "").replace("\"", "\"\"").replace("\n", " ")
      sb.appendLine(
        listOf(
          dayInfo.dayNumber.toString(),
          dayInfo.dateIso,
          "Week ${dayInfo.weekIndex}",
          completed.toString(),
          total.toString(),
          String.format(Locale.US, "%.1f", pct),
          String.format(Locale.US, "%.1f", log?.studyHours ?: 0.0),
          String.format(Locale.US, "%.1f", log?.fiverrHours ?: 0.0),
          (log?.waterGlasses ?: 0L).toString(),
          String.format(Locale.US, "%.1f", log?.sleepHours ?: 0.0),
          (log?.linkedinConnections ?: 0L).toString(),
          (log?.instagramDms ?: 0L).toString(),
          "\"$safeNotes\"",
        ).joinToString(",")
      )
    }
    return sb.toString()
  }

  fun buildPrintableHtmlReport(
    habits: List<Habit>,
    logsByDay: Map<Int, DailyLog>,
    overallCompletionPercent: Int,
    currentStreak: Int,
    bestStreak: Int,
  ): String {
    val totalHabits = habits.size.coerceAtLeast(1)
    val totalCompletedChecks = (1..ChallengeSchedule.TOTAL_DAYS).sumOf { day ->
      logsByDay[day]?.completedHabitIds?.size ?: 0
    }
    val totalStudyHours = logsByDay.values.sumOf { it.studyHours }
    val totalFiverrHours = logsByDay.values.sumOf { it.fiverrHours }
    val totalLinkedin = logsByDay.values.sumOf { it.linkedinConnections }
    val totalInstaDms = logsByDay.values.sumOf { it.instagramDms }

    val dayRows = ChallengeSchedule.allDays.joinToString("\n") { dayInfo ->
      val log = logsByDay[dayInfo.dayNumber]
      val done = log?.completedHabitIds?.size ?: 0
      val pct = ((done * 100.0) / totalHabits).toInt().coerceIn(0, 100)
      val escapedNote = escapeHtml(log?.notes ?: "")
      """
      <tr>
        <td>Day ${dayInfo.dayNumber}</td>
        <td>${dayInfo.fullDateLabel}</td>
        <td>$done / $totalHabits ($pct%)</td>
        <td>${String.format(Locale.US, "%.1f", log?.studyHours ?: 0.0)}h</td>
        <td>${String.format(Locale.US, "%.1f", log?.fiverrHours ?: 0.0)}h</td>
        <td>${log?.linkedinConnections ?: 0} / ${log?.instagramDms ?: 0}</td>
        <td>$escapedNote</td>
      </tr>
      """.trimIndent()
    }

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8"/>
        <style>
          body { font-family: sans-serif; color: #0F172A; margin: 24px; line-height: 1.45; }
          .header { border-bottom: 3px solid #0F766E; padding-bottom: 12px; margin-bottom: 18px; }
          h1 { margin: 0; color: #0F766E; font-size: 22px; }
          .subtitle { color: #D97706; font-weight: bold; margin-top: 4px; font-size: 13px; }
          .meta { color: #475569; font-size: 12px; margin-top: 6px; }
          .stats { display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
          .stat-box { border: 1px solid #CBD5E1; border-radius: 8px; padding: 10px 14px; min-width: 120px; background: #F8FAFC; }
          .stat-val { font-size: 18px; font-weight: bold; color: #0F766E; }
          .stat-lbl { font-size: 11px; color: #64748B; }
          table { width: 100%; border-collapse: collapse; font-size: 11px; }
          th, td { border: 1px solid #CBD5E1; padding: 6px 8px; text-align: left; }
          th { background-color: #0F766E; color: #FFFFFF; }
          tr:nth-child(even) { background-color: #F8FAFC; }
          .footer { margin-top: 24px; text-align: center; font-size: 11px; color: #64748B; border-top: 1px solid #E2E8F0; padding-top: 10px; }
        </style>
      </head>
      <body>
        <div class="header">
          <h1>Nasrullah's 30-Day Challenge — Final &amp; Weekly Report</h1>
          <div class="subtitle">Discipline is Power | Keep Going!</div>
          <div class="meta">Challenge Window: 17 November 2026 – 16 December 2026 (30 Days)</div>
        </div>
        <div class="stats">
          <div class="stat-box"><div class="stat-val">$overallCompletionPercent%</div><div class="stat-lbl">Overall Progress</div></div>
          <div class="stat-box"><div class="stat-val">$currentStreak d / $bestStreak d</div><div class="stat-lbl">Current / Best Streak</div></div>
          <div class="stat-box"><div class="stat-val">$totalCompletedChecks</div><div class="stat-lbl">Total Habits Checked</div></div>
          <div class="stat-box"><div class="stat-val">${String.format(Locale.US, "%.1f", totalStudyHours)}h</div><div class="stat-lbl">CST &amp; Study Hours</div></div>
          <div class="stat-box"><div class="stat-val">${String.format(Locale.US, "%.1f", totalFiverrHours)}h</div><div class="stat-lbl">Fiverr Online Hours</div></div>
          <div class="stat-box"><div class="stat-val">$totalLinkedin / $totalInstaDms</div><div class="stat-lbl">LinkedIn / IG DMs</div></div>
        </div>
        <table>
          <thead>
            <tr>
              <th>Day</th>
              <th>Date</th>
              <th>Completion</th>
              <th>Study</th>
              <th>Fiverr</th>
              <th>LI / IG</th>
              <th>Daily Notes</th>
            </tr>
          </thead>
          <tbody>
            $dayRows
          </tbody>
        </table>
        <div class="footer">Designed for Nasrullah • 30-Day Discipline Tracker</div>
      </body>
      </html>
    """.trimIndent()
  }

  private fun escapeHtml(text: String): String =
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
}
