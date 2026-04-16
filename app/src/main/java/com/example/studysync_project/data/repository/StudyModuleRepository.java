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
    private static final String DEFAULT_SUBJECT = "General";

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
        if (studyModule == null) {
            return;
        }

        studyModule.setUserId(userId);
        AppExecutors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            normalizeModuleTextFields(studyModule);

            StudyModule existing = studyModuleDao.getStudyModuleByIdSync(studyModule.getModuleId());
            if (existing == null) {
                initializeProgressionForNewModule(studyModule, userId);
            } else {
                preserveProgressionFields(studyModule, existing);
                if (studyModule.getCreatedAt() <= 0L) {
                    studyModule.setCreatedAt(existing.getCreatedAt());
                }
            }

            if (studyModule.getCreatedAt() <= 0L) {
                studyModule.setCreatedAt(now);
            }
            studyModule.setUpdatedAt(now);

            studyModuleDao.insertStudyModule(studyModule);
            firestore.collection("modules")
                    .document(studyModule.getModuleId())
                    .set(studyModule)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void updateStudyModule(StudyModule studyModule) {
        if (studyModule == null) {
            return;
        }

        AppExecutors.diskIO().execute(() -> {
            normalizeModuleDefaults(studyModule);
            studyModule.setUpdatedAt(System.currentTimeMillis());
            studyModuleDao.updateStudyModule(studyModule);
            firestore.collection("modules")
                    .document(studyModule.getModuleId())
                    .set(studyModule)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
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

    public void deleteStudyModule(String moduleId) {
        AppExecutors.diskIO().execute(() -> studyModuleDao.deleteStudyModuleById(moduleId));
        firestore.collection("modules").document(moduleId).delete()
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void markModuleAsStarted(String moduleId) {
        AppExecutors.diskIO().execute(() -> {
            StudyModule module = studyModuleDao.getStudyModuleByIdSync(moduleId);
            if (module == null || !module.isUnlocked()) {
                return;
            }

            boolean changed = false;
            String state = safeText(module.getProgressionState());
            long now = System.currentTimeMillis();

            if (!StudyModule.PROGRESSION_MASTERED.equalsIgnoreCase(state)
                    && !StudyModule.PROGRESSION_IN_PROGRESS.equalsIgnoreCase(state)) {
                module.setProgressionState(StudyModule.PROGRESSION_IN_PROGRESS);
                changed = true;
            }

            if (module.getStartedAt() <= 0L) {
                module.setStartedAt(now);
                changed = true;
            }

            if (!changed) {
                return;
            }

            module.setUpdatedAt(now);
            studyModuleDao.updateStudyModule(module);
            firestore.collection("modules")
                    .document(module.getModuleId())
                    .set(module)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void syncStudyModulesFromFirestore(String userId) {
        firestore.collection("modules")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<StudyModule> modules = snap.toObjects(StudyModule.class);
                    AppExecutors.diskIO().execute(() -> {
                        for (StudyModule module : modules) {
                            normalizeModuleDefaults(module);
                        }
                        studyModuleDao.insertAllStudyModules(modules);
                    });
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<Integer> getActiveStudyModuleCountForUser(String userId) {
        return studyModuleDao.getActiveStudyModuleCountForUser(userId);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(studyModuleDao::clearAllStudyModules);
    }

    private void initializeProgressionForNewModule(StudyModule module, String userId) {
        normalizeModuleDefaults(module);

        String subjectKey = normalizeSubjectKey(module.getSubject());
        Integer maxOrder = studyModuleDao.getMaxUnlockOrderForSubjectSync(userId, subjectKey);
        int nextUnlockOrder = maxOrder == null ? 1 : maxOrder + 1;

        StudyModule latestInSubject = studyModuleDao.getLatestModuleForSubjectSync(userId, subjectKey);
        boolean unlockNow = latestInSubject == null
                || StudyModule.PROGRESSION_MASTERED.equalsIgnoreCase(safeText(latestInSubject.getProgressionState()));

        module.setUnlockOrder(nextUnlockOrder);
        module.setUnlocked(unlockNow);
        module.setProgressionState(StudyModule.PROGRESSION_NEW);
        module.setStartedAt(0L);
        module.setCompletedAt(0L);
        module.setMasteryScore(0.0);
        module.setMasteryAttempts(0);
    }

    private void preserveProgressionFields(StudyModule incoming, StudyModule existing) {
        incoming.setProgressionState(existing.getProgressionState());
        incoming.setUnlockOrder(existing.getUnlockOrder());
        incoming.setUnlocked(existing.isUnlocked());
        incoming.setStartedAt(existing.getStartedAt());
        incoming.setCompletedAt(existing.getCompletedAt());
        incoming.setMasteryScore(existing.getMasteryScore());
        incoming.setMasteryAttempts(existing.getMasteryAttempts());
    }

    private void normalizeModuleTextFields(StudyModule module) {
        String normalizedSubject = normalizeSubjectKey(module.getSubject());
        module.setSubject(normalizedSubject);

        if (safeText(module.getTopic()).isEmpty()) {
            module.setTopic(normalizedSubject);
        }
    }

    private void normalizeModuleDefaults(StudyModule module) {
        if (module == null) {
            return;
        }

        normalizeModuleTextFields(module);

        if (safeText(module.getProgressionState()).isEmpty()) {
            module.setProgressionState(StudyModule.PROGRESSION_NEW);
        }
        if (module.getUnlockOrder() < 0) {
            module.setUnlockOrder(0);
        }
        if (!module.isUnlocked() && module.getUnlockOrder() == 0
                && module.getStartedAt() <= 0L
                && module.getCompletedAt() <= 0L
                && module.getMasteryAttempts() == 0) {
            module.setUnlocked(true);
        }
        if (module.getMasteryScore() < 0.0) {
            module.setMasteryScore(0.0);
        }
        if (module.getMasteryAttempts() < 0) {
            module.setMasteryAttempts(0);
        }
    }

    private String normalizeSubjectKey(String subject) {
        String clean = safeText(subject);
        return clean.isEmpty() ? DEFAULT_SUBJECT : clean;
    }

    private static String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
