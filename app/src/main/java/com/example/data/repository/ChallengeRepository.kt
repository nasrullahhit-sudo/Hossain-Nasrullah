package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.ChallengeDefaults
import com.example.data.ChallengeSchedule
import com.example.data.model.ChallengeReminder
import com.example.data.model.DailyLog
import com.example.data.model.Habit
import com.example.data.model.HabitCategory
import com.example.data.model.ThemeMode
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class ChallengeRepository(private val db: FirebaseFirestore) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)
  private fun habitsCol(uid: String) = userDoc(uid).collection("habits")
  private fun dailyLogsCol(uid: String) = userDoc(uid).collection("dailyLogs")
  private fun remindersCol(uid: String) = userDoc(uid).collection("reminders")

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val docRef = userDoc(uid)
    emitAll(
      docRef.snapshots()
        .map { snapshot ->
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowError(error, OperationType.GET, docRef.path, uid)
          throw error
        }
    )
  }

  fun observeHabits(uid: String): Flow<List<Habit>> = flow {
    val colRef = habitsCol(uid)
    emitAll(
      colRef.orderBy("sortOrder", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(Habit::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  fun observeDailyLogs(uid: String): Flow<List<DailyLog>> = flow {
    val colRef = dailyLogsCol(uid)
    emitAll(
      colRef.orderBy("dayNumber", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(DailyLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  fun observeReminders(uid: String): Flow<List<ChallengeReminder>> = flow {
    val colRef = remindersCol(uid)
    emitAll(
      colRef.orderBy("hour", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            ChallengeReminder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, colRef.path, uid)
          throw error
        }
    )
  }

  suspend fun ensureUserSeeded(
    displayName: String = "Nasrullah",
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val profileRef = userDoc(uid)
    val profileSnap = try {
      profileRef.get().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, profileRef.path)
      throw e
    }

    val existingProfile = profileSnap.toObject(UserProfile::class.java)
    if (existingProfile == null) {
      val cleanName = displayName.trim().ifEmpty { "Nasrullah" }.take(80)
      val profilePayload = mapOf(
        "userId" to uid,
        "displayName" to cleanName,
        "themeMode" to initialTheme.wireValue,
        "challengeStartDate" to ChallengeSchedule.START_DATE_ISO,
        "challengeEndDate" to ChallengeSchedule.END_DATE_ISO,
        "seededDefaults" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      val batch = db.batch()
      batch.set(profileRef, profilePayload)

      ChallengeDefaults.defaultHabits(uid).forEach { habit ->
        val habitRef = habitsCol(uid).document(habit.id)
        batch.set(
          habitRef,
          mapOf(
            "userId" to uid,
            "title" to habit.title,
            "category" to habit.category,
            "targetDetail" to habit.targetDetail,
            "sortOrder" to habit.sortOrder,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      ChallengeDefaults.defaultReminders(uid).forEach { rem ->
        val remRef = remindersCol(uid).document(rem.id)
        batch.set(
          remRef,
          mapOf(
            "userId" to uid,
            "title" to rem.title,
            "targetText" to rem.targetText,
            "timeLabel" to rem.timeLabel,
            "hour" to rem.hour,
            "minute" to rem.minute,
            "enabled" to rem.enabled,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      try {
        batch.commit().await()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, profileRef.path)
        throw e
      }
    } else if (!existingProfile.seededDefaults) {
      val existingHabits = habitsCol(uid).get().await()
      val batch = db.batch()
      if (existingHabits.isEmpty) {
        ChallengeDefaults.defaultHabits(uid).forEach { habit ->
          batch.set(
            habitsCol(uid).document(habit.id),
            mapOf(
              "userId" to uid,
              "title" to habit.title,
              "category" to habit.category,
              "targetDetail" to habit.targetDetail,
              "sortOrder" to habit.sortOrder,
              "isActive" to true,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }
      batch.update(
        profileRef,
        mapOf(
          "seededDefaults" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.commit().await()
    }
  }

  suspend fun updateThemeMode(themeMode: ThemeMode): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = userDoc(uid)
    try {
      ref.update(
        mapOf(
          "themeMode" to themeMode.wireValue,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, ref.path)
      throw e
    }
  }

  suspend fun addHabit(
    title: String,
    category: HabitCategory,
    targetDetail: String,
    nextSortOrder: Long,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "habit_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = habitsCol(uid).document(docId)
    val payload = mapOf(
      "userId" to uid,
      "title" to title.trim().take(160),
      "category" to category.wireValue,
      "targetDetail" to targetDetail.trim().take(160),
      "sortOrder" to nextSortOrder.coerceIn(0L, 1000L),
      "isActive" to true,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateHabit(
    habitId: String,
    title: String,
    category: HabitCategory,
    targetDetail: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = habitsCol(uid).document(habitId)
    try {
      docRef.update(
        mapOf(
          "title" to title.trim().take(160),
          "category" to category.wireValue,
          "targetDetail" to targetDetail.trim().take(160),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteHabit(habitId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = habitsCol(uid).document(habitId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun reorderHabits(orderedHabits: List<Habit>): Result<Unit> = runCatching {
    val uid = requireUserId()
    val batch = db.batch()
    orderedHabits.forEachIndexed { index, habit ->
      val docRef = habitsCol(uid).document(habit.id)
      batch.update(
        docRef,
        mapOf(
          "sortOrder" to index.toLong().coerceIn(0L, 1000L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, habitsCol(uid).path)
      throw e
    }
  }

  suspend fun resetDefaultHabits(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val existing = habitsCol(uid).get().await()
    val batch = db.batch()
    existing.documents.forEach { doc -> batch.delete(doc.reference) }
    ChallengeDefaults.defaultHabits(uid).forEach { habit ->
      batch.set(
        habitsCol(uid).document(habit.id),
        mapOf(
          "userId" to uid,
          "title" to habit.title,
          "category" to habit.category,
          "targetDetail" to habit.targetDetail,
          "sortOrder" to habit.sortOrder,
          "isActive" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, habitsCol(uid).path)
      throw e
    }
  }

  suspend fun saveDailyLog(
    dayNumber: Int,
    existingLog: DailyLog?,
    completedHabitIds: List<String>,
    totalHabitsSnapshot: Int,
    notes: String,
    studyHours: Double,
    fiverrHours: Double,
    waterGlasses: Int,
    sleepHours: Double,
    linkedinConnections: Int,
    instagramDms: Int,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val dayInfo = ChallengeSchedule.dayInfoFor(dayNumber)
    val docRef = dailyLogsCol(uid).document(dayInfo.dayKey)

    val boundedCompleted = completedHabitIds.distinct().take(100)
    val boundedTotal = totalHabitsSnapshot.coerceIn(0, 100).toLong()
    val cleanNotes = notes.take(4000)
    val cleanStudy = studyHours.coerceIn(0.0, 24.0)
    val cleanFiverr = fiverrHours.coerceIn(0.0, 24.0)
    val cleanWater = waterGlasses.coerceIn(0, 50).toLong()
    val cleanSleep = sleepHours.coerceIn(0.0, 24.0)
    val cleanLinkedin = linkedinConnections.coerceIn(0, 500).toLong()
    val cleanInsta = instagramDms.coerceIn(0, 500).toLong()

    val updateFields = mapOf(
      "completedHabitIds" to boundedCompleted,
      "totalHabitsSnapshot" to boundedTotal,
      "notes" to cleanNotes,
      "studyHours" to cleanStudy,
      "fiverrHours" to cleanFiverr,
      "waterGlasses" to cleanWater,
      "sleepHours" to cleanSleep,
      "linkedinConnections" to cleanLinkedin,
      "instagramDms" to cleanInsta,
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    try {
      val needsCheck = existingLog?.createdAt == null
      val existsOnServer = if (needsCheck) docRef.get().await().exists() else true
      if (existsOnServer) {
        docRef.update(updateFields).await()
      } else {
        val createFields = mapOf(
          "userId" to uid,
          "dayNumber" to dayInfo.dayNumber.toLong(),
          "dateIso" to dayInfo.dateIso,
          "completedHabitIds" to boundedCompleted,
          "totalHabitsSnapshot" to boundedTotal,
          "notes" to cleanNotes,
          "studyHours" to cleanStudy,
          "fiverrHours" to cleanFiverr,
          "waterGlasses" to cleanWater,
          "sleepHours" to cleanSleep,
          "linkedinConnections" to cleanLinkedin,
          "instagramDms" to cleanInsta,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        docRef.set(createFields).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun addReminder(
    title: String,
    targetText: String,
    timeLabel: String,
    hour: Int,
    minute: Int,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "rem_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = remindersCol(uid).document(docId)
    val payload = mapOf(
      "userId" to uid,
      "title" to title.trim().take(120),
      "targetText" to targetText.trim().take(120),
      "timeLabel" to timeLabel.trim().take(40),
      "hour" to hour.coerceIn(0, 23).toLong(),
      "minute" to minute.coerceIn(0, 59).toLong(),
      "enabled" to true,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun toggleReminder(reminder: ChallengeReminder, enabled: Boolean): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = remindersCol(uid).document(reminder.id)
    try {
      docRef.update(
        mapOf(
          "enabled" to enabled,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteReminder(reminderId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = remindersCol(uid).document(reminderId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun restoreFromBackupJson(jsonContent: String): Result<Int> = runCatching {
    val uid = requireUserId()
    val root = JSONObject(jsonContent)

    val habitsArray = root.optJSONArray("habits") ?: JSONArray()
    val logsArray = root.optJSONArray("dailyLogs") ?: JSONArray()
    val remindersArray = root.optJSONArray("reminders") ?: JSONArray()

    // Clear current habits & reminders & logs before writing restored docs in batches
    val existingHabits = habitsCol(uid).get().await()
    val existingLogs = dailyLogsCol(uid).get().await()
    val existingReminders = remindersCol(uid).get().await()

    val batch = db.batch()
    existingHabits.documents.forEach { batch.delete(it.reference) }
    existingLogs.documents.forEach { batch.delete(it.reference) }
    existingReminders.documents.forEach { batch.delete(it.reference) }

    var restoredCount = 0

    for (i in 0 until habitsArray.length()) {
      val obj = habitsArray.getJSONObject(i)
      val rawId = obj.optString("id").ifBlank { "habit_${i + 1}" }
      val safeId = sanitizeDocId(rawId, "habit_${i + 1}")
      val cat = HabitCategory.fromWire(obj.optString("category")).wireValue
      batch.set(
        habitsCol(uid).document(safeId),
        mapOf(
          "userId" to uid,
          "title" to obj.optString("title", "Habit ${i + 1}").take(160).ifBlank { "Habit ${i + 1}" },
          "category" to cat,
          "targetDetail" to obj.optString("targetDetail", "").take(160),
          "sortOrder" to obj.optLong("sortOrder", i.toLong()).coerceIn(0L, 1000L),
          "isActive" to obj.optBoolean("isActive", true),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      restoredCount++
    }

    for (i in 0 until logsArray.length()) {
      val obj = logsArray.getJSONObject(i)
      val dayNum = obj.optInt("dayNumber", i + 1).coerceIn(1, ChallengeSchedule.TOTAL_DAYS)
      val dayInfo = ChallengeSchedule.dayInfoFor(dayNum)
      val completedJson = obj.optJSONArray("completedHabitIds") ?: JSONArray()
      val completedIds = buildList {
        for (j in 0 until completedJson.length()) {
          val idStr = completedJson.optString(j)
          if (idStr.isNotBlank()) add(idStr)
        }
      }.distinct().take(100)

      batch.set(
        dailyLogsCol(uid).document(dayInfo.dayKey),
        mapOf(
          "userId" to uid,
          "dayNumber" to dayInfo.dayNumber.toLong(),
          "dateIso" to dayInfo.dateIso,
          "completedHabitIds" to completedIds,
          "totalHabitsSnapshot" to obj.optLong("totalHabitsSnapshot", 21L).coerceIn(0L, 100L),
          "notes" to obj.optString("notes", "").take(4000),
          "studyHours" to obj.optDouble("studyHours", 0.0).coerceIn(0.0, 24.0),
          "fiverrHours" to obj.optDouble("fiverrHours", 0.0).coerceIn(0.0, 24.0),
          "waterGlasses" to obj.optLong("waterGlasses", 0L).coerceIn(0L, 50L),
          "sleepHours" to obj.optDouble("sleepHours", 0.0).coerceIn(0.0, 24.0),
          "linkedinConnections" to obj.optLong("linkedinConnections", 0L).coerceIn(0L, 500L),
          "instagramDms" to obj.optLong("instagramDms", 0L).coerceIn(0L, 500L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      restoredCount++
    }

    for (i in 0 until remindersArray.length()) {
      val obj = remindersArray.getJSONObject(i)
      val safeId = sanitizeDocId(obj.optString("id"), "rem_${i + 1}")
      batch.set(
        remindersCol(uid).document(safeId),
        mapOf(
          "userId" to uid,
          "title" to obj.optString("title", "Reminder").take(120).ifBlank { "Reminder" },
          "targetText" to obj.optString("targetText", "").take(120),
          "timeLabel" to obj.optString("timeLabel", "08:00 PM").take(40).ifBlank { "08:00 PM" },
          "hour" to obj.optLong("hour", 20L).coerceIn(0L, 23L),
          "minute" to obj.optLong("minute", 0L).coerceIn(0L, 59L),
          "enabled" to obj.optBoolean("enabled", true),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      restoredCount++
    }

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userDoc(uid).path)
      throw e
    }
    restoredCount
  }

  private fun sanitizeDocId(raw: String, fallback: String): String {
    val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
    return cleaned.ifBlank { fallback }
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: error
    if (auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path)
    }
  }
}
