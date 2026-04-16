package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a study task/to-do item
 */
@Entity(tableName = "tasks")
public class Task {
    @PrimaryKey
    @NonNull
    public String taskId;

    public String userId;
    public String title;
    public String description;

    @PropertyName("start_date")
    public long startDate;  // Timestamp

    @PropertyName("due_date")
    public long dueDate;  // Timestamp

    @PropertyName("is_completed")
    public boolean isCompleted;

    public String priority;  // LOW, MEDIUM, HIGH
    public String category;  // Subject, Topic, etc.

    @PropertyName("created_at")
    public long createdAt;

    @PropertyName("updated_at")
    public long updatedAt;

    @PropertyName("completed_at")
    public long completedAt;  // When task was marked complete

    // Constructors
    public Task() {
        this.taskId = "";
        this.startDate = 0L;
        this.dueDate = 0L;
    }

    @Ignore
    public Task(String userId, String title, String description, long dueDate,
                String priority, String category) {
        this(userId, title, description, System.currentTimeMillis(), dueDate, priority, category);
    }

    @Ignore
    public Task(String userId, String title, String description, long startDate, long dueDate,
                String priority, String category) {
        this.taskId = "";
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.priority = priority;
        this.category = category;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters
    @NonNull
    public String getTaskId() { return taskId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public long getStartDate() { return startDate; }
    public long getDueDate() { return dueDate; }
    public boolean isCompleted() { return isCompleted; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getCompletedAt() { return completedAt; }

    // Setters
    public void setTaskId(@NonNull String taskId) { this.taskId = taskId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
        if (completed) {
            this.completedAt = System.currentTimeMillis();
        }
    }
    public void setPriority(String priority) { this.priority = priority; }
    public void setCategory(String category) { this.category = category; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
