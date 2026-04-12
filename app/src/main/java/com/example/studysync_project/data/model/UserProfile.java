package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    // Progression analytics (MVP)
    public double progressionIndex;
    public double progressionDelta;
    public String progressionState;
    public int currentStreakDays;
    public int longestStreakDays;
    public int studyMinutesLast7Days;
    public double averageQuizScoreLast7Days;
    public String strongestSubject;
    public String focusSubject;
    public String unlockedBadgesCsv;
    public String lastUnlockedBadge;
    public long lastBadgeUnlockedAt;
    public long lastProgressComputedAt;

    // Consent + onboarding (versioned)
    public boolean termsAccepted;
    public long termsAcceptedAt;
    public int termsVersion;
    public boolean personalizationEnabled;

    public String username;
    public int age;
    public String gradeLevel;
    public String strand;
    public String goal;
    public String subjectsCsv;
    public String topicsOfInterestCsv;
    public int weeklyStudyTargetMinutes;

    // Constructors
    public UserProfile() {
        this.userId = ""; 
    }

    @Ignore
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

        this.progressionIndex = 0.0;
        this.progressionDelta = 0.0;
        this.progressionState = "STARTING";
        this.currentStreakDays = 0;
        this.longestStreakDays = 0;
        this.studyMinutesLast7Days = 0;
        this.averageQuizScoreLast7Days = 0.0;
        this.strongestSubject = "";
        this.focusSubject = "";
        this.unlockedBadgesCsv = "";
        this.lastUnlockedBadge = "";
        this.lastBadgeUnlockedAt = 0L;
        this.lastProgressComputedAt = 0L;

        this.termsAccepted = false;
        this.termsAcceptedAt = 0L;
        this.termsVersion = 0;
        this.personalizationEnabled = false;

        this.gradeLevel = null;
        this.strand = null;
        this.goal = null;
        this.subjectsCsv = null;
        this.topicsOfInterestCsv = null;
        this.weeklyStudyTargetMinutes = 0;
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

    public double getProgressionIndex() { return progressionIndex; }
    public double getProgressionDelta() { return progressionDelta; }
    public String getProgressionState() { return progressionState; }
    public int getCurrentStreakDays() { return currentStreakDays; }
    public int getLongestStreakDays() { return longestStreakDays; }
    public int getStudyMinutesLast7Days() { return studyMinutesLast7Days; }
    public double getAverageQuizScoreLast7Days() { return averageQuizScoreLast7Days; }
    public String getStrongestSubject() { return strongestSubject; }
    public String getFocusSubject() { return focusSubject; }
    public String getUnlockedBadgesCsv() { return unlockedBadgesCsv; }
    public String getLastUnlockedBadge() { return lastUnlockedBadge; }
    public long getLastBadgeUnlockedAt() { return lastBadgeUnlockedAt; }
    public long getLastProgressComputedAt() { return lastProgressComputedAt; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public long getTermsAcceptedAt() { return termsAcceptedAt; }
    public int getTermsVersion() { return termsVersion; }
    public boolean isPersonalizationEnabled() { return personalizationEnabled; }

    public String getUsername() { return username; }
    public int getAge() { return age; }
    public String getGradeLevel() { return gradeLevel; }
    public String getStrand() { return strand; }
    public String getGoal() { return goal; }
    public String getSubjectsCsv() { return subjectsCsv; }
    public String getTopicsOfInterestCsv() { return topicsOfInterestCsv; }
    public int getWeeklyStudyTargetMinutes() { return weeklyStudyTargetMinutes; }

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

    public void setProgressionIndex(double progressionIndex) { this.progressionIndex = progressionIndex; }
    public void setProgressionDelta(double progressionDelta) { this.progressionDelta = progressionDelta; }
    public void setProgressionState(String progressionState) { this.progressionState = progressionState; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }
    public void setLongestStreakDays(int longestStreakDays) { this.longestStreakDays = longestStreakDays; }
    public void setStudyMinutesLast7Days(int studyMinutesLast7Days) { this.studyMinutesLast7Days = studyMinutesLast7Days; }
    public void setAverageQuizScoreLast7Days(double averageQuizScoreLast7Days) { this.averageQuizScoreLast7Days = averageQuizScoreLast7Days; }
    public void setStrongestSubject(String strongestSubject) { this.strongestSubject = strongestSubject; }
    public void setFocusSubject(String focusSubject) { this.focusSubject = focusSubject; }
    public void setUnlockedBadgesCsv(String unlockedBadgesCsv) { this.unlockedBadgesCsv = unlockedBadgesCsv; }
    public void setLastUnlockedBadge(String lastUnlockedBadge) { this.lastUnlockedBadge = lastUnlockedBadge; }
    public void setLastBadgeUnlockedAt(long lastBadgeUnlockedAt) { this.lastBadgeUnlockedAt = lastBadgeUnlockedAt; }
    public void setLastProgressComputedAt(long lastProgressComputedAt) { this.lastProgressComputedAt = lastProgressComputedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }
    public void setTermsAcceptedAt(long termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }
    public void setTermsVersion(int termsVersion) { this.termsVersion = termsVersion; }
    public void setPersonalizationEnabled(boolean personalizationEnabled) { this.personalizationEnabled = personalizationEnabled; }

    public void setUsername(String username) { this.username = username; }
    public void setAge(int age) { this.age = age; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    public void setStrand(String strand) { this.strand = strand; }
    public void setGoal(String goal) { this.goal = goal; }
    public void setSubjectsCsv(String subjectsCsv) { this.subjectsCsv = subjectsCsv; }
    public void setTopicsOfInterestCsv(String topicsOfInterestCsv) { this.topicsOfInterestCsv = topicsOfInterestCsv; }
    public void setWeeklyStudyTargetMinutes(int weeklyStudyTargetMinutes) { this.weeklyStudyTargetMinutes = weeklyStudyTargetMinutes; }
}
