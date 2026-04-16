package com.example.studysync_project.ui.tasks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.utils.AppExecutors;

import java.util.List;

public class TaskReminderBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        PendingResult pendingResult = goAsync();
        Context appContext = context.getApplicationContext();
        AppExecutors.diskIO().execute(() -> {
            try {
                List<Task> activeTasks = AppDatabase.getInstance(appContext)
                        .taskDao()
                        .getAllActiveTasksSync();
                TaskReminderScheduler.rescheduleAll(appContext, activeTasks);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
