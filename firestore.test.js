const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfile(uid) {
  return {
    userId: uid,
    displayName: "Nasrullah",
    themeMode: "system",
    challengeStartDate: "2026-11-17",
    challengeEndDate: "2026-12-16",
    seededDefaults: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validHabit(uid) {
  return {
    userId: uid,
    title: "Fajr Prayer",
    category: "Spiritual",
    targetDetail: "Daily before sunrise",
    sortOrder: 0,
    isActive: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validDailyLog(uid) {
  return {
    userId: uid,
    dayNumber: 1,
    dateIso: "2026-11-17",
    completedHabitIds: ["habit_1"],
    totalHabitsSnapshot: 21,
    notes: "Strong Day 1 start!",
    studyHours: 3.0,
    fiverrHours: 7.5,
    waterGlasses: 10,
    sleepHours: 8.0,
    linkedinConnections: 10,
    instagramDms: 20,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

function validReminder(uid) {
  return {
    userId: uid,
    title: "LinkedIn Video Post",
    targetText: "8 PM Bangladesh Time",
    timeLabel: "08:00 PM",
    hour: 20,
    minute: 0,
    enabled: true,
    createdAt: nowTs(),
    updatedAt: nowTs(),
  };
}

test("Unauthenticated user cannot read or write habits or dailyLogs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("habits").doc("h1").set(validHabit(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("day_01").set(validDailyLog(ALICE_UID))
  );
});

test("Owner can create, read, and update own profile, habit, dailyLog, and reminder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userDoc.set(validProfile(ALICE_UID)));
  await assertSucceeds(userDoc.get());
  await assertSucceeds(
    userDoc.update({
      themeMode: "dark",
      updatedAt: nowTs(),
    })
  );

  const habitDoc = userDoc.collection("habits").doc("habit_1");
  await assertSucceeds(habitDoc.set(validHabit(ALICE_UID)));
  await assertSucceeds(userDoc.collection("habits").get());
  await assertSucceeds(
    habitDoc.update({
      title: "Fajr Prayer + Sunnah",
      updatedAt: nowTs(),
    })
  );

  const logDoc = userDoc.collection("dailyLogs").doc("day_01");
  await assertSucceeds(logDoc.set(validDailyLog(ALICE_UID)));
  await assertSucceeds(userDoc.collection("dailyLogs").get());
  await assertSucceeds(
    logDoc.update({
      waterGlasses: 11,
      fiverrHours: 8.0,
      updatedAt: nowTs(),
    })
  );

  const remDoc = userDoc.collection("reminders").doc("rem_1");
  await assertSucceeds(remDoc.set(validReminder(ALICE_UID)));
  await assertSucceeds(userDoc.collection("reminders").get());
});

test("Cross-user access is denied for profile, habits, dailyLogs, and reminders", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").set(validHabit(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_2").set(validHabit(BOB_UID))
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("day_01").set(validDailyLog(BOB_UID))
  );
});

test("Shadow update with ghost field or invalid values fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const habitDoc = aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1");

  await assertFails(
    habitDoc.set({
      ...validHabit(ALICE_UID),
      isAdmin: true,
    })
  );

  await assertFails(
    habitDoc.set({
      ...validHabit(ALICE_UID),
      category: "InvalidCategory",
    })
  );

  await assertSucceeds(habitDoc.set(validHabit(ALICE_UID)));
  await assertFails(
    habitDoc.update({
      userId: BOB_UID,
      updatedAt: nowTs(),
    })
  );
});
