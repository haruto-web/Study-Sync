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

public class TimerService extends Service {

    public static final String ACTION_TICK = "com.example.studysync_project.TIMER_TICK";
    public static final String ACTION_FINISH = "com.example.studysync_project.TIMER_FINISH";
    public static final String EXTRA_MILLIS_LEFT = "millis_left";
    public static final String EXTRA_PROGRESS = "progress";

    private static final String CHANNEL_ID = "timer_channel";
    private static final int NOTIF_ID = 1001;

    private final IBinder binder = new LocalBinder();
    private CountDownTimer countDownTimer;
    private long totalMillis;
    private long millisLeft;
    private boolean running = false;

    public static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        totalMillis = durationMillis;
        millisLeft = durationMillis;
        running = true;

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long left) {
                millisLeft = left;
                String timeStr = formatTime(left);
                updateNotification(timeStr);

                Intent tick = new Intent(ACTION_TICK);
                tick.putExtra(EXTRA_MILLIS_LEFT, left);
                tick.putExtra(EXTRA_PROGRESS, (int) ((left * 100) / totalMillis));
                sendBroadcast(tick);
            }

            @Override
            public void onFinish() {
                running = false;
                millisLeft = 0;
                updateNotification("Done!");
                sendBroadcast(new Intent(ACTION_FINISH));
            }
        }.start();
    }

    public void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        running = false;
    }

    public void resumeTimer() {
        if (!running && millisLeft > 0) startTimer(millisLeft);
    }

    public void resetTimer(long durationMillis) {
        if (countDownTimer != null) countDownTimer.cancel();
        running = false;
        totalMillis = durationMillis;
        millisLeft = durationMillis;
    }

    public boolean isRunning() {
        return running;
    }

    public long getMillisLeft() {
        return millisLeft;
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
        if (countDownTimer != null) countDownTimer.cancel();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }
}
