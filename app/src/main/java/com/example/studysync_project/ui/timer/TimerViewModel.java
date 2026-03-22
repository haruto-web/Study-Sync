package com.example.studysync_project.ui.timer;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.utils.IdUtil;

public class TimerViewModel extends ViewModel {

    public static final int TYPE_FOCUS = 0;
    public static final int TYPE_SHORT_BREAK = 1;
    public static final int TYPE_LONG_BREAK = 2;

    public static final long FOCUS_MILLIS = 25 * 60 * 1000L;
    public static final long SHORT_BREAK_MILLIS = 5 * 60 * 1000L;
    public static final long LONG_BREAK_MILLIS = 15 * 60 * 1000L;

    private final TimerRepository repository;
    private final String userId;

    private final MutableLiveData<String> timerText = new MutableLiveData<>("25:00");
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(100);
    private final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> sessionType = new MutableLiveData<>(TYPE_FOCUS);
    private final MutableLiveData<Integer> sessionCount = new MutableLiveData<>(1);
    private final TimerSession activeSession = null;
    // Tracks the start time of the current focus session for logging
    private long sessionStartTime = 0;
    private String currentSubject = "";

    public TimerViewModel(Context context, String userId) {
        this.repository = new TimerRepository(context);
        this.userId = userId;
    }

    public LiveData<String> getTimerText() {
        return timerText;
    }

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<Boolean> getIsRunning() {
        return isRunning;
    }

    public LiveData<Integer> getSessionType() {
        return sessionType;
    }

    public LiveData<Integer> getSessionCount() {
        return sessionCount;
    }

    public LiveData<Integer> getTotalMinutesToday() {
        return repository.getTotalStudyMinutesForUser(userId);
    }

    public LiveData<Integer> getSessionsCompletedToday() {
        return repository.getCompletedSessionCountForUser(userId);
    }

    public void onTick(long millisLeft, int prog) {
        timerText.setValue(TimerService.formatTime(millisLeft));
        progress.setValue(prog);
    }

    public void onStartPause(TimerService service) {
        if (service == null) return;
        if (service.isRunning()) {
            service.pauseTimer();
            isRunning.setValue(false);
        } else {
            if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();
            service.resumeTimer();
            isRunning.setValue(true);
        }
    }

    public void onStart(TimerService service) {
        if (service == null) return;
        sessionStartTime = System.currentTimeMillis();
        service.startTimer(getDurationForCurrentType());
        isRunning.setValue(true);
    }

    public void onReset(TimerService service) {
        if (service == null) return;
        service.resetTimer(getDurationForCurrentType());
        isRunning.setValue(false);
        sessionStartTime = 0;
        timerText.setValue(TimerService.formatTime(getDurationForCurrentType()));
        progress.setValue(100);
    }

    public void onSkip(TimerService service) {
        if (service == null) return;
        service.resetTimer(getDurationForCurrentType());
        isRunning.setValue(false);
        sessionStartTime = 0;
        advanceSession();
    }

    public void onSessionFinished() {
        isRunning.setValue(false);
        if (sessionType.getValue() == TYPE_FOCUS) {
            logCompletedSession();
            advanceSession();
        } else {
            // After break, go back to focus
            setSessionType(TYPE_FOCUS, null);
        }
    }

    public void setSessionType(int type, TimerService service) {
        sessionType.setValue(type);
        sessionStartTime = 0;
        isRunning.setValue(false);
        long duration = getDurationForType(type);
        timerText.setValue(TimerService.formatTime(duration));
        progress.setValue(100);
        if (service != null) service.resetTimer(duration);
    }

    public void setSubject(String subject) {
        this.currentSubject = subject;
    }

    private void advanceSession() {
        int count = sessionCount.getValue() != null ? sessionCount.getValue() : 1;
        if (sessionType.getValue() == TYPE_FOCUS) {
            if (count % 4 == 0) {
                setSessionType(TYPE_LONG_BREAK, null);
            } else {
                setSessionType(TYPE_SHORT_BREAK, null);
            }
            sessionCount.setValue(count + 1);
        } else {
            setSessionType(TYPE_FOCUS, null);
        }
    }

    private void logCompletedSession() {
        if (sessionStartTime == 0) return;
        long endTime = System.currentTimeMillis();
        int actualMinutes = (int) ((endTime - sessionStartTime) / 60000);

        TimerSession session = new TimerSession(userId, 25, currentSubject, "");
        session.setSessionId(IdUtil.generateId("session"));
        session.setCompleted(true);
        session.setEndTime(endTime);
        session.setActualDurationMinutes(Math.max(actualMinutes, 1));
        repository.createTimerSession(session, userId);
        sessionStartTime = 0;
    }

    public long getDurationForCurrentType() {
        return getDurationForType(sessionType.getValue() != null ? sessionType.getValue() : TYPE_FOCUS);
    }

    private long getDurationForType(int type) {
        switch (type) {
            case TYPE_SHORT_BREAK:
                return SHORT_BREAK_MILLIS;
            case TYPE_LONG_BREAK:
                return LONG_BREAK_MILLIS;
            default:
                return FOCUS_MILLIS;
        }
    }
}
