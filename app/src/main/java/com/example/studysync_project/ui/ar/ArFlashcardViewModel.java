package com.example.studysync_project.ui.ar;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.repository.QuestionRepository;
import com.example.studysync_project.data.repository.QuizRepository;

import java.util.List;

public class ArFlashcardViewModel extends ViewModel {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    public ArFlashcardViewModel(Context context) {
        this.quizRepository = new QuizRepository(context);
        this.questionRepository = new QuestionRepository(context);
    }

    public LiveData<List<Quiz>> getAllQuizzesForUser(String userId) {
        return quizRepository.getAllQuizzesForUser(userId);
    }

    public LiveData<List<Question>> getQuestionsForQuiz(String quizId) {
        return questionRepository.getQuestionsForQuiz(quizId);
    }

    public void syncQuizzes(String userId) {
        quizRepository.syncQuizzesFromFirestore(userId);
    }

    public void syncQuestions(String quizId) {
        questionRepository.syncQuestionsFromFirestore(quizId);
    }
}
