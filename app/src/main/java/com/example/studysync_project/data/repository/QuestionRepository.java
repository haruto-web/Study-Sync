package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuestionDao;
import com.example.studysync_project.data.model.Question;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Repository for Question data
 * Handles communication between Firestore, Room database, and UI
 */
public class QuestionRepository {
    private final QuestionDao questionDao;
    private final FirebaseFirestore firestore;
    private final Context context;

    public QuestionRepository(Context context) {
        this.context = context;
        this.questionDao = AppDatabase.getInstance(context).questionDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Get all questions for a specific quiz
     */
    public LiveData<List<Question>> getQuestionsForQuiz(String quizId) {
        return questionDao.getQuestionsForQuiz(quizId);
    }

    /**
     * Get questions synchronously
     */
    public List<Question> getQuestionsForQuizSync(String quizId) {
        return questionDao.getQuestionsForQuizSync(quizId);
    }

    /**
     * Get a single question
     */
    public LiveData<Question> getQuestionById(String questionId) {
        return questionDao.getQuestionById(questionId);
    }

    /**
     * Create a new question
     */
    public void createQuestion(Question question) {
        // Save to Firestore
        firestore.collection("quizzes")
            .document(question.getQuizId())
            .collection("questions")
            .document(question.getQuestionId())
            .set(question)
            .addOnSuccessListener(aVoid -> {
                // Save to local Room database
                questionDao.insertQuestion(question);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Create multiple questions
     */
    public void createAllQuestions(List<Question> questions) {
        questionDao.insertAllQuestions(questions);
        
        // Batch save to Firestore (if needed)
        for (Question question : questions) {
            firestore.collection("quizzes")
                .document(question.getQuizId())
                .collection("questions")
                .document(question.getQuestionId())
                .set(question);
        }
    }

    /**
     * Update a question
     */
    public void updateQuestion(Question question) {
        firestore.collection("quizzes")
            .document(question.getQuizId())
            .collection("questions")
            .document(question.getQuestionId())
            .set(question)
            .addOnSuccessListener(aVoid -> {
                questionDao.updateQuestion(question);
            });
    }

    /**
     * Delete a question
     */
    public void deleteQuestion(String quizId, String questionId) {
        firestore.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .document(questionId)
            .delete();
        
        questionDao.deleteQuestionById(questionId);
    }

    /**
     * Delete all questions for a quiz
     */
    public void deleteAllQuestionsForQuiz(String quizId) {
        questionDao.deleteQuestionsForQuiz(quizId);
    }

    /**
     * Get question count for quiz
     */
    public LiveData<Integer> getQuestionCountForQuiz(String quizId) {
        return questionDao.getQuestionCountForQuiz(quizId);
    }

    /**
     * Sync questions from Firestore to Room
     */
    public void syncQuestionsFromFirestore(String quizId) {
        firestore.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Question> questions = queryDocumentSnapshots.toObjects(Question.class);
                questionDao.insertAllQuestions(questions);
            })
            .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Clear all local question data
     */
    public void clearLocalData() {
        questionDao.clearAllQuestions();
    }
}
