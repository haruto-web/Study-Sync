package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.ui.tasks.TaskReminderScheduler;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private final Context appContext;
    private final TaskDao taskDao;
    private final FirebaseFirestore firestore;
    private final ProgressionRepository progressionRepository;

    public TaskRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.taskDao = AppDatabase.getInstance(appContext).taskDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.progressionRepository = new ProgressionRepository(appContext);
    }

    public LiveData<List<Task>> getAllTasksForUser(String userId) {
        return taskDao.getAllTasksForUser(userId);
    }

    public LiveData<List<Task>> getActiveTasksForUser(String userId) {
        return taskDao.getActiveTasksForUser(userId);
    }

    public LiveData<List<Task>> getCompletedTasksForUser(String userId) {
        return taskDao.getCompletedTasksForUser(userId);
    }

    public LiveData<List<Task>> getTasksByPriority(String userId, String priority) {
        return taskDao.getTasksByPriority(userId, priority);
    }

    public LiveData<List<Task>> getTasksByCategory(String userId, String category) {
        return taskDao.getTasksByCategory(userId, category);
    }

    public LiveData<List<Task>> getOverdueTasksForUser(String userId) {
        return taskDao.getOverdueTasksForUser(userId, System.currentTimeMillis());
    }

    public LiveData<Task> getTaskById(String taskId) {
        return taskDao.getTaskById(taskId);
    }

    public void createTask(Task task, String userId) {
        task.setUserId(userId);
        normalizeTimeline(task);
        // Write to Room immediately for offline-first
        AppExecutors.diskIO().execute(() -> {
            taskDao.insertTask(task);
            if (task.isCompleted()) {
                TaskReminderScheduler.cancelForTask(appContext, task);
            } else {
                TaskReminderScheduler.scheduleForTask(appContext, task);
            }
        });
        // Sync to Firestore
        firestore.collection("tasks").document(task.getTaskId()).set(task)
                .addOnFailureListener(Throwable::printStackTrace);
        progressionRepository.recomputeProgressionAsync(userId);
    }

    public void updateTask(Task task) {
        normalizeTimeline(task);
        task.setUpdatedAt(System.currentTimeMillis());
        AppExecutors.diskIO().execute(() -> {
            Task existing = taskDao.getTaskByIdSync(task.getTaskId());
            if (existing != null) {
                TaskReminderScheduler.cancelForTask(appContext, existing);
            }

            taskDao.updateTask(task);
            if (task.isCompleted()) {
                TaskReminderScheduler.cancelForTask(appContext, task);
            } else {
                TaskReminderScheduler.scheduleForTask(appContext, task);
            }
        });
        firestore.collection("tasks").document(task.getTaskId()).set(task)
                .addOnFailureListener(Throwable::printStackTrace);
        if (task.getUserId() != null) {
            progressionRepository.recomputeProgressionAsync(task.getUserId());
        }
    }

    public void completeTask(String taskId) {
        AppExecutors.diskIO().execute(() -> {
            Task task = taskDao.getTaskByIdSync(taskId);
            if (task != null) {
                TaskReminderScheduler.cancelForTask(appContext, task);
                task.setCompleted(true);
                taskDao.updateTask(task);
                firestore.collection("tasks").document(taskId).set(task);
                if (task.getUserId() != null) {
                    progressionRepository.recomputeAndPersistForSync(task.getUserId());
                }
            }
        });
    }

    public void deleteTask(String taskId) {
        AppExecutors.diskIO().execute(() -> {
            Task task = taskDao.getTaskByIdSync(taskId);
            if (task != null) {
                TaskReminderScheduler.cancelForTask(appContext, task);
            }
            taskDao.deleteTaskById(taskId);
            if (task != null && task.getUserId() != null) {
                progressionRepository.recomputeAndPersistForSync(task.getUserId());
            }
        });
        firestore.collection("tasks").document(taskId).delete();
    }

    public void syncTasksFromFirestore(String userId) {
        firestore.collection("tasks").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snap -> {
                    List<Task> tasks = snap.toObjects(Task.class);
                    AppExecutors.diskIO().execute(() -> {
                        List<Task> safeTasks = tasks != null ? tasks : new ArrayList<>();
                        for (Task task : safeTasks) {
                            normalizeTimeline(task);
                        }
                        taskDao.insertAllTasks(safeTasks);
                        TaskReminderScheduler.rescheduleAll(appContext, safeTasks);
                        progressionRepository.recomputeAndPersistForSync(userId);
                    });
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<Integer> getActiveTaskCountForUser(String userId) {
        return taskDao.getActiveTaskCountForUser(userId);
    }

    public LiveData<Integer> getCompletedTaskCountForUser(String userId) {
        return taskDao.getCompletedTaskCountForUser(userId);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(() -> {
            List<Task> activeTasks = taskDao.getAllActiveTasksSync();
            if (activeTasks != null) {
                for (Task task : activeTasks) {
                    TaskReminderScheduler.cancelForTask(appContext, task);
                }
            }
            taskDao.clearAllTasks();
        });
    }

    private static void normalizeTimeline(Task task) {
        if (task == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long createdAt = task.getCreatedAt() > 0L ? task.getCreatedAt() : now;
        long startDate = task.getStartDate() > 0L ? task.getStartDate() : createdAt;
        long dueDate = task.getDueDate() > 0L ? task.getDueDate() : startDate;

        if (startDate > dueDate) {
            dueDate = startDate;
        }

        task.setStartDate(startDate);
        task.setDueDate(dueDate);
    }
}
