package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.PropertyName;

/**
 * Represents a reusable study module (content used for review and quiz generation).
 */
@Entity(tableName = "study_modules")
public class StudyModule {
    public static final String PROGRESSION_NEW = "NEW";
    public static final String PROGRESSION_IN_PROGRESS = "IN_PROGRESS";
    public static final String PROGRESSION_MASTERED = "MASTERED";

    @PrimaryKey
    @NonNull
    public String moduleId;

    public String userId;
    public String title;
    public String subject;
    public String topic;
    public String description;
    public String contentText;
    public String sourceType;
    public String sourceRef;

    @PropertyName("created_at")
    public long createdAt;

    @PropertyName("updated_at")
    public long updatedAt;

    public boolean isArchived;

    public String progressionState;
    public int unlockOrder;
    public boolean isUnlocked;
    public long startedAt;
    public long completedAt;
    public double masteryScore;
    public int masteryAttempts;

    public StudyModule() {
        this.moduleId = "";
        applyDefaultProgression();
    }

    @Ignore
    public StudyModule(
            String userId,
            String title,
            String subject,
            String topic,
            String description,
            String contentText,
            String sourceType,
            String sourceRef
    ) {
        this.moduleId = "";
        this.userId = userId;
        this.title = title;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.contentText = contentText;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isArchived = false;
        applyDefaultProgression();
    }

    private void applyDefaultProgression() {
        this.progressionState = PROGRESSION_NEW;
        this.unlockOrder = 0;
        this.isUnlocked = true;
        this.startedAt = 0L;
        this.completedAt = 0L;
        this.masteryScore = 0.0;
        this.masteryAttempts = 0;
    }

    @NonNull
    public String getModuleId() { return moduleId; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public String getContentText() { return contentText; }
    public String getSourceType() { return sourceType; }
    public String getSourceRef() { return sourceRef; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public boolean isArchived() { return isArchived; }
    public String getProgressionState() { return progressionState; }
    public int getUnlockOrder() { return unlockOrder; }
    public boolean isUnlocked() { return isUnlocked; }
    public long getStartedAt() { return startedAt; }
    public long getCompletedAt() { return completedAt; }
    public double getMasteryScore() { return masteryScore; }
    public int getMasteryAttempts() { return masteryAttempts; }

    public void setModuleId(@NonNull String moduleId) { this.moduleId = moduleId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setTopic(String topic) { this.topic = topic; }
    public void setDescription(String description) { this.description = description; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setArchived(boolean archived) { isArchived = archived; }
    public void setProgressionState(String progressionState) { this.progressionState = progressionState; }
    public void setUnlockOrder(int unlockOrder) { this.unlockOrder = unlockOrder; }
    public void setUnlocked(boolean unlocked) { isUnlocked = unlocked; }
    public void setStartedAt(long startedAt) { this.startedAt = startedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public void setMasteryScore(double masteryScore) { this.masteryScore = masteryScore; }
    public void setMasteryAttempts(int masteryAttempts) { this.masteryAttempts = masteryAttempts; }
}
