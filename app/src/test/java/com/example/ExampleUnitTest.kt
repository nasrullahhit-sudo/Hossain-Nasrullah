package com.example

import com.example.data.ChallengeDefaults
import com.example.data.ChallengeSchedule
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun challengeSchedule_isExactly30DaysFromNov17ToDec16() {
    assertEquals(30, ChallengeSchedule.allDays.size)
    assertEquals("2026-11-17", ChallengeSchedule.allDays.first().dateIso)
    assertEquals("2026-12-16", ChallengeSchedule.allDays.last().dateIso)
  }

  @Test
  fun challengeDefaults_has21HabitsStartingWithFajr() {
    val habits = ChallengeDefaults.defaultHabits("test_uid")
    assertEquals(21, habits.size)
    assertEquals("Fajr Prayer", habits[0].title)
    assertEquals("Fiverr Online Availability — 7–8 Hours", habits[12].title)
    assertEquals("LinkedIn Video Post — 8 PM Bangladesh Time", habits[13].title)
    assertEquals("Personal Time — 40 Minutes", habits[20].title)
  }
}
