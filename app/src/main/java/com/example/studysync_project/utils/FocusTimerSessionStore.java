package com.example.studysync_project.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Shared app-wide state for the currently active timer session.
 */
public final class FocusTimerSessionStore {

    private static final String PREFS_NAME = "focus_timer_state";

    private static final String KEY_ACTIVE = "active";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_TOTAL_MILLIS = "total_millis";
    private static final String KEY_MILLIS_LEFT = "millis_left";
    private static final String KEY_UPDATED_AT = "updated_at";
    private static final String KEY_MODULE_ID = "module_id";
    private static final String KEY_MODULE_TITLE = "module_title";
    private static final String KEY_SUBJECT = "subject";

    private FocusTimerSessionStore() {
    }

    public static void setActive(
            Context context,
            boolean active,
            boolean running,
            long totalMillis,
            long millisLeft,
            String moduleId,
            String moduleTitle,
            String subject
    ) {
        SharedPreferences prefs = prefs(context);
        long now = System.currentTimeMillis();
        prefs.edit()
                .putBoolean(KEY_ACTIVE, active)
                .putBoolean(KEY_RUNNING, running)
                .putLong(KEY_TOTAL_MILLIS, Math.max(0L, totalMillis))
                .putLong(KEY_MILLIS_LEFT, Math.max(0L, millisLeft))
                .putLong(KEY_UPDATED_AT, now)
                .putString(KEY_MODULE_ID, safeText(moduleId))
                .putString(KEY_MODULE_TITLE, safeText(moduleTitle))
                .putString(KEY_SUBJECT, safeText(subject))
                .apply();
    }

    public static void updateTick(Context context, long millisLeft, long totalMillis) {
        SharedPreferences prefs = prefs(context);
        prefs.edit()
                .putLong(KEY_MILLIS_LEFT, Math.max(0L, millisLeft))
                .putLong(KEY_TOTAL_MILLIS, Math.max(0L, totalMillis))
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void setRunning(Context context, boolean running) {
        SharedPreferences prefs = prefs(context);
        prefs.edit()
                .putBoolean(KEY_RUNNING, running)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }

    public static void clear(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_ACTIVE, false)
                .putBoolean(KEY_RUNNING, false)
                .putLong(KEY_TOTAL_MILLIS, 0L)
                .putLong(KEY_MILLIS_LEFT, 0L)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .putString(KEY_MODULE_ID, "")
                .putString(KEY_MODULE_TITLE, "")
                .putString(KEY_SUBJECT, "")
                .apply();
    }

    public static Snapshot getSnapshot(Context context) {
        SharedPreferences prefs = prefs(context);
        Snapshot snapshot = new Snapshot();
        snapshot.active = prefs.getBoolean(KEY_ACTIVE, false);
        snapshot.running = prefs.getBoolean(KEY_RUNNING, false);
        snapshot.totalMillis = Math.max(0L, prefs.getLong(KEY_TOTAL_MILLIS, 0L));
        snapshot.millisLeft = Math.max(0L, prefs.getLong(KEY_MILLIS_LEFT, 0L));
        snapshot.updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L);
        snapshot.moduleId = safeText(prefs.getString(KEY_MODULE_ID, ""));
        snapshot.moduleTitle = safeText(prefs.getString(KEY_MODULE_TITLE, ""));
        snapshot.subject = safeText(prefs.getString(KEY_SUBJECT, ""));
        return snapshot;
    }

    public static long getDisplayMillisLeft(Snapshot snapshot, long now) {
        if (snapshot == null) {
            return 0L;
        }

        long left = Math.max(0L, snapshot.millisLeft);
        if (!snapshot.running) {
            return left;
        }

        if (snapshot.updatedAt <= 0L || now <= snapshot.updatedAt) {
            return left;
        }

        long elapsed = now - snapshot.updatedAt;
        return Math.max(0L, left - elapsed);
    }

    public static int getDisplayProgressPercent(Snapshot snapshot, long now) {
        if (snapshot == null || snapshot.totalMillis <= 0L) {
            return 0;
        }

        long left = getDisplayMillisLeft(snapshot, now);
        return (int) Math.max(0, Math.min(100, (left * 100L) / snapshot.totalMillis));
    }

    public static int getDisplayCompletionPercent(Snapshot snapshot, long now) {
        int remaining = getDisplayProgressPercent(snapshot, now);
        return Math.max(0, Math.min(100, 100 - remaining));
    }

    public static String formatTime(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    public static String formatProgressText(Snapshot snapshot, long now) {
        int progress = getDisplayCompletionPercent(snapshot, now);
        return progress + "% complete";
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String safeText(String value) {
        return value != null ? value.trim() : "";
    }

    public static final class Snapshot {
        public boolean active;
        public boolean running;
        public long totalMillis;
        public long millisLeft;
        public long updatedAt;
        public String moduleId;
        public String moduleTitle;
        public String subject;

        public String getDisplayModuleTitle() {
            if (!safeText(moduleTitle).isEmpty()) {
                return moduleTitle;
            }
            if (!safeText(subject).isEmpty()) {
                return subject;
            }
            return "Focus Session";
        }
    }
}
