package com.example.studysync_project.ui.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.utils.FocusTimerSessionStore;
import com.example.studysync_project.utils.IdUtil;

public class TimerService extends Service {

    public static final String ACTION_TICK = "com.example.studysync_project.TIMER_TICK";
    public static final String ACTION_FINISH = "com.example.studysync_project.TIMER_FINISH";
    public static final String EXTRA_MILLIS_LEFT = "millis_left";
    public static final String EXTRA_PROGRESS = "progress";

    public static final String END_REASON_COMPLETED = "COMPLETED";
    public static final String END_REASON_ABORTED = "ABORTED";
    public static final String END_REASON_SKIPPED = "SKIPPED";
    public static final String END_REASON_INTERRUPTED = "INTERRUPTED";

    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private static final String CHANNEL_ID = "timer_channel";
    private static final int NOTIF_ID = 1001;

    private final IBinder binder = new LocalBinder();
    private CountDownTimer countDownTimer;
    private TimerRepository timerRepository;
    private long totalMillis;
    private long millisLeft;
    private boolean running = false;

    private boolean trackingStudySession;
    private String trackedUserId = "";
    private String trackedSubject = "";
    private String trackedModuleId = "";
    private String trackedModuleTitle = "";
    private int trackedPlannedMinutes;
    private long trackedSessionStartAt;
    private long trackedActiveSegmentStartAt;
    private long trackedElapsedMillis;
    private String trackedSessionId = "";
    private int trackedLastHeartbeatMinute;

    public static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        timerRepository = new TimerRepository(getApplicationContext());
        createNotificationChannel();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification("Timer running", "00:00"));
        return START_NOT_STICKY;
    }

    public void startTimer(long durationMillis) {
        startTimer(durationMillis, null, null, null, null, false);
    }

    public void startTimer(long durationMillis, String userId, String subject, boolean shouldTrackStudySession) {
        startTimer(durationMillis, userId, subject, null, null, shouldTrackStudySession);
    }

    public void startTimer(
            long durationMillis,
            String userId,
            String subject,
            String moduleId,
            String moduleTitle,
            boolean shouldTrackStudySession
    ) {
        totalMillis = durationMillis;
        millisLeft = durationMillis;
        running = true;

        String safeSubject = subject != null ? subject.trim() : "";
        String safeModuleId = moduleId != null ? moduleId.trim() : "";
        String safeModuleTitle = moduleTitle != null ? moduleTitle.trim() : "";

        if (shouldTrackStudySession) {
            beginTrackedSession(userId, safeSubject, safeModuleId, safeModuleTitle, durationMillis);
        } else {
            clearTrackedSession();
        }

        FocusTimerSessionStore.setActive(
                this,
                true,
                true,
                durationMillis,
                durationMillis,
                safeModuleId,
                safeModuleTitle,
                safeSubject
        );

        startCountdown(durationMillis);
    }

    private void startCountdown(long durationMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Push an immediate UI tick so users see movement as soon as they press Start.
        broadcastTick(millisLeft);

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long left) {
                millisLeft = left;
                String timeStr = formatTime(left);
                updateNotification(timeStr);
                maybePersistHeartbeat();
                FocusTimerSessionStore.updateTick(TimerService.this, left, totalMillis);
                broadcastTick(left);
            }

            @Override
            public void onFinish() {
                running = false;
                millisLeft = 0;
                captureTrackedSegment();
                persistTrackedSession(END_REASON_COMPLETED);
                clearTrackedSession();
                updateNotification("Done!");
                FocusTimerSessionStore.clear(TimerService.this);
                Intent finishIntent = new Intent(ACTION_FINISH).setPackage(getPackageName());
                sendBroadcast(finishIntent);
            }
        }.start();
    }

    public void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        captureTrackedSegment();
        FocusTimerSessionStore.updateTick(this, millisLeft, totalMillis);
        FocusTimerSessionStore.setRunning(this, false);
        maybePersistHeartbeat();
        running = false;
    }

    public void resumeTimer() {
        if (!running && millisLeft > 0) {
            if (trackingStudySession) {
                trackedActiveSegmentStartAt = System.currentTimeMillis();
            }
            running = true;
            FocusTimerSessionStore.setRunning(this, true);
            FocusTimerSessionStore.updateTick(this, millisLeft, totalMillis);
            startCountdown(millisLeft);
        }
    }

    public void resetTimer(long durationMillis) {
        resetTimer(durationMillis, END_REASON_ABORTED);
    }

    public void resetTimer(long durationMillis, String endReason) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (trackingStudySession) {
            captureTrackedSegment();
            persistTrackedSession(endReason);
        }

        running = false;
        totalMillis = durationMillis;
        millisLeft = durationMillis;
        FocusTimerSessionStore.clear(this);
        clearTrackedSession();
    }

    public boolean isRunning() {
        return running;
    }

    public long getMillisLeft() {
        return millisLeft;
    }

    public long getTotalMillis() {
        return totalMillis;
    }

    private void broadcastTick(long left) {
        Intent tick = new Intent(ACTION_TICK).setPackage(getPackageName());
        tick.putExtra(EXTRA_MILLIS_LEFT, left);
        int progress = totalMillis > 0 ? (int) ((left * 100) / totalMillis) : 0;
        tick.putExtra(EXTRA_PROGRESS, progress);
        sendBroadcast(tick);
    }

    private void beginTrackedSession(
            String userId,
            String subject,
            String moduleId,
            String moduleTitle,
            long durationMillis
    ) {
        if (userId == null || userId.trim().isEmpty()) {
            clearTrackedSession();
            return;
        }

        long now = System.currentTimeMillis();
        trackingStudySession = true;
        trackedUserId = userId.trim();
        trackedSubject = subject != null ? subject.trim() : "";
        trackedModuleId = moduleId != null ? moduleId.trim() : "";
        trackedModuleTitle = moduleTitle != null ? moduleTitle.trim() : "";
        trackedPlannedMinutes = Math.max(1, (int) (durationMillis / 60000L));
        trackedSessionStartAt = now;
        trackedActiveSegmentStartAt = now;
        trackedElapsedMillis = 0L;
        trackedSessionId = IdUtil.generateId("session");
        trackedLastHeartbeatMinute = 0;
    }

    private void captureTrackedSegment() {
        if (!trackingStudySession || trackedActiveSegmentStartAt <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now > trackedActiveSegmentStartAt) {
            trackedElapsedMillis += (now - trackedActiveSegmentStartAt);
        }
        trackedActiveSegmentStartAt = 0L;
    }

    private void maybePersistHeartbeat() {
        if (!trackingStudySession || trackedUserId.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int elapsedMinutes = (int) (computeElapsedMillis(now) / 60000L);
        if (elapsedMinutes <= trackedLastHeartbeatMinute) {
            return;
        }

        TimerSession heartbeat = buildTrackedSessionSnapshot(
                elapsedMinutes,
                false,
                STATUS_IN_PROGRESS,
                now
        );
        timerRepository.createTimerSession(heartbeat, trackedUserId);
        trackedLastHeartbeatMinute = elapsedMinutes;
    }

    private void persistTrackedSession(String endReason) {
        if (!trackingStudySession || trackedUserId.isEmpty()) {
            return;
        }

        String normalizedReason = normalizeEndReason(endReason);
        int actualMinutes;
        long now = System.currentTimeMillis();

        if (END_REASON_COMPLETED.equals(normalizedReason)) {
            // Completed focus session counts as the configured focus block.
            actualMinutes = Math.max(1, trackedPlannedMinutes);
        } else {
            // Partial sessions only count once at least one full minute has elapsed.
            actualMinutes = (int) (trackedElapsedMillis / 60000L);
            if (actualMinutes < 1) {
                return;
            }
        }

        TimerSession session = buildTrackedSessionSnapshot(actualMinutes, true, normalizedReason, now);
        timerRepository.createTimerSession(session, trackedUserId);
        trackedLastHeartbeatMinute = Math.max(trackedLastHeartbeatMinute, actualMinutes);
    }

    private long computeElapsedMillis(long now) {
        long elapsed = trackedElapsedMillis;
        if (running && trackedActiveSegmentStartAt > 0L && now > trackedActiveSegmentStartAt) {
            elapsed += (now - trackedActiveSegmentStartAt);
        }
        return Math.max(0L, elapsed);
    }

    private TimerSession buildTrackedSessionSnapshot(int actualMinutes, boolean completed, String status, long endTime) {
        TimerSession session = new TimerSession(trackedUserId, trackedPlannedMinutes, trackedSubject, "");
        session.setSessionId(trackedSessionId);
        session.setStartTime(trackedSessionStartAt);
        session.setCreatedAt(trackedSessionStartAt);
        session.setEndTime(endTime);
        session.setActualDurationMinutes(Math.max(0, actualMinutes));
        session.setNotes(buildSessionNotes(status));
        session.setCompleted(completed);
        return session;
    }

    private String buildSessionNotes(String status) {
        StringBuilder notes = new StringBuilder();
        notes.append("status=").append(status != null ? status : "");
        if (trackedModuleId != null && !trackedModuleId.trim().isEmpty()) {
            notes.append(";moduleId=").append(trackedModuleId.trim());
        }
        if (trackedModuleTitle != null && !trackedModuleTitle.trim().isEmpty()) {
            notes.append(";moduleTitle=").append(trackedModuleTitle.trim());
        }
        return notes.toString();
    }

    private static String normalizeEndReason(String endReason) {
        if (END_REASON_COMPLETED.equalsIgnoreCase(endReason)) {
            return END_REASON_COMPLETED;
        }
        if (END_REASON_ABORTED.equalsIgnoreCase(endReason)) {
            return END_REASON_ABORTED;
        }
        if (END_REASON_SKIPPED.equalsIgnoreCase(endReason)) {
            return END_REASON_SKIPPED;
        }
        if (END_REASON_INTERRUPTED.equalsIgnoreCase(endReason)) {
            return END_REASON_INTERRUPTED;
        }
        return END_REASON_ABORTED;
    }

    private void clearTrackedSession() {
        trackingStudySession = false;
        trackedUserId = "";
        trackedSubject = "";
        trackedModuleId = "";
        trackedModuleTitle = "";
        trackedPlannedMinutes = 0;
        trackedSessionStartAt = 0L;
        trackedActiveSegmentStartAt = 0L;
        trackedElapsedMillis = 0L;
        trackedSessionId = "";
        trackedLastHeartbeatMinute = 0;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Focus Timer", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Shows timer progress");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    private Notification buildNotification(String title, String text) {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String timeText) {
        Notification n = buildNotification("StudySync — Focus Timer", timeText);
        getSystemService(NotificationManager.class).notify(NOTIF_ID, n);
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (trackingStudySession) {
            captureTrackedSegment();
            persistTrackedSession(END_REASON_INTERRUPTED);
        }

        FocusTimerSessionStore.clear(this);
        clearTrackedSession();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }
}
