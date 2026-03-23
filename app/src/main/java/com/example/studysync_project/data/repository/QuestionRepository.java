package com.example.studysync_project.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.QuestionDao;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.utils.AppExecutors;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class QuestionRepository {
    private final QuestionDao questionDao;
    private final FirebaseFirestore firestore;

    public QuestionRepository(Context context) {
        this.questionDao = AppDatabase.getInstance(context).questionDao();
        this.firestore = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Question>> getQuestionsForQuiz(String quizId) {
        return questionDao.getQuestionsForQuiz(quizId);
    }

    public List<Question> getQuestionsForQuizSync(String quizId) {
        return questionDao.getQuestionsForQuizSync(quizId);
    }

    public LiveData<Question> getQuestionById(String questionId) {
        return questionDao.getQuestionById(questionId);
    }

    public void createQuestion(Question question) {
        AppExecutors.diskIO().execute(() -> questionDao.insertQuestion(question));
        firestore.collection("quizzes").document(question.getQuizId())
                .collection("questions").document(question.getQuestionId())
                .set(question).addOnFailureListener(Throwable::printStackTrace);
    }

    public void createAllQuestions(List<Question> questions) {
        AppExecutors.diskIO().execute(() -> questionDao.insertAllQuestions(questions));
        for (Question q : questions) {
            firestore.collection("quizzes").document(q.getQuizId())
                    .collection("questions").document(q.getQuestionId()).set(q);
        }
    }

    public void updateQuestion(Question question) {
        AppExecutors.diskIO().execute(() -> questionDao.updateQuestion(question));
        firestore.collection("quizzes").document(question.getQuizId())
                .collection("questions").document(question.getQuestionId())
                .set(question).addOnFailureListener(Throwable::printStackTrace);
    }

    public void deleteQuestion(String quizId, String questionId) {
        AppExecutors.diskIO().execute(() -> questionDao.deleteQuestionById(questionId));
        firestore.collection("quizzes").document(quizId)
                .collection("questions").document(questionId).delete();
    }

    public void deleteAllQuestionsForQuiz(String quizId) {
        AppExecutors.diskIO().execute(() -> questionDao.deleteQuestionsForQuiz(quizId));
    }

    public LiveData<Integer> getQuestionCountForQuiz(String quizId) {
        return questionDao.getQuestionCountForQuiz(quizId);
    }

    public void syncQuestionsFromFirestore(String quizId) {
        firestore.collection("quizzes").document(quizId).collection("questions").get()
                .addOnSuccessListener(snap -> {
                    List<Question> questions = snap.toObjects(Question.class);
                    AppExecutors.diskIO().execute(() -> questionDao.insertAllQuestions(questions));
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    public void clearLocalData() {
        AppExecutors.diskIO().execute(questionDao::clearAllQuestions);
    }
}
