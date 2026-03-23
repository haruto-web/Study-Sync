package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuizRepository {
    private final QuizDao quizDao;
    private final FirebaseFirestore firestore;

    public QuizRepository(Context context) {
        this.quizDao = AppDatabase.getInstance(context).quizDao();
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

    public void createQuiz(Quiz quiz, String userId) {
        quiz.setUserId(userId);
        AppExecutors.diskIO().execute(() -> quizDao.insertQuiz(quiz));
        firestore.collection("quizzes").document(quiz.getQuizId()).set(quiz)
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void updateQuiz(Quiz quiz) {
        quiz.setUpdatedAt(System.currentTimeMillis());
        AppExecutors.diskIO().execute(() -> quizDao.updateQuiz(quiz));
        firestore.collection("quizzes").document(quiz.getQuizId()).set(quiz)
                .addOnFailureListener(Throwable::printStackTrace);
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
                    AppExecutors.diskIO().execute(() -> quizDao.insertAllQuizzes(quizzes));
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public LiveData<Integer> getActiveQuizCountForUser(String userId) {
        return quizDao.getActiveQuizCountForUser(userId);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(quizDao::clearAllQuizzes);
    }
}
