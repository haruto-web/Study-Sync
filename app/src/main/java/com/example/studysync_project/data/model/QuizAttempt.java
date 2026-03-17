package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a user's attempt at taking a quiz
 */
@Entity(tableName = "quiz_attempts")
public class QuizAttempt {
    @PrimaryKey
    @NonNull
    public String attemptId;

    public String userId;
    public String quizId;

    @PropertyName("score_percentage")
    public double scorePercentage;

    public int questionsAttempted;
    public int correctAnswers;

    @PropertyName("time_taken_minutes")
    public int timeTakenMinutes;

    @PropertyName("attempted_at")
    public long attemptedAt;

    public boolean passed;
    public String answers;  // JSON string of answers: {"q1": "A", "q2": "B"}

    // Constructors
    public QuizAttempt() {
    }

    public QuizAttempt(String userId, String quizId, int questionsAttempted,
                       int correctAnswers, double scorePercentage, int timeTakenMinutes) {
        this.userId = userId;
        this.quizId = quizId;
        this.questionsAttempted = questionsAttempted;
        this.correctAnswers = correctAnswers;
        this.scorePercentage = scorePercentage;
        this.timeTakenMinutes = timeTakenMinutes;
        this.attemptedAt = System.currentTimeMillis();
    }

    // Getters
    public String getAttemptId() { return attemptId; }
    public String getUserId() { return userId; }
    public String getQuizId() { return quizId; }
    public double getScorePercentage() { return scorePercentage; }
    public int getQuestionsAttempted() { return questionsAttempted; }
    public int getCorrectAnswers() { return correctAnswers; }
    public int getTimeTakenMinutes() { return timeTakenMinutes; }
    public long getAttemptedAt() { return attemptedAt; }
    public boolean isPassed() { return passed; }
    public String getAnswers() { return answers; }

    // Setters
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }
    public void setScorePercentage(double scorePercentage) { this.scorePercentage = scorePercentage; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public void setAnswers(String answers) { this.answers = answers; }
}
