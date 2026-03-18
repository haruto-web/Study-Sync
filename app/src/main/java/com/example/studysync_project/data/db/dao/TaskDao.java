package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.Task;

import java.util.List;

/**
 * Data Access Object for Task entity
 */
@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTask(Task task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllTasks(List<Task> tasks);

    @Update
    void updateTask(Task task);

    @Delete
    void deleteTask(Task task);

    @Query("DELETE FROM tasks WHERE taskId = :taskId")
    void deleteTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE taskId = :taskId")
    LiveData<Task> getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE userId = :userId ORDER BY dueDate ASC, priority DESC")
    LiveData<List<Task>> getAllTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    LiveData<List<Task>> getActiveTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 1 ORDER BY completedAt DESC")
    LiveData<List<Task>> getCompletedTasksForUser(String userId);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND priority = :priority AND isCompleted = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByPriority(String userId, String priority);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND category = :category AND isCompleted = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByCategory(String userId, String category);

    @Query("SELECT * FROM tasks WHERE userId = :userId AND isCompleted = 0 AND dueDate <= :dueDate ORDER BY dueDate ASC")
    LiveData<List<Task>> getOverdueTasksForUser(String userId, long dueDate);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isCompleted = 0")
    LiveData<Integer> getActiveTaskCountForUser(String userId);

    @Query("SELECT COUNT(*) FROM tasks WHERE userId = :userId AND isCompleted = 1")
    LiveData<Integer> getCompletedTaskCountForUser(String userId);

    @Query("DELETE FROM tasks WHERE userId = :userId")
    void deleteAllTasksForUser(String userId);

    @Query("DELETE FROM tasks")
    void clearAllTasks();
}
