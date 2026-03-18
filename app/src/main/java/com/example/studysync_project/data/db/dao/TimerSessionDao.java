package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.TimerSession;

import java.util.List;

/**
 * Data Access Object for TimerSession entity
 */
@Dao
public interface TimerSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTimerSession(TimerSession session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllTimerSessions(List<TimerSession> sessions);

    @Update
    void updateTimerSession(TimerSession session);

    @Delete
    void deleteTimerSession(TimerSession session);

    @Query("DELETE FROM timer_sessions WHERE sessionId = :sessionId")
    void deleteTimerSessionById(String sessionId);

    @Query("SELECT * FROM timer_sessions WHERE sessionId = :sessionId")
    LiveData<TimerSession> getTimerSessionById(String sessionId);

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId ORDER BY startTime DESC")
    LiveData<List<TimerSession>> getAllTimerSessionsForUser(String userId);

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND isCompleted = 1 ORDER BY startTime DESC")
    LiveData<List<TimerSession>> getCompletedTimerSessionsForUser(String userId);

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND subject = :subject AND isCompleted = 1 ORDER BY startTime DESC")
    LiveData<List<TimerSession>> getCompletedSessionsBySubject(String userId, String subject);

    @Query("SELECT SUM(actualDurationMinutes) FROM timer_sessions WHERE userId = :userId AND isCompleted = 1")
    LiveData<Integer> getTotalStudyMinutesForUser(String userId);

    @Query("SELECT COUNT(*) FROM timer_sessions WHERE userId = :userId AND isCompleted = 1")
    LiveData<Integer> getCompletedSessionCountForUser(String userId);

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND isCompleted = 0")
    LiveData<TimerSession> getOngoingSessionForUser(String userId);

    @Query("SELECT * FROM timer_sessions WHERE userId = :userId AND startTime >= :startDate ORDER BY startTime DESC")
    LiveData<List<TimerSession>> getSessionsInRange(String userId, long startDate);

    @Query("DELETE FROM timer_sessions WHERE userId = :userId")
    void deleteAllSessionsForUser(String userId);

    @Query("DELETE FROM timer_sessions")
    void clearAllTimerSessions();
}
