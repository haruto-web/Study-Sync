package com.example.studysync_project.ui.quiz;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.QuizRepository;
import com.example.studysync_project.data.repository.StudyModuleRepository;

import java.util.List;

/**
 * ViewModel for Quiz feature
 * Manages quiz data and UI state
 */
public class QuizViewModel extends ViewModel {
    private final QuizRepository quizRepository;
    private final StudyModuleRepository studyModuleRepository;
    private LiveData<List<StudyModule>> allStudyModules;
    private LiveData<List<Quiz>> allQuizzes;
    private LiveData<List<Quiz>> quizzesBySubject;
    private LiveData<Integer> quizCount;

    public QuizViewModel(Context context) {
        this.quizRepository = new QuizRepository(context);
        this.studyModuleRepository = new StudyModuleRepository(context);
    }

    /**
     * Get all active study modules for user
     */
    public LiveData<List<StudyModule>> getAllStudyModulesForUser(String userId) {
        allStudyModules = studyModuleRepository.getAllStudyModulesForUser(userId);
        return allStudyModules;
    }

    /**
     * Get a single study module
     */
    public LiveData<StudyModule> getStudyModuleById(String moduleId) {
        return studyModuleRepository.getStudyModuleById(moduleId);
    }

    /**
     * Sync study modules from Firestore
     */
    public void syncStudyModules(String userId) {
        studyModuleRepository.syncStudyModulesFromFirestore(userId);
    }

    /**
     * Get all active quizzes for user
     */
    public LiveData<List<Quiz>> getAllQuizzesForUser(String userId) {
        allQuizzes = quizRepository.getAllQuizzesForUser(userId);
        return allQuizzes;
    }

    /**
     * Get quizzes by subject
     */
    public LiveData<List<Quiz>> getQuizzesBySubject(String subject) {
        quizzesBySubject = quizRepository.getQuizzesBySubject(subject);
        return quizzesBySubject;
    }

    /**
     * Get a single quiz
     */
    public LiveData<Quiz> getQuizById(String quizId) {
        return quizRepository.getQuizById(quizId);
    }

    /**
     * Create a new quiz
     */
    public void createQuiz(Quiz quiz, String userId) {
        quizRepository.createQuiz(quiz, userId);
    }

    /**
     * Update a quiz
     */
    public void updateQuiz(Quiz quiz) {
        quizRepository.updateQuiz(quiz);
    }

    /**
     * Delete a quiz
     */
    public void deleteQuiz(String quizId) {
        quizRepository.deleteQuiz(quizId);
    }

    /**
     * Sync quizzes from Firestore
     */
    public void syncQuizzes(String userId) {
        quizRepository.syncQuizzesFromFirestore(userId);
    }

    /**
     * Get count of active quizzes
     */
    public LiveData<Integer> getActiveQuizCount(String userId) {
        quizCount = quizRepository.getActiveQuizCountForUser(userId);
        return quizCount;
    }

    /**
     * Get all quizzes live data
     */
    public LiveData<List<Quiz>> getAllQuizzes() {
        return allQuizzes;
    }
}
