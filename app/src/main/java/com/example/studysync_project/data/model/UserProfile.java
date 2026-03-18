package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents user profile information
 */
@Entity(tableName = "users")
public class UserProfile {
    @PrimaryKey
    @NonNull
    public String userId;

    public String email;
    public String fullName;
    public String profileImageUrl;
    public String bio;

    public long createdAt;
    public long updatedAt;
    public long lastLogin;

    public int totalQuizzesTaken;
    public int totalTasksCompleted;
    public int totalStudyMinutes;
    public double averageQuizScore;

    // Constructors
    public UserProfile() {
        this.userId = ""; 
    }

    public UserProfile(@NonNull String userId, String email, String fullName) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.lastLogin = System.currentTimeMillis();
        this.totalQuizzesTaken = 0;
        this.totalTasksCompleted = 0;
        this.totalStudyMinutes = 0;
        this.averageQuizScore = 0.0;
    }

    // Getters
    @NonNull
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getBio() { return bio; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public long getLastLogin() { return lastLogin; }
    public int getTotalQuizzesTaken() { return totalQuizzesTaken; }
    public int getTotalTasksCompleted() { return totalTasksCompleted; }
    public int getTotalStudyMinutes() { return totalStudyMinutes; }
    public double getAverageQuizScore() { return averageQuizScore; }

    // Setters
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    public void setEmail(String email) { this.email = email; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setBio(String bio) { this.bio = bio; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
    public void setTotalQuizzesTaken(int totalQuizzesTaken) { this.totalQuizzesTaken = totalQuizzesTaken; }
    public void setTotalTasksCompleted(int totalTasksCompleted) { this.totalTasksCompleted = totalTasksCompleted; }
    public void setTotalStudyMinutes(int totalStudyMinutes) { this.totalStudyMinutes = totalStudyMinutes; }
    public void setAverageQuizScore(double averageQuizScore) { this.averageQuizScore = averageQuizScore; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
