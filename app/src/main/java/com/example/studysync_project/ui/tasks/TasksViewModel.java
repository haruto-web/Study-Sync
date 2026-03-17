package com.example.studysync_project.ui.tasks;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.data.repository.TaskRepository;

import java.util.List;

/**
 * ViewModel for Tasks feature
 * Manages task data and filtering
 */
public class TasksViewModel extends ViewModel {
    private final TaskRepository taskRepository;
    private LiveData<List<Task>> allTasks;
    private LiveData<List<Task>> activeTasks;
    private LiveData<List<Task>> completedTasks;
    private LiveData<Integer> activeTaskCount;

    public TasksViewModel(Context context) {
        this.taskRepository = new TaskRepository(context);
    }

    /**
     * Get all tasks for user
     */
    public LiveData<List<Task>> getAllTasksForUser(String userId) {
        activeTasks = taskRepository.getActiveTasksForUser(userId);
        return activeTasks;
    }

    /**
     * Get completed tasks
     */
    public LiveData<List<Task>> getCompletedTasks(String userId) {
        completedTasks = taskRepository.getCompletedTasksForUser(userId);
        return completedTasks;
    }

    /**
     * Get tasks by priority
     */
    public LiveData<List<Task>> getTasksByPriority(String userId, String priority) {
        return taskRepository.getTasksByPriority(userId, priority);
    }

    /**
     * Get tasks by category
     */
    public LiveData<List<Task>> getTasksByCategory(String userId, String category) {
        return taskRepository.getTasksByCategory(userId, category);
    }

    /**
     * Get overdue tasks
     */
    public LiveData<List<Task>> getOverdueTasks(String userId) {
        return taskRepository.getOverdueTasksForUser(userId);
    }

    /**
     * Get a single task
     */
    public LiveData<Task> getTaskById(String taskId) {
        return taskRepository.getTaskById(taskId);
    }

    /**
     * Create a new task
     */
    public void createTask(Task task, String userId) {
        taskRepository.createTask(task, userId);
    }

    /**
     * Update a task
     */
    public void updateTask(Task task) {
        taskRepository.updateTask(task);
    }

    /**
     * Complete a task
     */
    public void completeTask(String taskId) {
        taskRepository.completeTask(taskId);
    }

    /**
     * Delete a task
     */
    public void deleteTask(String taskId) {
        taskRepository.deleteTask(taskId);
    }

    /**
     * Sync tasks from Firestore
     */
    public void syncTasks(String userId) {
        taskRepository.syncTasksFromFirestore(userId);
    }

    /**
     * Get count of active tasks
     */
    public LiveData<Integer> getActiveTaskCount(String userId) {
        activeTaskCount = taskRepository.getActiveTaskCountForUser(userId);
        return activeTaskCount;
    }

    /**
     * Get all active tasks
     */
    public LiveData<List<Task>> getActiveTasks() {
        return activeTasks;
    }
}
