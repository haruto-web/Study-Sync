package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.StudyModuleDao;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for saved study modules.
 */
public class StudyModuleRepository {
    private final StudyModuleDao studyModuleDao;
    private final FirebaseFirestore firestore;

    public StudyModuleRepository(Context context) {
        this.studyModuleDao = AppDatabase.getInstance(context).studyModuleDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<StudyModule>> getAllStudyModulesForUser(String userId) {
        return studyModuleDao.getAllStudyModulesForUser(userId);
    }

    public LiveData<StudyModule> getStudyModuleById(String moduleId) {
        return studyModuleDao.getStudyModuleById(moduleId);
    }

    public StudyModule getStudyModuleByIdSync(String moduleId) {
        return studyModuleDao.getStudyModuleByIdSync(moduleId);
    }

    public void upsertStudyModule(StudyModule studyModule, String userId) {
        studyModule.setUserId(userId);
        AppExecutors.diskIO().execute(() -> studyModuleDao.insertStudyModule(studyModule));
        firestore.collection("modules").document(studyModule.getModuleId()).set(studyModule)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateStudyModule(StudyModule studyModule) {
        studyModule.setUpdatedAt(System.currentTimeMillis());
        AppExecutors.diskIO().execute(() -> studyModuleDao.updateStudyModule(studyModule));
        firestore.collection("modules").document(studyModule.getModuleId()).set(studyModule)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void archiveStudyModule(String moduleId) {
        AppExecutors.diskIO().execute(() -> {
            StudyModule module = studyModuleDao.getStudyModuleByIdSync(moduleId);
            if (module != null) {
                module.setArchived(true);
                module.setUpdatedAt(System.currentTimeMillis());
                studyModuleDao.updateStudyModule(module);
                firestore.collection("modules").document(moduleId).update("isArchived", true);
            }
        });
    }

    public void syncStudyModulesFromFirestore(String userId) {
        firestore.collection("modules")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<StudyModule> modules = snap.toObjects(StudyModule.class);
                    AppExecutors.diskIO().execute(() -> studyModuleDao.insertAllStudyModules(modules));
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<Integer> getActiveStudyModuleCountForUser(String userId) {
        return studyModuleDao.getActiveStudyModuleCountForUser(userId);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(studyModuleDao::clearAllStudyModules);
    }
}
