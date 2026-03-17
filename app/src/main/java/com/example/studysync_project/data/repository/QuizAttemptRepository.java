package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.model.QuizAttempt;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for QuizAttempt data
 * Handles communication between Firestore, Room database, and UI
 */
public class QuizAttemptRepository {
    private final QuizAttemptDao quizAttemptDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public QuizAttemptRepository(Context context) {
        this.context = context;
        this.quizAttemptDao = AppDatabase.getInstance(context).quizAttemptDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get all quiz attempts for user
     */
    public LiveData<List<QuizAttempt>> getAllQuizAttemptsForUser(String userId) {
        return quizAttemptDao.getAllQuizAttemptsForUser(userId);
    }

    /**
     * Get attempts for a specific quiz
     */
    public LiveData<List<QuizAttempt>> getAttemptsForQuiz(String userId, String quizId) {
        return quizAttemptDao.getAttemptsForQuiz(userId, quizId);
    }

    /**
     * Get a single attempt
     */
    public LiveData<QuizAttempt> getQuizAttemptById(String attemptId) {
        return quizAttemptDao.getQuizAttemptById(attemptId);
    }

    /**
     * Get last attempt for a quiz
     */
    public LiveData<QuizAttempt> getLastAttemptForQuiz(String userId, String quizId) {
        return quizAttemptDao.getLastAttemptForQuiz(userId, quizId);
    }

    /**
     * Get average score for user
     */
    public LiveData<Double> getAverageScoreForUser(String userId) {
        return quizAttemptDao.getAverageScoreForUser(userId);
    }

    /**
     * Get total quiz attempts count
     */
    public LiveData<Integer> getTotalQuizAttemptsForUser(String userId) {
        return quizAttemptDao.getTotalQuizAttemptsForUser(userId);
    }

    /**
     * Get passed quizzes count
     */
    public LiveData<Integer> getPassedQuizzesCountForUser(String userId) {
        return quizAttemptDao.getPassedQuizzesCountForUser(userId);
    }

    /**
     * Save a new quiz attempt
     */
    public void saveQuizAttempt(QuizAttempt attempt, String userId) {
        attempt.setUserId(userId);
        
        // Save to Firestore
        firestore.collection("users")
            .document(userId)
            .collection("quizAttempts")
            .document(attempt.getAttemptId())
            .set(attempt)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                quizAttemptDao.insertQuizAttempt(attempt);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Update a quiz attempt
     */
    public void updateQuizAttempt(QuizAttempt attempt) {
        firestore.collection("users")
            .document(attempt.getUserId())
            .collection("quizAttempts")
            .document(attempt.getAttemptId())
            .set(attempt)
            .addOnSuccessListener(aVoid -> {
                quizAttemptDao.updateQuizAttempt(attempt);
            });
    }

    /**
     * Delete a quiz attempt
     */
    public void deleteQuizAttempt(String userId, String attemptId) {
        firestore.collection("users")
            .document(userId)
            .collection("quizAttempts")
            .document(attemptId)
            .delete();
        
        quizAttemptDao.deleteQuizAttemptById(attemptId);
    }

    /**
     * Sync quiz attempts from Firestore to Room
     */
    public void syncQuizAttemptsFromFirestore(String userId) {
        firestore.collection("users")
            .document(userId)
            .collection("quizAttempts")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<QuizAttempt> attempts = queryDocumentSnapshots.toObjects(QuizAttempt.class);
                quizAttemptDao.insertAllQuizAttempts(attempts);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Clear all local quiz attempt data
     */
    public void clearLocalData() {
        quizAttemptDao.clearAllQuizAttempts();
    }
}
