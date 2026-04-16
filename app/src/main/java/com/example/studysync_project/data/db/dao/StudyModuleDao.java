package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.StudyModule;

import java.util.List;

/**
 * Data Access Object for StudyModule entity.
 */
@Dao
public interface StudyModuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStudyModule(StudyModule studyModule);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllStudyModules(List<StudyModule> studyModules);

    @Update
    void updateStudyModule(StudyModule studyModule);

    @Query("DELETE FROM study_modules WHERE moduleId = :moduleId")
    void deleteStudyModuleById(String moduleId);

    @Query("SELECT * FROM study_modules WHERE moduleId = :moduleId")
    LiveData<StudyModule> getStudyModuleById(String moduleId);

    @Query("SELECT * FROM study_modules WHERE moduleId = :moduleId")
    StudyModule getStudyModuleByIdSync(String moduleId);

    @Query("SELECT * FROM study_modules WHERE userId = :userId AND isArchived = 0 ORDER BY isUnlocked DESC, LOWER(COALESCE(subject, 'general')) ASC, unlockOrder ASC, createdAt DESC")
    LiveData<List<StudyModule>> getAllStudyModulesForUser(String userId);

    @Query("SELECT * FROM study_modules WHERE userId = :userId AND isArchived = 0 ORDER BY LOWER(COALESCE(subject, 'general')) ASC, unlockOrder ASC, createdAt ASC")
    List<StudyModule> getAllStudyModulesForUserSync(String userId);

    @Query("SELECT MAX(unlockOrder) FROM study_modules WHERE userId = :userId AND isArchived = 0 AND LOWER(COALESCE(subject, 'general')) = LOWER(:subject)")
    Integer getMaxUnlockOrderForSubjectSync(String userId, String subject);

    @Query("SELECT * FROM study_modules WHERE userId = :userId AND isArchived = 0 AND LOWER(COALESCE(subject, 'general')) = LOWER(:subject) ORDER BY unlockOrder DESC, createdAt DESC LIMIT 1")
    StudyModule getLatestModuleForSubjectSync(String userId, String subject);

    @Query("SELECT * FROM study_modules WHERE userId = :userId AND isArchived = 0 AND isUnlocked = 0 AND LOWER(COALESCE(subject, 'general')) = LOWER(:subject) AND unlockOrder > :afterUnlockOrder ORDER BY unlockOrder ASC, createdAt ASC LIMIT 1")
    StudyModule getNextLockedModuleForSubjectSync(String userId, String subject, int afterUnlockOrder);

    @Query("SELECT COUNT(*) FROM study_modules WHERE userId = :userId AND isArchived = 0")
    LiveData<Integer> getActiveStudyModuleCountForUser(String userId);

    @Query("DELETE FROM study_modules")
    void clearAllStudyModules();
}
