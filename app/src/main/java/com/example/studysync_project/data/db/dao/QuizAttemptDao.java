package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.QuizAttempt;

import java.util.List;

/**
 * Data Access Object for QuizAttempt entity
 */
@Dao
public interface QuizAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuizAttempt(QuizAttempt attempt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllQuizAttempts(List<QuizAttempt> attempts);

    @Update
    void updateQuizAttempt(QuizAttempt attempt);

    @Delete
    void deleteQuizAttempt(QuizAttempt attempt);

    @Query("DELETE FROM quiz_attempts WHERE attemptId = :attemptId")
    void deleteQuizAttemptById(String attemptId);

    @Query("SELECT * FROM quiz_attempts WHERE attemptId = :attemptId")
    LiveData<QuizAttempt> getQuizAttemptById(String attemptId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY attemptedAt DESC")
    LiveData<List<QuizAttempt>> getAllQuizAttemptsForUser(String userId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId ORDER BY attemptedAt DESC")
    LiveData<List<QuizAttempt>> getAttemptsForQuiz(String userId, String quizId);

        @Query("SELECT AVG(quiz_latest.quizScore) " +
            "FROM (" +
            "  SELECT qa.quizId AS quizId, AVG(qa.scorePercentage) AS quizScore " +
            "  FROM quiz_attempts qa " +
            "  INNER JOIN (" +
            "    SELECT quizId, MAX(attemptedAt) AS latestAttempt " +
            "    FROM quiz_attempts " +
            "    WHERE userId = :userId " +
            "    GROUP BY quizId" +
            "  ) latest ON latest.quizId = qa.quizId AND latest.latestAttempt = qa.attemptedAt " +
            "  WHERE qa.userId = :userId " +
            "  GROUP BY qa.quizId" +
            ") quiz_latest")
    LiveData<Double> getAverageScoreForUser(String userId);

        @Query("SELECT AVG(quiz_latest.quizScore) " +
            "FROM (" +
            "  SELECT qa.quizId AS quizId, AVG(qa.scorePercentage) AS quizScore " +
            "  FROM quiz_attempts qa " +
            "  INNER JOIN (" +
            "    SELECT quizId, MAX(attemptedAt) AS latestAttempt " +
            "    FROM quiz_attempts " +
            "    WHERE userId = :userId AND attemptedAt >= :startTime AND attemptedAt < :endTime " +
            "    GROUP BY quizId" +
            "  ) latest ON latest.quizId = qa.quizId AND latest.latestAttempt = qa.attemptedAt " +
            "  WHERE qa.userId = :userId AND qa.attemptedAt >= :startTime AND qa.attemptedAt < :endTime " +
            "  GROUP BY qa.quizId" +
            ") quiz_latest")
    Double getAverageScoreBetweenSync(String userId, long startTime, long endTime);

        @Query("SELECT AVG(module_scores.moduleScore) " +
            "FROM (" +
            "  SELECT q.moduleId AS moduleId, AVG(quiz_latest.quizScore) AS moduleScore " +
            "  FROM quizzes q " +
            "  INNER JOIN (" +
            "    SELECT qa.quizId AS quizId, AVG(qa.scorePercentage) AS quizScore " +
            "    FROM quiz_attempts qa " +
            "    INNER JOIN (" +
            "      SELECT quizId, MAX(attemptedAt) AS latestAttempt " +
            "      FROM quiz_attempts " +
            "      WHERE userId = :userId " +
            "      GROUP BY quizId" +
            "    ) latest ON latest.quizId = qa.quizId AND latest.latestAttempt = qa.attemptedAt " +
            "    WHERE qa.userId = :userId " +
            "    GROUP BY qa.quizId" +
            "  ) quiz_latest ON quiz_latest.quizId = q.quizId " +
            "  WHERE q.userId = :userId AND q.moduleId IS NOT NULL AND TRIM(q.moduleId) != '' " +
            "  GROUP BY q.moduleId" +
            ") module_scores")
        LiveData<Double> getAverageScoreForUserByModule(String userId);

        @Query("SELECT AVG(module_scores.moduleScore) " +
            "FROM (" +
            "  SELECT q.moduleId AS moduleId, AVG(quiz_latest.quizScore) AS moduleScore " +
            "  FROM quizzes q " +
            "  INNER JOIN (" +
            "    SELECT qa.quizId AS quizId, AVG(qa.scorePercentage) AS quizScore " +
            "    FROM quiz_attempts qa " +
            "    INNER JOIN (" +
            "      SELECT quizId, MAX(attemptedAt) AS latestAttempt " +
            "      FROM quiz_attempts " +
            "      WHERE userId = :userId AND attemptedAt >= :startTime AND attemptedAt < :endTime " +
            "      GROUP BY quizId" +
            "    ) latest ON latest.quizId = qa.quizId AND latest.latestAttempt = qa.attemptedAt " +
            "    WHERE qa.userId = :userId AND qa.attemptedAt >= :startTime AND qa.attemptedAt < :endTime " +
            "    GROUP BY qa.quizId" +
            "  ) quiz_latest ON quiz_latest.quizId = q.quizId " +
            "  WHERE q.userId = :userId AND q.moduleId IS NOT NULL AND TRIM(q.moduleId) != '' " +
            "  GROUP BY q.moduleId" +
            ") module_scores")
        Double getAverageScoreBetweenByModuleSync(String userId, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND attemptedAt >= :startTime AND attemptedAt < :endTime")
    int getAttemptCountBetweenSync(String userId, long startTime, long endTime);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId")
    LiveData<Integer> getTotalQuizAttemptsForUser(String userId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND passed = 1")
    LiveData<Integer> getPassedQuizzesCountForUser(String userId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId ORDER BY attemptedAt DESC LIMIT 1")
    LiveData<QuizAttempt> getLastAttemptForQuiz(String userId, String quizId);

    @Query("SELECT attemptedAt FROM quiz_attempts WHERE userId = :userId AND attemptedAt >= :since")
    List<Long> getAttemptTimestampsSinceSync(String userId, long since);

    @Query("SELECT MAX(attemptedAt) FROM quiz_attempts WHERE userId = :userId")
    Long getLatestAttemptTimestampSync(String userId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId ORDER BY attemptedAt DESC LIMIT :limit")
    List<QuizAttempt> getRecentAttemptsSync(String userId, int limit);

    @Query("SELECT MAX(scorePercentage) FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId")
    LiveData<Double> getHighestScoreForQuiz(String userId, String quizId);

    @Query("SELECT MAX(scorePercentage) FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId")
    Double getHighestScoreForQuizSync(String userId, String quizId);

    @Query("SELECT AVG(scorePercentage) FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId")
    LiveData<Double> getAverageScoreForQuiz(String userId, String quizId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId")
    LiveData<Integer> getAttemptCountForQuiz(String userId, String quizId);

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId")
    int getAttemptCountForQuizSync(String userId, String quizId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId ORDER BY attemptedAt DESC LIMIT 2")
    LiveData<List<QuizAttempt>> getLastTwoAttemptsForQuiz(String userId, String quizId);

    @Query("SELECT * FROM quiz_attempts WHERE userId = :userId AND quizId = :quizId ORDER BY attemptedAt DESC LIMIT :limit")
    List<QuizAttempt> getRecentAttemptsForQuizSync(String userId, String quizId, int limit);

    @Query("DELETE FROM quiz_attempts WHERE userId = :userId")
    void deleteAllAttemptsForUser(String userId);

    @Query("DELETE FROM quiz_attempts")
    void clearAllQuizAttempts();
}
