package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.Question;

import java.util.List;

/**
 * Data Access Object for Question entity
 */
@Dao
public interface QuestionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestion(Question question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllQuestions(List<Question> questions);

    @Update
    void updateQuestion(Question question);

    @Delete
    void deleteQuestion(Question question);

    @Query("DELETE FROM questions WHERE questionId = :questionId")
    void deleteQuestionById(String questionId);

    @Query("SELECT * FROM questions WHERE questionId = :questionId")
    LiveData<Question> getQuestionById(String questionId);

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY questionNumber ASC")
    LiveData<List<Question>> getQuestionsForQuiz(String quizId);

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY questionNumber ASC")
    List<Question> getQuestionsForQuizSync(String quizId);

    @Query("SELECT COUNT(*) FROM questions WHERE quizId = :quizId")
    LiveData<Integer> getQuestionCountForQuiz(String quizId);

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    void deleteQuestionsForQuiz(String quizId);

    @Query("DELETE FROM questions")
    void clearAllQuestions();
}
