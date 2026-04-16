package com.example.studysync_project.data.progression;

import android.content.Context;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.StudyModuleDao;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.StudyModule;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Maintains module and quiz progression states from actual learner attempts.
 */
public class ContentProgressionManager {

    private static final double MASTERY_THRESHOLD_SCORE = 80.0;
    private static final int MASTERY_MIN_ATTEMPTS = 2;
    private static final double SCORE_EPSILON = 0.01;

    private final StudyModuleDao studyModuleDao;
    private final QuizDao quizDao;
    private final QuizAttemptDao quizAttemptDao;
    private final FirebaseFirestore firestore;

    public ContentProgressionManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        this.studyModuleDao = db.studyModuleDao();
        this.quizDao = db.quizDao();
        this.quizAttemptDao = db.quizAttemptDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public void onQuizAttemptSaved(String userId, String quizId) {
        if (isBlank(userId) || isBlank(quizId)) {
            return;
        }

        Quiz quiz = quizDao.getQuizByIdSync(quizId);
        if (quiz == null || quiz.isArchived()) {
            return;
        }

        List<QuizAttempt> recentAttempts = quizAttemptDao.getRecentAttemptsForQuizSync(
                userId,
                quizId,
                MASTERY_MIN_ATTEMPTS
        );
        if (recentAttempts == null || recentAttempts.isEmpty()) {
            return;
        }

        int attemptCount = quizAttemptDao.getAttemptCountForQuizSync(userId, quizId);
        double lastScore = recentAttempts.get(0).getScorePercentage();
        Double bestScoreObj = quizAttemptDao.getHighestScoreForQuizSync(userId, quizId);
        double bestScore = bestScoreObj != null ? bestScoreObj : lastScore;

        double masteryWindowAverage = averageScore(recentAttempts);
        boolean meetsMastery = recentAttempts.size() >= MASTERY_MIN_ATTEMPTS
                && masteryWindowAverage >= MASTERY_THRESHOLD_SCORE;
        boolean wasMastered = quiz.getMasteredAt() > 0L;
        long now = System.currentTimeMillis();

        boolean quizChanged = false;
        if (!quiz.isUnlocked()) {
            quiz.setUnlocked(true);
            quizChanged = true;
        }
        if (quiz.getAttemptCount() != attemptCount) {
            quiz.setAttemptCount(attemptCount);
            quizChanged = true;
        }
        if (Math.abs(quiz.getLastScore() - lastScore) > SCORE_EPSILON) {
            quiz.setLastScore(lastScore);
            quizChanged = true;
        }
        if (Math.abs(quiz.getBestScore() - bestScore) > SCORE_EPSILON) {
            quiz.setBestScore(bestScore);
            quizChanged = true;
        }
        if (!wasMastered && meetsMastery) {
            quiz.setMasteredAt(now);
            quizChanged = true;
        }

        if (quizChanged) {
            quiz.setUpdatedAt(now);
            quizDao.updateQuiz(quiz);
            firestore.collection("quizzes")
                    .document(quiz.getQuizId())
                    .set(quiz)
                    .addOnFailureListener(Throwable::printStackTrace);
        }

        String moduleId = safeText(quiz.getModuleId());
        if (moduleId.isEmpty()) {
            return;
        }

        updateModuleProgressFromQuiz(moduleId, attemptCount, bestScore, masteryWindowAverage, meetsMastery, now);
    }

    private void updateModuleProgressFromQuiz(
            String moduleId,
            int attemptCount,
            double bestScore,
            double masteryWindowAverage,
            boolean meetsMastery,
            long now
    ) {
        StudyModule module = studyModuleDao.getStudyModuleByIdSync(moduleId);
        if (module == null || module.isArchived()) {
            return;
        }

        String currentState = safeText(module.getProgressionState());
        boolean wasMastered = StudyModule.PROGRESSION_MASTERED.equalsIgnoreCase(currentState)
                || module.getCompletedAt() > 0L;

        boolean changed = false;
        if (!module.isUnlocked()) {
            module.setUnlocked(true);
            changed = true;
        }
        if (module.getStartedAt() <= 0L) {
            module.setStartedAt(now);
            changed = true;
        }
        if (!wasMastered
                && !StudyModule.PROGRESSION_IN_PROGRESS.equalsIgnoreCase(currentState)) {
            module.setProgressionState(StudyModule.PROGRESSION_IN_PROGRESS);
            changed = true;
        }
        if (module.getMasteryAttempts() < attemptCount) {
            module.setMasteryAttempts(attemptCount);
            changed = true;
        }

        double candidateMasteryScore = Math.max(bestScore, masteryWindowAverage);
        if (candidateMasteryScore > module.getMasteryScore() + SCORE_EPSILON) {
            module.setMasteryScore(candidateMasteryScore);
            changed = true;
        }

        if (!wasMastered && meetsMastery) {
            module.setProgressionState(StudyModule.PROGRESSION_MASTERED);
            module.setCompletedAt(now);
            if (module.getMasteryScore() < masteryWindowAverage) {
                module.setMasteryScore(masteryWindowAverage);
            }
            changed = true;
        }

        if (changed) {
            module.setUpdatedAt(now);
            studyModuleDao.updateStudyModule(module);
            firestore.collection("modules")
                    .document(module.getModuleId())
                    .set(module)
                    .addOnFailureListener(Throwable::printStackTrace);
        }

        if (!wasMastered && meetsMastery) {
            unlockNextModuleInPath(module, now);
        }
    }

    private void unlockNextModuleInPath(StudyModule masteredModule, long now) {
        String userId = safeText(masteredModule.getUserId());
        if (userId.isEmpty()) {
            return;
        }

        String subject = normalizeSubject(masteredModule.getSubject());
        int currentOrder = masteredModule.getUnlockOrder();

        StudyModule nextModule = studyModuleDao.getNextLockedModuleForSubjectSync(
                userId,
                subject,
                currentOrder
        );
        if (nextModule == null) {
            return;
        }

        nextModule.setUnlocked(true);
        if (isBlank(nextModule.getProgressionState())) {
            nextModule.setProgressionState(StudyModule.PROGRESSION_NEW);
        }
        nextModule.setUpdatedAt(now);

        studyModuleDao.updateStudyModule(nextModule);
        firestore.collection("modules")
                .document(nextModule.getModuleId())
                .set(nextModule)
                .addOnFailureListener(Throwable::printStackTrace);

        List<Quiz> quizzes = quizDao.getQuizzesForModuleSync(nextModule.getModuleId());
        if (quizzes == null || quizzes.isEmpty()) {
            return;
        }

        for (Quiz quiz : quizzes) {
            if (quiz == null || quiz.isArchived() || quiz.isUnlocked()) {
                continue;
            }
            quiz.setUnlocked(true);
            quiz.setUpdatedAt(now);
            quizDao.updateQuiz(quiz);
            firestore.collection("quizzes")
                    .document(quiz.getQuizId())
                    .set(quiz)
                    .addOnFailureListener(Throwable::printStackTrace);
        }
    }

    private double averageScore(List<QuizAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        int count = 0;
        for (QuizAttempt attempt : attempts) {
            if (attempt == null) {
                continue;
            }
            total += attempt.getScorePercentage();
            count++;
        }
        if (count == 0) {
            return 0.0;
        }
        return total / count;
    }

    private String normalizeSubject(String subject) {
        String value = safeText(subject);
        if (value.isEmpty()) {
            return "General";
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
