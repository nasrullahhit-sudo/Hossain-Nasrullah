package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ThemeMode(val wireValue: String, val label: String) {
  SYSTEM("system", "System"),
  LIGHT("light", "Light"),
  DARK("dark", "Dark");

  companion object {
    fun fromWire(value: String?): ThemeMode =
      entries.firstOrNull { it.wireValue == value } ?: SYSTEM
  }
}

enum class HabitCategory(val wireValue: String, val shortLabel: String) {
  SPIRITUAL("Spiritual", "Spiritual"),
  STUDY("Study & Skill", "Study"),
  HEALTH("Health", "Health"),
  OUTREACH("Outreach & Work", "Work & Outreach"),
  CONTENT("Content & Personal", "Content & Life");

  companion object {
    fun fromWire(value: String?): HabitCategory =
      entries.firstOrNull { it.wireValue == value } ?: CONTENT
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Nasrullah",
  val themeMode: String = ThemeMode.SYSTEM.wireValue,
  val challengeStartDate: String = "2026-11-17",
  val challengeEndDate: String = "2026-12-16",
  val seededDefaults: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Habit(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val category: String = HabitCategory.CONTENT.wireValue,
  val targetDetail: String = "",
  val sortOrder: Long = 0L,
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DailyLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dayNumber: Long = 1L,
  val dateIso: String = "2026-11-17",
  val completedHabitIds: List<String> = emptyList(),
  val totalHabitsSnapshot: Long = 21L,
  val notes: String = "",
  val studyHours: Double = 0.0,
  val fiverrHours: Double = 0.0,
  val waterGlasses: Long = 0L,
  val sleepHours: Double = 0.0,
  val linkedinConnections: Long = 0L,
  val instagramDms: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChallengeReminder(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val targetText: String = "",
  val timeLabel: String = "08:00 PM",
  val hour: Long = 20L,
  val minute: Long = 0L,
  val enabled: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)
