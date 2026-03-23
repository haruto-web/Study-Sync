package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class TaskRepository {
    private final TaskDao taskDao;
    private final FirebaseFirestore firestore;

    public TaskRepository(Context context) {
        this.taskDao = AppDatabase.getInstance(context).taskDao();
        this.firestore = FirebaseFirestore.getInstance();
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
        // Write to Room immediately for offline-first
        AppExecutors.diskIO().execute(() -> taskDao.insertTask(task));
        // Sync to Firestore
        firestore.collection("tasks").document(task.getTaskId()).set(task)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateTask(Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        AppExecutors.diskIO().execute(() -> taskDao.updateTask(task));
        firestore.collection("tasks").document(task.getTaskId()).set(task)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void completeTask(String taskId) {
        AppExecutors.diskIO().execute(() -> {
            Task task = taskDao.getTaskByIdSync(taskId);
            if (task != null) {
                task.setCompleted(true);
                taskDao.updateTask(task);
                firestore.collection("tasks").document(taskId).set(task);
            }
        });
    }

    public void deleteTask(String taskId) {
        AppExecutors.diskIO().execute(() -> taskDao.deleteTaskById(taskId));
        firestore.collection("tasks").document(taskId).delete();
    }

    public void syncTasksFromFirestore(String userId) {
        firestore.collection("tasks").whereEqualTo("userId", userId).get()
                .addOnSuccessListener(snap -> {
                    List<Task> tasks = snap.toObjects(Task.class);
                    AppExecutors.diskIO().execute(() -> taskDao.insertAllTasks(tasks));
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
        AppExecutors.diskIO().execute(taskDao::clearAllTasks);
    }
}
