package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.UserProfileDao;
import com.example.studysync_project.data.model.UserProfile;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Repository for UserProfile data
 * Handles communication between Firestore, Room database, and UI
 */
public class UserRepository {
    private final UserProfileDao userProfileDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public UserRepository(Context context) {
        this.context = context;
        this.userProfileDao = AppDatabase.getInstance(context).userProfileDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get user profile
     */
    public LiveData<UserProfile> getUserProfile(String userId) {
        return userProfileDao.getUserProfile(userId);
    }

    /**
     * Get user profile synchronously (for non-UI threads)
     */
    public UserProfile getUserProfileDirect(String userId) {
        return userProfileDao.getUserProfileDirect(userId);
    }

    /**
     * Create or update user profile
     */
    public void saveUserProfile(UserProfile userProfile) {
        // Save to Firestore
        firestore.collection("users")
            .document(userProfile.getUserId())
            .set(userProfile)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                userProfileDao.insertUserProfile(userProfile);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Update user profile
     */
    public void updateUserProfile(UserProfile userProfile) {
        userProfile.setUpdatedAt(System.currentTimeMillis());
        
        // Update in Firestore
        firestore.collection("users")
            .document(userProfile.getUserId())
            .set(userProfile)
            .addOnSuccessListener(aVoid -> {
                // Update in Room
                userProfileDao.updateUserProfile(userProfile);
            });
    }

    /**
     * Update user profile picture URL
     */
    public void updateProfilePicture(String userId, String imageUrl) {
        firestore.collection("users")
            .document(userId)
            .update("profileImageUrl", imageUrl)
            .addOnSuccessListener(aVoid -> {
                // Update local database
                UserProfile userProfile = userProfileDao.getUserProfileDirect(userId);
                if (userProfile != null) {
                    userProfile.setProfileImageUrl(imageUrl);
                    userProfileDao.updateUserProfile(userProfile);
                }
            });
    }

    /**
     * Update user statistics
     */
    public void updateUserStats(String userId, int quizzesTaken, int tasksCompleted, 
                               int studyMinutes, double avgScore) {
        UserProfile userProfile = userProfileDao.getUserProfileDirect(userId);
        
        if (userProfile != null) {
            userProfile.setTotalQuizzesTaken(quizzesTaken);
            userProfile.setTotalTasksCompleted(tasksCompleted);
            userProfile.setTotalStudyMinutes(studyMinutes);
            userProfile.setAverageQuizScore(avgScore);
            updateUserProfile(userProfile);
        }
    }

    /**
     * Update last login time
     */
    public void updateLastLogin(String userId) {
        firestore.collection("users")
            .document(userId)
            .update("lastLogin", System.currentTimeMillis())
            .addOnSuccessListener(aVoid -> {
                UserProfile userProfile = userProfileDao.getUserProfileDirect(userId);
                if (userProfile != null) {
                    userProfile.setLastLogin(System.currentTimeMillis());
                    userProfileDao.updateUserProfile(userProfile);
                }
            });
    }

    /**
     * Sync user profile from Firestore to Room
     */
    public void syncUserProfileFromFirestore(String userId) {
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                if (userProfile != null) {
                    userProfileDao.insertUserProfile(userProfile);
                }
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Delete user profile
     */
    public void deleteUserProfile(String userId) {
        firestore.collection("users")
            .document(userId)
            .delete();
        
        userProfileDao.deleteUserById(userId);
    }

    /**
     * Clear all local user data
     */
    public void clearLocalData() {
        userProfileDao.clearAllUsers();
    }
}
