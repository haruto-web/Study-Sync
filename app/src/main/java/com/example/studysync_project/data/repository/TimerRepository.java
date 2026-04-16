package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.utils.StudyTimeWindow;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TimerRepository {
    private final TimerSessionDao timerSessionDao;
    private final FirebaseFirestore firestore;
    private final ProgressionRepository progressionRepository;

    public TimerRepository(Context context) {
        this.timerSessionDao = AppDatabase.getInstance(context).timerSessionDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.progressionRepository = new ProgressionRepository(context);
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

    public LiveData<Integer> getCompletedStudyMinutesInRange(String userId, long startTime, long endTime) {
        return timerSessionDao.getCompletedMinutesBetween(userId, startTime, endTime);
    }

    public LiveData<Integer> getCompletedOverlapMinutesInRange(String userId, long startTime, long endTime) {
        MediatorLiveData<Integer> result = new MediatorLiveData<>();
        LiveData<List<TimerSession>> source =
                timerSessionDao.getCompletedSessionsOverlappingRange(userId, startTime, endTime);
        result.addSource(source, sessions -> result.setValue(sumOverlapMinutes(sessions, startTime, endTime)));
        return result;
    }

    public LiveData<Integer> getOngoingOverlapMinutesInRange(String userId, long startTime, long endTime) {
        MediatorLiveData<Integer> result = new MediatorLiveData<>();
        LiveData<List<TimerSession>> source =
                timerSessionDao.getOngoingSessionsOverlappingRange(userId, startTime, endTime);
        result.addSource(source, sessions -> result.setValue(sumOverlapMinutes(sessions, startTime, endTime)));
        return result;
    }

    public LiveData<Integer> getCompletedSessionCountInRange(String userId, long startTime, long endTime) {
        return timerSessionDao.getCompletedSessionCountBetween(userId, startTime, endTime);
    }

    public LiveData<Integer> getTodayStudyMinutesForUser(String userId) {
        long now = System.currentTimeMillis();
        long start = StudyTimeWindow.startOfDayMillis(now);
        long end = StudyTimeWindow.endOfDayMillis(now);

        LiveData<Integer> completed = getCompletedOverlapMinutesInRange(userId, start, end);
        LiveData<Integer> ongoing = getOngoingOverlapMinutesInRange(userId, start, end);

        MediatorLiveData<Integer> total = new MediatorLiveData<>();
        final int[] completedMinutes = new int[]{0};
        final int[] ongoingMinutes = new int[]{0};

        total.addSource(completed, value -> {
            completedMinutes[0] = value != null ? value : 0;
            total.setValue(completedMinutes[0] + ongoingMinutes[0]);
        });
        total.addSource(ongoing, value -> {
            ongoingMinutes[0] = value != null ? value : 0;
            total.setValue(completedMinutes[0] + ongoingMinutes[0]);
        });

        return total;
    }

    public LiveData<Integer> getTodayCompletedSessionCountForUser(String userId) {
        long now = System.currentTimeMillis();
        long start = StudyTimeWindow.startOfDayMillis(now);
        long end = StudyTimeWindow.endOfDayMillis(now);
        return getCompletedSessionCountInRange(userId, start, end);
    }

    public void createTimerSession(TimerSession session, String userId) {
        session.setUserId(userId);
        AppExecutors.diskIO().execute(() -> timerSessionDao.insertTimerSession(session));
        firestore.collection("timerSessions").document(session.getSessionId())
                .set(session).addOnFailureListener(Throwable::printStackTrace);
        if (session.isCompleted()) {
            progressionRepository.recomputeProgressionAsync(userId);
        }
    }

    public void updateTimerSession(TimerSession session) {
        AppExecutors.diskIO().execute(() -> timerSessionDao.updateTimerSession(session));
        firestore.collection("timerSessions").document(session.getSessionId())
                .set(session).addOnFailureListener(Throwable::printStackTrace);
        if (session.isCompleted() && session.getUserId() != null) {
            progressionRepository.recomputeProgressionAsync(session.getUserId());
        }
    }

    public void deleteTimerSession(String sessionId) {
        AppExecutors.diskIO().execute(() -> {
            TimerSession session = timerSessionDao.getTimerSessionByIdSync(sessionId);
            timerSessionDao.deleteTimerSessionById(sessionId);
            if (session != null && session.getUserId() != null) {
                progressionRepository.recomputeAndPersistForSync(session.getUserId());
            }
        });
        firestore.collection("timerSessions").document(sessionId).delete();
    }

    public void syncTimerSessionsFromFirestore(String userId) {
        firestore.collection("timerSessions").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snap -> {
                    List<TimerSession> sessions = snap.toObjects(TimerSession.class);
                    AppExecutors.diskIO().execute(() -> {
                        timerSessionDao.insertAllTimerSessions(sessions);
                        progressionRepository.recomputeAndPersistForSync(userId);
                    });
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<List<TimerSession>> getSessionsInRange(String userId, long startDate) {
        return timerSessionDao.getSessionsInRange(userId, startDate);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(timerSessionDao::clearAllTimerSessions);
    }

    private static int sumOverlapMinutes(List<TimerSession> sessions, long windowStart, long windowEnd) {
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        long overlapMillisTotal = 0L;
        for (TimerSession session : sessions) {
            if (session == null) {
                continue;
            }

            long sessionStart = session.getStartTime();
            long sessionEnd = session.getEndTime();
            if (sessionEnd <= sessionStart) {
                long fallbackEnd = sessionStart + (Math.max(0, session.getActualDurationMinutes()) * 60000L);
                sessionEnd = Math.max(sessionStart, fallbackEnd);
            }

            long overlapStart = Math.max(sessionStart, windowStart);
            long overlapEnd = Math.min(sessionEnd, windowEnd);
            if (overlapEnd > overlapStart) {
                overlapMillisTotal += (overlapEnd - overlapStart);
            }
        }

        return (int) (overlapMillisTotal / 60000L);
    }
}
