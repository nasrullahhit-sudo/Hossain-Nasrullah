package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.HabitCategory
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureUserSeeded_authenticatedOwner_createsProfile21HabitsAndReminders() = runBlocking {
    val uid = signInTestUser("nasrullah_${UUID.randomUUID()}@test.com")
    val repo = ChallengeRepository(firestore)

    val seedResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserSeeded("Nasrullah") }
    assertTrue("Seed failed: ${seedResult.exceptionOrNull()}", seedResult.isSuccess)

    val habits = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeHabits(uid).first { it.size == 21 }
    }
    assertEquals(21, habits.size)
    assertEquals("Fajr Prayer", habits.first().title)
    assertEquals("Personal Time — 40 Minutes", habits.last().title)

    val reminders = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeReminders(uid).first { it.isNotEmpty() }
    }
    assertTrue(reminders.isNotEmpty())
  }

  @Test
  fun saveDailyLog_authenticatedOwner_createsAndUpdatesDailyRecord() = runBlocking {
    val uid = signInTestUser("nasrullah_log_${UUID.randomUUID()}@test.com")
    val repo = ChallengeRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserSeeded("Nasrullah").getOrThrow() }

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveDailyLog(
        dayNumber = 1,
        existingLog = null,
        completedHabitIds = listOf("habit_01", "habit_13"),
        totalHabitsSnapshot = 21,
        notes = "Day 1 completed cleanly.",
        studyHours = 3.0,
        fiverrHours = 7.5,
        waterGlasses = 10,
        sleepHours = 8.0,
        linkedinConnections = 10,
        instagramDms = 20,
      )
    }
    assertTrue("Create log failed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

    val logs = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeDailyLogs(uid).first { it.any { log -> log.dayNumber == 1L } }
    }
    val day1 = logs.first { it.dayNumber == 1L }
    assertEquals("2026-11-17", day1.dateIso)
    assertEquals(2, day1.completedHabitIds.size)

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveDailyLog(
        dayNumber = 1,
        existingLog = day1,
        completedHabitIds = listOf("habit_01", "habit_13", "habit_14"),
        totalHabitsSnapshot = 21,
        notes = "Updated Day 1 reflection.",
        studyHours = 3.5,
        fiverrHours = 8.0,
        waterGlasses = 10,
        sleepHours = 8.0,
        linkedinConnections = 12,
        instagramDms = 20,
      )
    }
    assertTrue("Update log failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
  }

  @Test
  fun addAndReorderHabits_authenticatedOwner_persistsOrder() = runBlocking {
    val uid = signInTestUser("nasrullah_habit_${UUID.randomUUID()}@test.com")
    val repo = ChallengeRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserSeeded("Nasrullah").getOrThrow() }

    val addedId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.addHabit("Review Portfolio", HabitCategory.OUTREACH, "15 min", 21L).getOrThrow()
    }
    assertTrue(addedId.isNotBlank())

    val habits = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeHabits(uid).first { list -> list.any { it.id == addedId } }
    }
    val swapped = habits.toMutableList()
    val last = swapped.removeAt(swapped.lastIndex)
    swapped.add(0, last)
    val reorderRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.reorderHabits(swapped) }
    assertTrue(reorderRes.isSuccess)
  }

  @Test
  fun observeHabits_crossUserAndUnauthenticated_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val repo = ChallengeRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserSeeded("Alice").getOrThrow() }

    signInTestUser("bob_${UUID.randomUUID()}@test.com")
    var bobDenied: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeHabits(aliceUid).first() }
    } catch (e: Exception) {
      bobDenied = e.unwrapFirestoreException()
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobDenied?.code)

    auth.signOut()
    var unauthDenied: FirebaseFirestoreException? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeHabits(aliceUid).first() }
    } catch (e: Exception) {
      unauthDenied = e.unwrapFirestoreException()
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, unauthDenied?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
