package com.example.studysync_project.ui.tasks;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_NAME = "Task Deadlines";
    private static final String CHANNEL_DESCRIPTION = "Daily reminders for task timeline and due dates";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        if (!TaskReminderScheduler.ACTION_TASK_REMINDER.equals(intent.getAction())) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String taskId = safe(intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_ID));
        String taskTitle = safe(intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_TITLE));
        String taskPriority = safe(intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_PRIORITY));
        String taskCategory = safe(intent.getStringExtra(TaskReminderScheduler.EXTRA_TASK_CATEGORY));
        long dueMillis = intent.getLongExtra(TaskReminderScheduler.EXTRA_DUE_MILLIS, 0L);
        int daysRemaining = Math.max(0, intent.getIntExtra(TaskReminderScheduler.EXTRA_DAYS_REMAINING, 0));
        boolean isDeadlineDay = intent.getBooleanExtra(TaskReminderScheduler.EXTRA_IS_DEADLINE_DAY, false);
        int notificationId = intent.getIntExtra(
                TaskReminderScheduler.EXTRA_NOTIFICATION_ID,
                (taskId + "#notif").hashCode()
        );

        String title = taskTitle.isEmpty() ? "Task reminder" : taskTitle;
        String priority = taskPriority.isEmpty() ? "MEDIUM" : taskPriority.toUpperCase(Locale.US);
        String category = taskCategory.isEmpty() ? "General" : taskCategory;

        String dueDateText = dueMillis > 0L
                ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(dueMillis))
                : "today";
        String dueTimeText = dueMillis > 0L
                ? new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(dueMillis))
                : "soon";

        String notificationTitle;
        String notificationBody;
        if (isDeadlineDay) {
            notificationTitle = "Deadline Today: " + title;
            notificationBody = "Due at " + dueTimeText
                    + " • Priority auto-escalated to HIGH"
                    + " • " + category;
        } else if (daysRemaining <= 1) {
            notificationTitle = "Due Tomorrow: " + title;
            notificationBody = "Due " + dueDateText + " at " + dueTimeText
                    + " • Priority: " + priority
                    + " • " + category;
        } else {
            notificationTitle = "Planner Reminder: " + title;
            notificationBody = "Due in " + daysRemaining + " days"
                    + " (" + dueDateText + " " + dueTimeText + ")"
                    + " • Priority: " + priority;
        }

        createChannelIfNeeded(context);

        Intent openTasksIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_OPEN_TAB_ID, R.id.tasksFragment)
                .putExtra("open_task_id", taskId);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (taskId + "#open").hashCode(),
                openTasksIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TaskReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationBody))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(notificationId, builder.build());
        }
    }

    private static void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = manager.getNotificationChannel(TaskReminderScheduler.CHANNEL_ID);
        if (channel != null) {
            return;
        }

        NotificationChannel newChannel = new NotificationChannel(
                TaskReminderScheduler.CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        newChannel.setDescription(CHANNEL_DESCRIPTION);
        manager.createNotificationChannel(newChannel);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
