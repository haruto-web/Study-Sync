package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a study timer session (pomodoro, timed study, etc.)
 */
@Entity(tableName = "timer_sessions")
public class TimerSession {
    @PrimaryKey
    @NonNull
    public String sessionId;

    public String userId;

    @PropertyName("start_time")
    public long startTime;  // Timestamp when session started

    @PropertyName("end_time")
    public long endTime;  // Timestamp when session ended (0 if ongoing)

    @PropertyName("duration_minutes")
    public int durationMinutes;  // Planned duration

    @PropertyName("actual_duration_minutes")
    public int actualDurationMinutes;  // Actual time spent

    public String subject;  // Math, Science, etc.
    public String notes;  // Notes about the session
    public boolean isCompleted;

    @PropertyName("paused_duration")
    public long pausedDuration;  // Total time paused

    @PropertyName("is_paused")
    public boolean isPaused;

    @PropertyName("created_at")
    public long createdAt;

    // Constructors
    public TimerSession() {
        this.sessionId = "";
    }

    @Ignore
    public TimerSession(String userId, int durationMinutes, String subject, String notes) {
        this.sessionId = "";
        this.userId = userId;
        this.durationMinutes = durationMinutes;
        this.subject = subject;
        this.notes = notes;
        this.startTime = System.currentTimeMillis();
        this.createdAt = System.currentTimeMillis();
        this.isCompleted = false;
        this.isPaused = false;
        this.pausedDuration = 0;
        this.actualDurationMinutes = 0;
    }

    // Getters
    @NonNull
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getActualDurationMinutes() { return actualDurationMinutes; }
    public String getSubject() { return subject; }
    public String getNotes() { return notes; }
    public boolean isCompleted() { return isCompleted; }
    public long getPausedDuration() { return pausedDuration; }
    public boolean isPaused() { return isPaused; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setSessionId(@NonNull String sessionId) { this.sessionId = sessionId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setActualDurationMinutes(int actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }
    public void setCompleted(boolean completed) { this.isCompleted = completed; }
    public void setPaused(boolean paused) { this.isPaused = paused; }
    public void addPausedDuration(long duration) { this.pausedDuration += duration; }
    public void setNotes(String notes) { this.notes = notes; }
}
