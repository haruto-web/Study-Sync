package com.example.studysync_project.data.progression;

import android.content.Context;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.db.dao.UserProfileDao;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProgressionRepository {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final int LOOKBACK_DAYS = 120;
    private static final int SUBJECT_ATTEMPT_LIMIT = 24;

    private final UserProfileDao userProfileDao;
    private final QuizAttemptDao quizAttemptDao;
    private final TimerSessionDao timerSessionDao;
    private final TaskDao taskDao;
    private final QuizDao quizDao;
    private final FirebaseFirestore firestore;

    public ProgressionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        this.userProfileDao = db.userProfileDao();
        this.quizAttemptDao = db.quizAttemptDao();
        this.timerSessionDao = db.timerSessionDao();
        this.taskDao = db.taskDao();
        this.quizDao = db.quizDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void recomputeProgressionAsync(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        AppExecutors.diskIO().execute(() -> recomputeAndPersist(userId));
    }

    public void recomputeAndPersistForSync(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        recomputeAndPersist(userId);
    }

    private void recomputeAndPersist(String userId) {
        UserProfile profile = userProfileDao.getUserProfileSync(userId);
        if (profile == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long start7Days = now - 7L * DAY_MS;
        long start14Days = now - 14L * DAY_MS;

        Double averageScore7DaysObj = quizAttemptDao.getAverageScoreBetweenSync(userId, start7Days, now + 1L);
        Double averageScorePrev7DaysObj = quizAttemptDao.getAverageScoreBetweenSync(userId, start14Days, start7Days);
        int attempts7Days = quizAttemptDao.getAttemptCountBetweenSync(userId, start7Days, now + 1L);

        double averageScore7Days = averageScore7DaysObj != null ? averageScore7DaysObj : 0.0;
        double averageScorePrev7Days = averageScorePrev7DaysObj != null ? averageScorePrev7DaysObj : averageScore7Days;

        double quizQuality = ProgressionScoring.clamp(averageScore7Days, 0.0, 100.0);
        double quizTrendScore = ProgressionScoring.clamp(50.0 + ((averageScore7Days - averageScorePrev7Days) * 2.5), 0.0, 100.0);
        double quizComponent = attempts7Days > 0
                ? ((quizQuality * 0.70) + (quizTrendScore * 0.30))
                : 0.0;

        Integer studyMinutes7DaysObj = timerSessionDao.getCompletedMinutesBetweenSync(userId, start7Days, now + 1L);
        int studyMinutes7Days = studyMinutes7DaysObj != null ? studyMinutes7DaysObj : 0;
        int weeklyTargetMinutes = profile.getWeeklyStudyTargetMinutes() > 0
                ? profile.getWeeklyStudyTargetMinutes() : 180;
        double studyVolumeComponent = ProgressionScoring.clamp((studyMinutes7Days * 100.0) / weeklyTargetMinutes, 0.0, 100.0);

        long activitySince = now - (LOOKBACK_DAYS * DAY_MS);
        Set<LocalDate> activeDays = collectActiveDays(userId, activitySince);
        StreakData streakData = computeStreak(activeDays);
        int activeDaysInLast7 = countActiveDaysSince(activeDays, start7Days);

        double streakScore = ProgressionScoring.clamp((streakData.currentStreakDays / 14.0) * 100.0, 0.0, 100.0);
        double consistencyDaysScore = ProgressionScoring.clamp((activeDaysInLast7 / 7.0) * 100.0, 0.0, 100.0);
        double consistencyComponent = (streakScore * 0.70) + (consistencyDaysScore * 0.30);

        int totalTasks = taskDao.getTotalTaskCountForUserSync(userId);
        int completedTasks = taskDao.getCompletedTaskCountForUserSync(userId);
        double taskCompletionComponent = totalTasks > 0
            ? ProgressionScoring.clamp((completedTasks * 100.0) / totalTasks, 0.0, 100.0)
                : 0.0;

        long latestActivityAt = maxTimestamp(
                quizAttemptDao.getLatestAttemptTimestampSync(userId),
                timerSessionDao.getLatestCompletedSessionTimestampSync(userId),
                taskDao.getLatestCompletedTaskTimestampSync(userId)
        );
        double recencyMomentumComponent = ProgressionScoring.computeMomentumScore(latestActivityAt, now);

        double progressionIndex = ProgressionScoring.round1(
                (quizComponent * 0.40)
                        + (consistencyComponent * 0.25)
                        + (studyVolumeComponent * 0.15)
                        + (taskCompletionComponent * 0.10)
                        + (recencyMomentumComponent * 0.10)
        );

        double progressionDelta = ProgressionScoring.round1(progressionIndex - profile.getProgressionIndex());
        String progressionState = ProgressionScoring.deriveProgressionState(
                progressionDelta,
                attempts7Days,
                studyMinutes7Days,
                latestActivityAt,
                now
        );

        SubjectInsights subjectInsights = computeSubjectInsights(userId);
        BadgeUpdate badgeUpdate = unlockBadges(
                profile.getUnlockedBadgesCsv(),
                streakData.currentStreakDays,
                attempts7Days,
                averageScore7Days,
                progressionIndex
        );

        profile.setProgressionIndex(progressionIndex);
        profile.setProgressionDelta(progressionDelta);
        profile.setProgressionState(progressionState);
        profile.setCurrentStreakDays(streakData.currentStreakDays);
        profile.setLongestStreakDays(Math.max(profile.getLongestStreakDays(), streakData.longestStreakDays));
        profile.setStudyMinutesLast7Days(studyMinutes7Days);
        profile.setAverageQuizScoreLast7Days(ProgressionScoring.round1(averageScore7Days));
        profile.setStrongestSubject(subjectInsights.strongestSubject);
        profile.setFocusSubject(subjectInsights.focusSubject);
        profile.setUnlockedBadgesCsv(badgeUpdate.badgesCsv);
        if (badgeUpdate.lastUnlockedBadge != null) {
            profile.setLastUnlockedBadge(badgeUpdate.lastUnlockedBadge);
            profile.setLastBadgeUnlockedAt(now);
        }
        profile.setLastProgressComputedAt(now);
        profile.setUpdatedAt(now);

        userProfileDao.insertUserProfile(profile);
        firestore.collection("users")
                .document(userId)
                .set(profile)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private Set<LocalDate> collectActiveDays(String userId, long since) {
        Set<LocalDate> activeDays = new HashSet<>();
        ZoneId zone = ZoneId.systemDefault();

        addTimestampsAsDays(activeDays, quizAttemptDao.getAttemptTimestampsSinceSync(userId, since), zone);
        addTimestampsAsDays(activeDays, timerSessionDao.getCompletedSessionStartTimesSinceSync(userId, since), zone);
        addTimestampsAsDays(activeDays, taskDao.getCompletedTaskTimestampsSinceSync(userId, since), zone);

        return activeDays;
    }

    private void addTimestampsAsDays(Set<LocalDate> days, List<Long> timestamps, ZoneId zone) {
        if (timestamps == null || timestamps.isEmpty()) return;
        for (Long timestamp : timestamps) {
            if (timestamp == null || timestamp <= 0L) continue;
            LocalDate date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate();
            days.add(date);
        }
    }

    private StreakData computeStreak(Set<LocalDate> activeDays) {
        if (activeDays == null || activeDays.isEmpty()) {
            return new StreakData(0, 0);
        }

        List<Long> epochDays = new ArrayList<>();
        for (LocalDate date : activeDays) {
            epochDays.add(date.toEpochDay());
        }
        Collections.sort(epochDays);

        Set<Long> epochDaySet = new HashSet<>(epochDays);
        long today = LocalDate.now(ZoneId.systemDefault()).toEpochDay();
        long anchor = today;

        if (!epochDaySet.contains(anchor) && epochDaySet.contains(today - 1L)) {
            anchor = today - 1L;
        }

        int currentStreak = 0;
        long cursor = anchor;
        while (epochDaySet.contains(cursor)) {
            currentStreak++;
            cursor--;
        }

        int longestStreak = 1;
        int running = 1;
        for (int i = 1; i < epochDays.size(); i++) {
            if (epochDays.get(i) == epochDays.get(i - 1) + 1L) {
                running++;
            } else {
                running = 1;
            }
            if (running > longestStreak) {
                longestStreak = running;
            }
        }

        return new StreakData(currentStreak, longestStreak);
    }

    private int countActiveDaysSince(Set<LocalDate> activeDays, long since) {
        if (activeDays == null || activeDays.isEmpty()) return 0;
        LocalDate sinceDate = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate();

        int count = 0;
        for (LocalDate day : activeDays) {
            if (!day.isBefore(sinceDate)) {
                count++;
            }
        }
        return count;
    }

    private SubjectInsights computeSubjectInsights(String userId) {
        List<QuizAttempt> attempts = quizAttemptDao.getRecentAttemptsSync(userId, SUBJECT_ATTEMPT_LIMIT);
        if (attempts == null || attempts.isEmpty()) {
            return new SubjectInsights("", "");
        }

        Map<String, SubjectAccumulator> buckets = new HashMap<>();
        Map<String, String> quizSubjectCache = new HashMap<>();

        for (QuizAttempt attempt : attempts) {
            String quizId = attempt.getQuizId();
            String subject = quizSubjectCache.get(quizId);
            if (subject == null) {
                subject = quizDao.getQuizSubjectByIdSync(quizId);
                if (subject == null || subject.trim().isEmpty()) {
                    subject = quizId != null ? quizId : "General";
                }
                quizSubjectCache.put(quizId, subject);
            }

            SubjectAccumulator accumulator = buckets.get(subject);
            if (accumulator == null) {
                accumulator = new SubjectAccumulator();
                buckets.put(subject, accumulator);
            }
            accumulator.totalScore += attempt.getScorePercentage();
            accumulator.count += 1;
        }

        if (buckets.isEmpty()) {
            return new SubjectInsights("", "");
        }

        String strongest = "";
        String weakest = "";
        double strongestScore = -1.0;
        double weakestScore = 101.0;

        for (Map.Entry<String, SubjectAccumulator> entry : buckets.entrySet()) {
            SubjectAccumulator accumulator = entry.getValue();
            if (accumulator.count <= 0) continue;

            double avg = accumulator.totalScore / accumulator.count;
            if (avg > strongestScore) {
                strongestScore = avg;
                strongest = entry.getKey();
            }
            if (avg < weakestScore) {
                weakestScore = avg;
                weakest = entry.getKey();
            }
        }

        return new SubjectInsights(strongest, weakest);
    }

    private BadgeUpdate unlockBadges(
            String currentBadgesCsv,
            int currentStreak,
            int attempts7Days,
            double averageScore7Days,
            double progressionIndex
    ) {
        LinkedHashSet<String> badges = parseBadges(currentBadgesCsv);
        String lastUnlocked = null;

        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, currentStreak >= 3, "STREAK_3");
        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, currentStreak >= 7, "STREAK_7");
        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, currentStreak >= 14, "STREAK_14");
        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, attempts7Days >= 3 && averageScore7Days >= 80.0, "QUIZ_ACE_80");
        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, progressionIndex >= 70.0, "MOMENTUM_70");
        lastUnlocked = addBadgeIfEligible(badges, lastUnlocked, progressionIndex >= 85.0, "MOMENTUM_85");

        String csv = String.join(",", badges);
        return new BadgeUpdate(csv, lastUnlocked);
    }

    private LinkedHashSet<String> parseBadges(String csv) {
        LinkedHashSet<String> badges = new LinkedHashSet<>();
        if (csv == null || csv.trim().isEmpty()) return badges;

        String[] parts = csv.split(",");
        for (String part : parts) {
            String badge = part != null ? part.trim() : "";
            if (!badge.isEmpty()) {
                badges.add(badge);
            }
        }
        return badges;
    }

    private String addBadgeIfEligible(LinkedHashSet<String> badges, String lastUnlocked, boolean eligible, String badge) {
        if (!eligible) return lastUnlocked;
        if (badges.add(badge)) {
            return badge;
        }
        return lastUnlocked;
    }

    private long maxTimestamp(Long a, Long b, Long c) {
        long max = 0L;
        if (a != null && a > max) max = a;
        if (b != null && b > max) max = b;
        if (c != null && c > max) max = c;
        return max;
    }

    public static String formatStateLabel(String state) {
        return ProgressionScoring.formatStateLabel(state);
    }

    private static class StreakData {
        final int currentStreakDays;
        final int longestStreakDays;

        StreakData(int currentStreakDays, int longestStreakDays) {
            this.currentStreakDays = currentStreakDays;
            this.longestStreakDays = longestStreakDays;
        }
    }

    private static class SubjectAccumulator {
        double totalScore = 0.0;
        int count = 0;
    }

    private static class SubjectInsights {
        final String strongestSubject;
        final String focusSubject;

        SubjectInsights(String strongestSubject, String focusSubject) {
            this.strongestSubject = strongestSubject != null ? strongestSubject : "";
            this.focusSubject = focusSubject != null ? focusSubject : "";
        }
    }

    private static class BadgeUpdate {
        final String badgesCsv;
        final String lastUnlockedBadge;

        BadgeUpdate(String badgesCsv, String lastUnlockedBadge) {
            this.badgesCsv = badgesCsv != null ? badgesCsv : "";
            this.lastUnlockedBadge = lastUnlockedBadge;
        }
    }
}
