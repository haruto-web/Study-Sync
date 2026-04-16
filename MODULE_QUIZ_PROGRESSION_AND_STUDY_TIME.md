# StudySync Module/Quiz Progression and Study Time Notes

Date: 2026-04-13

## 1) Current Module and Quiz Progression System (How It Works)

### 1.1 Data model and persisted state
The progression system is now persisted in Room and Firestore.

Module fields (study_modules):
- progressionState: NEW, IN_PROGRESS, MASTERED
- unlockOrder: sequence number inside a subject path
- isUnlocked: whether learner can open module
- startedAt, completedAt
- masteryScore, masteryAttempts

Quiz fields (quizzes):
- isUnlocked
- lastScore, bestScore
- attemptCount
- masteredAt

Database migration:
- AppDatabase was bumped to version 7 and includes migration 6 -> 7 adding these columns.

### 1.2 Module sequencing and unlock initialization
When a module is created:
- It is assigned an unlockOrder for its subject.
- It is unlocked only if:
  - there is no prior module in that subject, or
  - the latest prior module is MASTERED.
- Otherwise it is created locked.

This behavior is set in StudyModuleRepository during upsert of a newly created module.

### 1.3 Read-first behavior before quiz generation
The flow now enforces read-first:
- A module in NEW state cannot generate quiz yet.
- Opening a module marks it IN_PROGRESS (if it was NEW and unlocked).
- Quiz generation from module is blocked if module is locked or still NEW.

### 1.4 Attempt-driven mastery and unlock progression
Every saved quiz attempt triggers progression updates via ContentProgressionManager.

Current mastery rule:
- Use recent attempt window size: 2 attempts
- Mastery threshold: average >= 80%

When an attempt is saved:
- Quiz metrics are updated:
  - attemptCount, lastScore, bestScore
  - masteredAt set when mastery rule is met
- Linked module metrics are updated:
  - state transitions NEW/IN_PROGRESS/MASTERED
  - masteryAttempts, masteryScore
  - completedAt set on mastery
- If module becomes MASTERED, the next locked module in the same subject path is unlocked.
- Any quizzes linked to that newly unlocked module are also unlocked.

### 1.5 UI lock/status behavior
The list UI now reflects progression:
- Module card chip: Locked, New, In Progress, Mastered
- Quiz card chip: Locked, New, In Progress, Mastered
- Locked modules/quizzes are dimmed and blocked from opening.
- Generate Quiz button on module cards:
  - Locked if module locked
  - Read First if module is NEW
  - Generate Quiz once module is IN_PROGRESS or MASTERED

## 2) Current Gaps and What Can Be Improved

### 2.1 Progression model improvements
- Add prerequisite graph support, not just simple linear subject unlockOrder.
- Add per-topic progression granularity (topic nodes under subject path).
- Add spaced repetition/retention checks (mastered now does not decay yet).
- Add dynamic mastery rule by difficulty and Bloom level (not one static 80% rule).
- Add confidence and mistake-category weighting (conceptual vs careless errors).

### 2.2 Pedagogical improvements
- Require short active recall step before quiz unlock (quick checkpoint questions).
- Add minimum reading engagement signals before quiz starts.
- Add remediation branch: if score drops after mastery, route to review module.
- Add adaptive quiz difficulty progression tied to mastery history.

### 2.3 UX improvements
- Add explicit lock reason text on each locked item.
- Add progress bars on module cards (read progress + quiz mastery).
- Add "Next best action" CTA per card.
- Add timeline/history view of unlocks and mastered milestones.

### 2.4 Reliability and analytics improvements
- Add progression backfill worker for legacy modules/quizzes.
- Add telemetry events for state transitions to debug unexpected locks.
- Add unit tests for multi-quiz-per-module and cross-subject edge cases.

## 3) Current Study Time and Minutes Studied System

### 3.1 How minutes are currently calculated
Minutes studied are based on completed timer sessions only.

Current flow:
1. User runs focus timer.
2. On timer finish event, TimerViewModel logs a completed TimerSession.
3. actualDurationMinutes is computed from sessionStartTime -> finish time.
4. Home and Timer screens read sums from TimerSessionDao:
   - today minutes: sum(actualDurationMinutes) where isCompleted=1 and startTime in today window
   - weekly minutes for progression: sum(actualDurationMinutes) in last 7 days

Important detail:
- Minutes are not continuously incremented while timer is running.
- Minutes increase only when a focus session is logged as completed.

## 4) Why Minutes Might Not Be Moving (Likely Causes)

Most likely causes in current implementation:

1. Session must complete to count
- If user starts then resets/skips/exits before finish, no completed session is logged.
- Therefore minutes stay unchanged.

2. Finish handling is Fragment-driven
- Completion logging is done in TimerViewModel.onSessionFinished, triggered by TimerFragment receiver.
- Receiver is registered in fragment onStart and unregistered onStop.
- If timer finishes while user is not on TimerFragment, finish event may not be handled, and session may not be logged.

3. Today query uses startTime day window
- Today sum uses startTime range.
- A session crossing midnight can be attributed unexpectedly.

4. No in-progress persistence
- No periodic save/heartbeat while running.
- If app/service lifecycle interrupts before finish handling, progress can be lost.

## 5) Study Time Enhancement Plan

### 5.1 High-priority fixes (recommended first)
1. Move completion logging into TimerService
- The service should be source of truth for timer finish and persistence.
- On CountDownTimer.onFinish, service should write completed TimerSession directly.

2. Add partial-session logging
- On pause/reset/skip/stop, save session with status:
  - COMPLETED, ABORTED, INTERRUPTED
- Count partial minutes using a policy (for example, >= 1 minute counts).

3. Add minute heartbeat updates
- While running, persist elapsed minutes periodically (for example every 60 seconds).
- UI can show live "minutes today" instead of waiting for full session completion.

4. Use overlap-based day attribution
- Compute minutes for a day using overlap between [startTime, endTime] and [dayStart, dayEnd].
- Fixes midnight boundary issues.

### 5.2 Medium-term upgrades
- Add session quality metrics (focus score, interruptions count).
- Add weekly trends chart for study minutes and consistency.
- Add goal engine: daily target, weekly target, streak with grace rules.
- Add auto-recovery of running session after process death.

### 5.3 Suggested technical additions
- Add TimerSession.status and elapsedSeconds columns.
- Add DAO queries for:
  - in-progress elapsed today
  - overlap-based minutes by date window
  - completed vs aborted session counts
- Add a tiny domain service for time aggregation so Home/Progress use same logic.

## 6) Quick Validation Checklist for "Minutes Not Moving"

Use this to verify current behavior quickly:
- Start a focus session and let it fully finish while staying on Timer screen.
- Confirm TimerSession row inserted with isCompleted=1 and actualDurationMinutes > 0.
- Confirm Home and Timer cards update from DAO observers.
- Repeat by leaving Timer screen before finish; check if session still gets logged.
- Test a near-midnight session to verify day attribution behavior.

## 7) Recommended Next Implementation Order

1. Service-owned completion persistence (fix missed finish events).
2. Partial session persistence on pause/reset/skip.
3. Live minute heartbeat and UI updates.
4. Overlap-based minute attribution and analytics query updates.
5. Session status dashboards and study-quality insights.
