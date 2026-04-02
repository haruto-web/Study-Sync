package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.PropertyName;

/**
 * Represents a reusable study module (content used for review and quiz generation).
 */
@Entity(tableName = "study_modules")
public class StudyModule {
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

    public StudyModule() {
        this.moduleId = "";
    }

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
}
