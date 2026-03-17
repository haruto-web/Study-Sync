package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.UserProfile;

/**
 * Data Access Object for UserProfile entity
 */
@Dao
public interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserProfile(UserProfile userProfile);

    @Update
    void updateUserProfile(UserProfile userProfile);

    @Delete
    void deleteUserProfile(UserProfile userProfile);

    @Query("SELECT * FROM users WHERE userId = :userId")
    LiveData<UserProfile> getUserProfile(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    UserProfile getUserProfileSync(String userId);

    @Query("SELECT * FROM users WHERE userId = :userId")
    UserProfile getUserProfileDirect(String userId);

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUserById(String userId);

    @Query("DELETE FROM users")
    void clearAllUsers();

    @Query("SELECT COUNT(*) FROM users")
    LiveData<Integer> getUserCount();
}
