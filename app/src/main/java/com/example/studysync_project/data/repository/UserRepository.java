package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.UserProfileDao;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserRepository {
    private final UserProfileDao userProfileDao;
    private final FirebaseFirestore firestore;
    private final ProgressionRepository progressionRepository;

    public UserRepository(Context context) {
        this.userProfileDao = AppDatabase.getInstance(context).userProfileDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.progressionRepository = new ProgressionRepository(context);
    }

    public LiveData<UserProfile> getUserProfile(String userId) {
        return userProfileDao.getUserProfile(userId);
    }

    public void saveUserProfile(UserProfile userProfile) {
        AppExecutors.diskIO().execute(() -> userProfileDao.insertUserProfile(userProfile));
        firestore.collection("users").document(userProfile.getUserId())
                .set(userProfile).addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateUserProfile(UserProfile userProfile) {
        userProfile.setUpdatedAt(System.currentTimeMillis());
        AppExecutors.diskIO().execute(() -> userProfileDao.updateUserProfile(userProfile));
        firestore.collection("users").document(userProfile.getUserId())
                .set(userProfile).addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateProfilePicture(String userId, String imageUrl) {
        AppExecutors.diskIO().execute(() -> {
            UserProfile profile = userProfileDao.getUserProfileDirect(userId);
            if (profile != null) {
                profile.setProfileImageUrl(imageUrl);
                userProfileDao.updateUserProfile(profile);
            }
        });
        firestore.collection("users").document(userId)
                .update("profileImageUrl", imageUrl);
    }

    public void updateNameAndBio(String userId, String email, String fullName, String bio) {
        AppExecutors.diskIO().execute(() -> {
            UserProfile profile = userProfileDao.getUserProfileDirect(userId);
            if (profile == null) {
                profile = new UserProfile(userId, email, fullName);
            } else {
                if (email != null && !email.trim().isEmpty()) {
                    profile.setEmail(email);
                }
                profile.setFullName(fullName);
            }
            profile.setBio(bio);
            profile.setUpdatedAt(System.currentTimeMillis());
            userProfileDao.insertUserProfile(profile);
            firestore.collection("users").document(userId)
                    .set(profile)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void updateProfileFields(String userId, String email, String fullName, String username,
                                    int age, String gradeLevel, String strand, String topicsOfInterestCsv,
                                    String profileImageUrl) {
        AppExecutors.diskIO().execute(() -> {
            UserProfile profile = userProfileDao.getUserProfileDirect(userId);
            if (profile == null) {
                profile = new UserProfile(userId, email, fullName);
            }
            if (email != null && !email.trim().isEmpty()) profile.setEmail(email);
            if (fullName != null && !fullName.trim().isEmpty()) profile.setFullName(fullName);
            profile.setUsername(username);
            profile.setAge(age);
            profile.setGradeLevel(gradeLevel);
            profile.setStrand(strand);
            profile.setTopicsOfInterestCsv(topicsOfInterestCsv);
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                profile.setProfileImageUrl(profileImageUrl);
            }
            profile.setUpdatedAt(System.currentTimeMillis());
            userProfileDao.insertUserProfile(profile);
            firestore.collection("users").document(userId)
                    .set(profile)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void syncUserProfileFromFirestore(String userId) {
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    UserProfile profile = doc.toObject(UserProfile.class);
                    if (profile != null) {
                        AppExecutors.diskIO().execute(() -> {
                            userProfileDao.insertUserProfile(profile);
                            progressionRepository.recomputeAndPersistForSync(userId);
                        });
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void deleteUserProfile(String userId) {
        AppExecutors.diskIO().execute(() -> userProfileDao.deleteUserById(userId));
        firestore.collection("users").document(userId).delete();
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(userProfileDao::clearAllUsers);
    }
}
