package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuizAttemptRepository {
    private final QuizAttemptDao quizAttemptDao;
    private final FirebaseFirestore firestore;
    private final ProgressionRepository progressionRepository;

    public QuizAttemptRepository(Context context) {
        this.quizAttemptDao = AppDatabase.getInstance(context).quizAttemptDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.progressionRepository = new ProgressionRepository(context);
    }

    public LiveData<List<QuizAttempt>> getAllQuizAttemptsForUser(String userId) {
        return quizAttemptDao.getAllQuizAttemptsForUser(userId);
    }

    public LiveData<List<QuizAttempt>> getAttemptsForQuiz(String userId, String quizId) {
        return quizAttemptDao.getAttemptsForQuiz(userId, quizId);
    }

    public LiveData<QuizAttempt> getQuizAttemptById(String attemptId) {
        return quizAttemptDao.getQuizAttemptById(attemptId);
    }

    public LiveData<QuizAttempt> getLastAttemptForQuiz(String userId, String quizId) {
        return quizAttemptDao.getLastAttemptForQuiz(userId, quizId);
    }

    public LiveData<Double> getAverageScoreForUser(String userId) {
        return quizAttemptDao.getAverageScoreForUser(userId);
    }

    public LiveData<Integer> getTotalQuizAttemptsForUser(String userId) {
        return quizAttemptDao.getTotalQuizAttemptsForUser(userId);
    }

    public LiveData<Integer> getPassedQuizzesCountForUser(String userId) {
        return quizAttemptDao.getPassedQuizzesCountForUser(userId);
    }

    public void saveQuizAttempt(QuizAttempt attempt, String userId) {
        attempt.setUserId(userId);
        AppExecutors.diskIO().execute(() -> quizAttemptDao.insertQuizAttempt(attempt));
        firestore.collection("users").document(userId)
                .collection("quizAttempts").document(attempt.getAttemptId())
                .set(attempt).addOnFailureListener(Throwable::printStackTrace);
        progressionRepository.recomputeProgressionAsync(userId);
    }

    public void deleteQuizAttempt(String userId, String attemptId) {
        AppExecutors.diskIO().execute(() -> quizAttemptDao.deleteQuizAttemptById(attemptId));
        firestore.collection("users").document(userId)
                .collection("quizAttempts").document(attemptId).delete();
        progressionRepository.recomputeProgressionAsync(userId);
    }

    public void syncQuizAttemptsFromFirestore(String userId) {
        firestore.collection("users").document(userId).collection("quizAttempts").get()
                .addOnSuccessListener(snap -> {
                    List<QuizAttempt> attempts = snap.toObjects(QuizAttempt.class);
                    AppExecutors.diskIO().execute(() -> {
                        quizAttemptDao.insertAllQuizAttempts(attempts);
                        progressionRepository.recomputeAndPersistForSync(userId);
                    });
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(quizAttemptDao::clearAllQuizAttempts);
    }
}
