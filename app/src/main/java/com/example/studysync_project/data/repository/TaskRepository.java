package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.model.Task;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for Task data
 * Handles communication between Firestore, Room database, and UI
 */
public class TaskRepository {
    private final TaskDao taskDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public TaskRepository(Context context) {
        this.context = context;
        this.taskDao = AppDatabase.getInstance(context).taskDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get all tasks for user
     */
    public LiveData<List<Task>> getAllTasksForUser(String userId) {
        return taskDao.getAllTasksForUser(userId);
    }

    /**
     * Get only active (incomplete) tasks for user
     */
    public LiveData<List<Task>> getActiveTasksForUser(String userId) {
        return taskDao.getActiveTasksForUser(userId);
    }

    /**
     * Get only completed tasks for user
     */
    public LiveData<List<Task>> getCompletedTasksForUser(String userId) {
        return taskDao.getCompletedTasksForUser(userId);
    }

    /**
     * Get tasks by priority
     */
    public LiveData<List<Task>> getTasksByPriority(String userId, String priority) {
        return taskDao.getTasksByPriority(userId, priority);
    }

    /**
     * Get tasks by category
     */
    public LiveData<List<Task>> getTasksByCategory(String userId, String category) {
        return taskDao.getTasksByCategory(userId, category);
    }

    /**
     * Get overdue tasks
     */
    public LiveData<List<Task>> getOverdueTasksForUser(String userId) {
        return taskDao.getOverdueTasksForUser(userId, System.currentTimeMillis());
    }

    /**
     * Get a single task by ID
     */
    public LiveData<Task> getTaskById(String taskId) {
        return taskDao.getTaskById(taskId);
    }

    /**
     * Create a new task
     */
    public void createTask(Task task, String userId) {
        task.setUserId(userId);
        
        // Save to Firestore
        firestore.collection("tasks")
            .document(task.getTaskId())
            .set(task)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                taskDao.insertTask(task);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Update an existing task
     */
    public void updateTask(Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        
        // Update in Firestore
        firestore.collection("tasks")
            .document(task.getTaskId())
            .set(task)
            .addOnSuccessListener(aVoid -> {
                // Update in Room
                taskDao.updateTask(task);
            });
    }

    /**
     * Mark task as complete
     */
    public void completeTask(String taskId) {
        LiveData<Task> taskLiveData = taskDao.getTaskById(taskId);
        Task task = taskLiveData.getValue();
        
        if (task != null) {
            task.setCompleted(true);
            updateTask(task);
        }
    }

    /**
     * Delete a task
     */
    public void deleteTask(String taskId) {
        firestore.collection("tasks")
            .document(taskId)
            .delete();
        
        taskDao.deleteTaskById(taskId);
    }

    /**
     * Sync tasks from Firestore to Room database
     */
    public void syncTasksFromFirestore(String userId) {
        firestore.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Task> tasks = queryDocumentSnapshots.toObjects(Task.class);
                taskDao.insertAllTasks(tasks);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Get count of active tasks
     */
    public LiveData<Integer> getActiveTaskCountForUser(String userId) {
        return taskDao.getActiveTaskCountForUser(userId);
    }

    /**
     * Get count of completed tasks
     */
    public LiveData<Integer> getCompletedTaskCountForUser(String userId) {
        return taskDao.getCompletedTaskCountForUser(userId);
    }

    /**
     * Clear all local task data
     */
    public void clearLocalData() {
        taskDao.clearAllTasks();
    }
}
