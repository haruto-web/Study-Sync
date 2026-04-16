package com.example.studysync_project.ui.tasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.studysync_project.data.model.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class TaskReminderScheduler {

    public static final String ACTION_TASK_REMINDER =
            "com.example.studysync_project.action.TASK_REMINDER";

    static final String EXTRA_TASK_ID = "extra_task_id";
    static final String EXTRA_TASK_TITLE = "extra_task_title";
    static final String EXTRA_TASK_PRIORITY = "extra_task_priority";
    static final String EXTRA_TASK_CATEGORY = "extra_task_category";
    static final String EXTRA_DUE_MILLIS = "extra_due_millis";
    static final String EXTRA_DAYS_REMAINING = "extra_days_remaining";
    static final String EXTRA_IS_DEADLINE_DAY = "extra_is_deadline_day";
    static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";

    static final String CHANNEL_ID = "task_deadline_channel";

    private static final int DEFAULT_DUE_HOUR = 19;
    private static final int DEFAULT_DUE_MINUTE = 0;
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;
    private static final int MAX_DAILY_REMINDERS = 120;

    private TaskReminderScheduler() {
    }

    public static void scheduleForTask(Context context, Task task) {
        if (context == null || task == null || task.isCompleted()) {
            return;
        }

        String taskId = safe(task.getTaskId());
        if (taskId.isEmpty()) {
            return;
        }

        cancelForTask(context, task);

        ReminderPlan plan = buildReminderPlan(task);
        if (plan == null || plan.triggerTimes.isEmpty()) {
            return;
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (int index = 0; index < plan.triggerTimes.size(); index++) {
            long triggerAt = plan.triggerTimes.get(index);
            if (triggerAt <= now) {
                continue;
            }

            long dayMillis = plan.startDayMillis + (index * DAY_MILLIS);
            boolean deadlineDay = dayMillis >= plan.dueDayMillis;
            int daysRemaining = (int) Math.max(0L, (plan.dueDayMillis - dayMillis) / DAY_MILLIS);
            int notificationId = buildNotificationId(taskId, index);

            Intent reminderIntent = new Intent(context, TaskReminderReceiver.class)
                    .setAction(ACTION_TASK_REMINDER)
                    .putExtra(EXTRA_TASK_ID, taskId)
                    .putExtra(EXTRA_TASK_TITLE, safe(task.getTitle()))
                    .putExtra(EXTRA_TASK_PRIORITY, normalizePriority(task.getPriority()))
                    .putExtra(EXTRA_TASK_CATEGORY, safe(task.getCategory()))
                    .putExtra(EXTRA_DUE_MILLIS, plan.dueMillis)
                    .putExtra(EXTRA_DAYS_REMAINING, daysRemaining)
                    .putExtra(EXTRA_IS_DEADLINE_DAY, deadlineDay)
                    .putExtra(EXTRA_NOTIFICATION_ID, notificationId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    buildRequestCode(taskId, index),
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    public static void cancelForTask(Context context, Task task) {
        if (context == null || task == null) {
            return;
        }

        String taskId = safe(task.getTaskId());
        if (taskId.isEmpty()) {
            return;
        }

        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) {
            return;
        }

        ReminderPlan plan = buildReminderPlan(task);
        int expectedReminders = plan != null ? plan.triggerTimes.size() : MAX_DAILY_REMINDERS;

        for (int index = 0; index < expectedReminders; index++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    buildRequestCode(taskId, index),
                    new Intent(context, TaskReminderReceiver.class).setAction(ACTION_TASK_REMINDER),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }

        // Fallback to clean stale request codes when a task range shrinks significantly.
        for (int index = expectedReminders; index < MAX_DAILY_REMINDERS; index++) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    buildRequestCode(taskId, index),
                    new Intent(context, TaskReminderReceiver.class).setAction(ACTION_TASK_REMINDER),
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    public static void rescheduleAll(Context context, List<Task> tasks) {
        if (context == null || tasks == null) {
            return;
        }

        for (Task task : tasks) {
            if (task == null) {
                continue;
            }
            if (task.isCompleted()) {
                cancelForTask(context, task);
            } else {
                scheduleForTask(context, task);
            }
        }
    }

    private static ReminderPlan buildReminderPlan(Task task) {
        if (task == null) {
            return null;
        }

        long startMillis = resolveStartMillis(task);
        long dueMillis = resolveDueMillis(task, startMillis);
        if (startMillis <= 0L || dueMillis <= 0L) {
            return null;
        }

        long startDay = startOfDay(startMillis);
        long dueDay = startOfDay(dueMillis);
        if (startDay > dueDay) {
            long temp = startDay;
            startDay = dueDay;
            dueDay = temp;
            dueMillis = Math.max(dueMillis, startMillis);
        }

        Calendar dueCalendar = Calendar.getInstance();
        dueCalendar.setTimeInMillis(dueMillis);
        int dueHour = dueCalendar.get(Calendar.HOUR_OF_DAY);
        int dueMinute = dueCalendar.get(Calendar.MINUTE);
        if (dueHour == 0 && dueMinute == 0) {
            dueHour = DEFAULT_DUE_HOUR;
            dueMinute = DEFAULT_DUE_MINUTE;
            Calendar adjustedDue = Calendar.getInstance();
            adjustedDue.setTimeInMillis(dueDay);
            adjustedDue.set(Calendar.HOUR_OF_DAY, dueHour);
            adjustedDue.set(Calendar.MINUTE, dueMinute);
            adjustedDue.set(Calendar.SECOND, 0);
            adjustedDue.set(Calendar.MILLISECOND, 0);
            dueMillis = adjustedDue.getTimeInMillis();
        }

        long totalDays = Math.max(1L, ((dueDay - startDay) / DAY_MILLIS) + 1L);
        if (totalDays > MAX_DAILY_REMINDERS) {
            startDay = dueDay - ((MAX_DAILY_REMINDERS - 1L) * DAY_MILLIS);
        }

        List<Long> triggerTimes = new ArrayList<>();
        Calendar dayCursor = Calendar.getInstance();
        dayCursor.setTimeInMillis(startDay);
        while (dayCursor.getTimeInMillis() <= dueDay && triggerTimes.size() < MAX_DAILY_REMINDERS) {
            Calendar trigger = (Calendar) dayCursor.clone();
            trigger.set(Calendar.HOUR_OF_DAY, dueHour);
            trigger.set(Calendar.MINUTE, dueMinute);
            trigger.set(Calendar.SECOND, 0);
            trigger.set(Calendar.MILLISECOND, 0);
            triggerTimes.add(trigger.getTimeInMillis());
            dayCursor.add(Calendar.DAY_OF_YEAR, 1);
        }

        return new ReminderPlan(startDay, dueDay, dueMillis, triggerTimes);
    }

    private static long resolveStartMillis(Task task) {
        if (task == null) {
            return 0L;
        }
        if (task.getStartDate() > 0L) {
            return task.getStartDate();
        }
        if (task.getCreatedAt() > 0L) {
            return task.getCreatedAt();
        }
        return task.getDueDate();
    }

    private static long resolveDueMillis(Task task, long fallbackStart) {
        if (task == null) {
            return fallbackStart;
        }
        long due = task.getDueDate() > 0L ? task.getDueDate() : fallbackStart;
        return Math.max(due, fallbackStart);
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static int buildRequestCode(String taskId, int dayIndex) {
        return (taskId + "#" + dayIndex).hashCode();
    }

    private static int buildNotificationId(String taskId, int dayIndex) {
        return (taskId + "@" + dayIndex).hashCode();
    }

    private static String normalizePriority(String value) {
        if ("HIGH".equalsIgnoreCase(value)) {
            return "HIGH";
        }
        if ("LOW".equalsIgnoreCase(value)) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private static final class ReminderPlan {
        final long startDayMillis;
        final long dueDayMillis;
        final long dueMillis;
        final List<Long> triggerTimes;

        ReminderPlan(long startDayMillis, long dueDayMillis, long dueMillis, List<Long> triggerTimes) {
            this.startDayMillis = startDayMillis;
            this.dueDayMillis = dueDayMillis;
            this.dueMillis = dueMillis;
            this.triggerTimes = triggerTimes;
        }
    }
}
