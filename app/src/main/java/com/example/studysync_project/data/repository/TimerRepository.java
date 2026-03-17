package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.model.TimerSession;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for TimerSession data
 * Handles communication between Firestore, Room database, and UI
 */
public class TimerRepository {
    private final TimerSessionDao timerSessionDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public TimerRepository(Context context) {
        this.context = context;
        this.timerSessionDao = AppDatabase.getInstance(context).timerSessionDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get all timer sessions for user
     */
    public LiveData<List<TimerSession>> getAllTimerSessionsForUser(String userId) {
        return timerSessionDao.getAllTimerSessionsForUser(userId);
    }

    /**
     * Get completed timer sessions
     */
    public LiveData<List<TimerSession>> getCompletedTimerSessionsForUser(String userId) {
        return timerSessionDao.getCompletedTimerSessionsForUser(userId);
    }

    /**
     * Get sessions by subject
     */
    public LiveData<List<TimerSession>> getCompletedSessionsBySubject(String userId, String subject) {
        return timerSessionDao.getCompletedSessionsBySubject(userId, subject);
    }

    /**
     * Get ongoing session (if any)
     */
    public LiveData<TimerSession> getOngoingSessionForUser(String userId) {
        return timerSessionDao.getOngoingSessionForUser(userId);
    }

    /**
     * Get a single session
     */
    public LiveData<TimerSession> getTimerSessionById(String sessionId) {
        return timerSessionDao.getTimerSessionById(sessionId);
    }

    /**
     * Get total study minutes for user
     */
    public LiveData<Integer> getTotalStudyMinutesForUser(String userId) {
        return timerSessionDao.getTotalStudyMinutesForUser(userId);
    }

    /**
     * Get count of completed sessions
     */
    public LiveData<Integer> getCompletedSessionCountForUser(String userId) {
        return timerSessionDao.getCompletedSessionCountForUser(userId);
    }

    /**
     * Create a new timer session
     */
    public void createTimerSession(TimerSession session, String userId) {
        session.setUserId(userId);
        
        // Save to Firestore
        firestore.collection("timerSessions")
            .document(session.getSessionId())
            .set(session)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                timerSessionDao.insertTimerSession(session);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Update an existing timer session
     */
    public void updateTimerSession(TimerSession session) {
        // Update in Firestore
        firestore.collection("timerSessions")
            .document(session.getSessionId())
            .set(session)
            .addOnSuccessListener(aVoid -> {
                // Update in Room
                timerSessionDao.updateTimerSession(session);
            });
    }

    /**
     * Complete a timer session
     */
    public void completeTimerSession(String sessionId) {
        LiveData<TimerSession> sessionLiveData = timerSessionDao.getTimerSessionById(sessionId);
        TimerSession session = sessionLiveData.getValue();
        
        if (session != null) {
            session.setCompleted(true);
            session.setEndTime(System.currentTimeMillis());
            updateTimerSession(session);
        }
    }

    /**
     * Delete a timer session
     */
    public void deleteTimerSession(String sessionId) {
        firestore.collection("timerSessions")
            .document(sessionId)
            .delete();
        
        timerSessionDao.deleteTimerSessionById(sessionId);
    }

    /**
     * Sync timer sessions from Firestore to Room
     */
    public void syncTimerSessionsFromFirestore(String userId) {
        firestore.collection("timerSessions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<TimerSession> sessions = queryDocumentSnapshots.toObjects(TimerSession.class);
                timerSessionDao.insertAllTimerSessions(sessions);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Get sessions in a specific date range
     */
    public LiveData<List<TimerSession>> getSessionsInRange(String userId, long startDate) {
        return timerSessionDao.getSessionsInRange(userId, startDate);
    }

    /**
     * Clear all local timer session data
     */
    public void clearLocalData() {
        timerSessionDao.clearAllTimerSessions();
    }
}
