package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TimerRepository {
    private final TimerSessionDao timerSessionDao;
    private final FirebaseFirestore firestore;

    public TimerRepository(Context context) {
        this.timerSessionDao = AppDatabase.getInstance(context).timerSessionDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<TimerSession>> getAllTimerSessionsForUser(String userId) {
        return timerSessionDao.getAllTimerSessionsForUser(userId);
    }

    public LiveData<List<TimerSession>> getCompletedTimerSessionsForUser(String userId) {
        return timerSessionDao.getCompletedTimerSessionsForUser(userId);
    }

    public LiveData<List<TimerSession>> getCompletedSessionsBySubject(String userId, String subject) {
        return timerSessionDao.getCompletedSessionsBySubject(userId, subject);
    }

    public LiveData<TimerSession> getOngoingSessionForUser(String userId) {
        return timerSessionDao.getOngoingSessionForUser(userId);
    }

    public LiveData<TimerSession> getTimerSessionById(String sessionId) {
        return timerSessionDao.getTimerSessionById(sessionId);
    }

    public LiveData<Integer> getTotalStudyMinutesForUser(String userId) {
        return timerSessionDao.getTotalStudyMinutesForUser(userId);
    }

    public LiveData<Integer> getCompletedSessionCountForUser(String userId) {
        return timerSessionDao.getCompletedSessionCountForUser(userId);
    }

    public void createTimerSession(TimerSession session, String userId) {
        session.setUserId(userId);
        AppExecutors.diskIO().execute(() -> timerSessionDao.insertTimerSession(session));
        firestore.collection("timerSessions").document(session.getSessionId())
                .set(session).addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateTimerSession(TimerSession session) {
        AppExecutors.diskIO().execute(() -> timerSessionDao.updateTimerSession(session));
        firestore.collection("timerSessions").document(session.getSessionId())
                .set(session).addOnFailureListener(Throwable::printStackTrace);
    }

    public void deleteTimerSession(String sessionId) {
        AppExecutors.diskIO().execute(() -> timerSessionDao.deleteTimerSessionById(sessionId));
        firestore.collection("timerSessions").document(sessionId).delete();
    }

    public void syncTimerSessionsFromFirestore(String userId) {
        firestore.collection("timerSessions").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snap -> {
                    List<TimerSession> sessions = snap.toObjects(TimerSession.class);
                    AppExecutors.diskIO().execute(() -> timerSessionDao.insertAllTimerSessions(sessions));
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<List<TimerSession>> getSessionsInRange(String userId, long startDate) {
        return timerSessionDao.getSessionsInRange(userId, startDate);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(timerSessionDao::clearAllTimerSessions);
    }
}
