package com.example.studysync_project.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.studysync_project.data.model.Quiz;

import java.util.List;

/**
 * Data Access Object for Quiz entity
 */
@Dao
public interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuiz(Quiz quiz);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAllQuizzes(List<Quiz> quizzes);

    @Update
    void updateQuiz(Quiz quiz);

    @Delete
    void deleteQuiz(Quiz quiz);

    @Query("DELETE FROM quizzes WHERE quizId = :quizId")
    void deleteQuizById(String quizId);

    @Query("SELECT * FROM quizzes WHERE quizId = :quizId")
    LiveData<Quiz> getQuizById(String quizId);

    @Query("SELECT * FROM quizzes WHERE quizId = :quizId")
    Quiz getQuizByIdSync(String quizId);

    @Query("SELECT subject FROM quizzes WHERE quizId = :quizId LIMIT 1")
    String getQuizSubjectByIdSync(String quizId);

    @Query("SELECT * FROM quizzes WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    LiveData<List<Quiz>> getAllQuizzesForUser(String userId);

    @Query("SELECT * FROM quizzes WHERE subject = :subject AND isArchived = 0 ORDER BY createdAt DESC")
    LiveData<List<Quiz>> getQuizzesBySubject(String subject);

    @Query("SELECT * FROM quizzes WHERE difficulty = :difficulty AND isArchived = 0")
    LiveData<List<Quiz>> getQuizzesByDifficulty(int difficulty);

    @Query("SELECT * FROM quizzes WHERE isArchived = 0 ORDER BY createdAt DESC")
    LiveData<List<Quiz>> getAllActiveQuizzes();

    @Query("DELETE FROM quizzes")
    void clearAllQuizzes();

    @Query("SELECT COUNT(*) FROM quizzes WHERE userId = :userId AND isArchived = 0")
    LiveData<Integer> getActiveQuizCountForUser(String userId);
}
