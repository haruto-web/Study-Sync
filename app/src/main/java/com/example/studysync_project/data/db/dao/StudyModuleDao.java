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

    @Query("SELECT * FROM study_modules WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    LiveData<List<StudyModule>> getAllStudyModulesForUser(String userId);

    @Query("SELECT COUNT(*) FROM study_modules WHERE userId = :userId AND isArchived = 0")
    LiveData<Integer> getActiveStudyModuleCountForUser(String userId);

    @Query("DELETE FROM study_modules")
    void clearAllStudyModules();
}
