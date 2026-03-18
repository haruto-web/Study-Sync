package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.model.Quiz;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

/**
 * Repository for Quiz data
 * Handles communication between Firestore, Room database, and UI
 */
public class QuizRepository {
    private final QuizDao quizDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public QuizRepository(Context context) {
        this.context = context;
        this.quizDao = AppDatabase.getInstance(context).quizDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get all active quizzes for current user from Room (local cache)
     */
    public LiveData<List<Quiz>> getAllQuizzesForUser(String userId) {
        return quizDao.getAllQuizzesForUser(userId);
    }

    /**
     * Get all quizzes by subject
     */
    public LiveData<List<Quiz>> getQuizzesBySubject(String subject) {
        return quizDao.getQuizzesBySubject(subject);
    }

    /**
     * Get a single quiz by ID
     */
    public LiveData<Quiz> getQuizById(String quizId) {
        return quizDao.getQuizById(quizId);
    }

    /**
     * Create a new quiz
     * Saves to Firestore first, then syncs to Room
     */
    public void createQuiz(Quiz quiz, String userId) {
        quiz.setUserId(userId);
        
        // Save to Firestore
        firestore.collection("quizzes")
            .document(quiz.getQuizId())
            .set(quiz)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                quizDao.insertQuiz(quiz);
            })
            .addOnFailureListener(e -> {
                // Log error - in production, should notify user
                e.printStackTrace();
            });
    }

    /**
     * Update an existing quiz
     */
    public void updateQuiz(Quiz quiz) {
        quiz.setUpdatedAt(System.currentTimeMillis());
        
        // Update in Firestore
        firestore.collection("quizzes")
            .document(quiz.getQuizId())
            .set(quiz)
            .addOnSuccessListener(aVoid -> {
                // Update in Room
                quizDao.updateQuiz(quiz);
            });
    }

    /**
     * Delete a quiz (soft delete - mark as archived)
     */
    public void deleteQuiz(String quizId) {
        // Get quiz, mark as archived, and update
        LiveData<Quiz> quizLiveData = quizDao.getQuizById(quizId);
        Quiz quiz = quizLiveData.getValue();
        
        if (quiz != null) {
            quiz.setArchived(true);
            updateQuiz(quiz);
        }
    }

    /**
     * Sync quizzes from Firestore to Room database
     * Called periodically to keep local database up to date
     */
    public void syncQuizzesFromFirestore(String userId) {
        firestore.collection("quizzes")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isArchived", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Quiz> quizzes = queryDocumentSnapshots.toObjects(Quiz.class);
                quizDao.insertAllQuizzes(quizzes);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Get count of active quizzes for user
     */
    public LiveData<Integer> getActiveQuizCountForUser(String userId) {
        return quizDao.getActiveQuizCountForUser(userId);
    }

    /**
     * Clear all local quiz data
     */
    public void clearLocalData() {
        quizDao.clearAllQuizzes();
    }
}
