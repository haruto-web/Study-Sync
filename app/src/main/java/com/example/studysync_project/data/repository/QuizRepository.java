package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.StudyModuleDao;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuizRepository {
    private final QuizDao quizDao;
    private final StudyModuleDao studyModuleDao;
    private final FirebaseFirestore firestore;

    public QuizRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.quizDao = database.quizDao();
        this.studyModuleDao = database.studyModuleDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Quiz>> getAllQuizzesForUser(String userId) {
        return quizDao.getAllQuizzesForUser(userId);
    }

    public LiveData<List<Quiz>> getQuizzesBySubject(String subject) {
        return quizDao.getQuizzesBySubject(subject);
    }

    public LiveData<Quiz> getQuizById(String quizId) {
        return quizDao.getQuizById(quizId);
    }

    public Quiz getQuizByIdSync(String quizId) {
        return quizDao.getQuizByIdSync(quizId);
    }

    public void createQuiz(Quiz quiz, String userId) {
        quiz.setUserId(userId);
        AppExecutors.diskIO().execute(() -> {
            initializeQuizForInsert(quiz);
            quizDao.insertQuiz(quiz);
            firestore.collection("quizzes")
                    .document(quiz.getQuizId())
                    .set(quiz)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void updateQuiz(Quiz quiz) {
        AppExecutors.diskIO().execute(() -> {
            normalizeQuizDefaults(quiz);
            quiz.setUpdatedAt(System.currentTimeMillis());
            quizDao.updateQuiz(quiz);
            firestore.collection("quizzes")
                    .document(quiz.getQuizId())
                    .set(quiz)
                    .addOnFailureListener(Throwable::printStackTrace);
        });
    }

    public void deleteQuiz(String quizId) {
        AppExecutors.diskIO().execute(() -> {
            Quiz quiz = quizDao.getQuizByIdSync(quizId);
            if (quiz != null) {
                quiz.setArchived(true);
                quizDao.updateQuiz(quiz);
                firestore.collection("quizzes").document(quizId)
                        .update("isArchived", true);
            }
        });
    }

    public void syncQuizzesFromFirestore(String userId) {
        firestore.collection("quizzes")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Quiz> quizzes = snap.toObjects(Quiz.class);
                    AppExecutors.diskIO().execute(() -> {
                        for (Quiz quiz : quizzes) {
                            normalizeQuizDefaults(quiz);
                        }
                        quizDao.insertAllQuizzes(quizzes);
                    });
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<Integer> getActiveQuizCountForUser(String userId) {
        return quizDao.getActiveQuizCountForUser(userId);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(quizDao::clearAllQuizzes);
    }

    private void initializeQuizForInsert(Quiz quiz) {
        normalizeQuizDefaults(quiz);

        String linkedModuleId = safeText(quiz.getModuleId());
        if (linkedModuleId.isEmpty()) {
            quiz.setUnlocked(true);
            return;
        }

        StudyModule module = studyModuleDao.getStudyModuleByIdSync(linkedModuleId);
        if (module == null) {
            quiz.setUnlocked(true);
            return;
        }

        boolean moduleReadyForQuiz = module.isUnlocked()
                && !StudyModule.PROGRESSION_NEW.equalsIgnoreCase(safeText(module.getProgressionState()));
        quiz.setUnlocked(moduleReadyForQuiz);
    }

    private void normalizeQuizDefaults(Quiz quiz) {
        if (quiz == null) {
            return;
        }

        if (quiz.getAttemptCount() < 0) {
            quiz.setAttemptCount(0);
        }
        if (quiz.getLastScore() < 0.0) {
            quiz.setLastScore(0.0);
        }
        if (quiz.getBestScore() < 0.0) {
            quiz.setBestScore(0.0);
        }
        if (quiz.getBestScore() < quiz.getLastScore()) {
            quiz.setBestScore(quiz.getLastScore());
        }
        if (quiz.getMasteredAt() < 0L) {
            quiz.setMasteredAt(0L);
        }
    }

    private static String safeText(String value) {
        return value != null ? value.trim() : "";
    }
}
