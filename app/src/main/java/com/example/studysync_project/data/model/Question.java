package com.example.studysync_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.google.firebase.firestore.PropertyName;

/**
 * Represents a quiz question with multiple choice options
 */
@Entity(tableName = "questions")
public class Question {
    @PrimaryKey
    @NonNull
    public String questionId;

    public String quizId;  // Foreign key to Quiz
    public String questionText;
    public String optionA;
    public String optionB;
    public String optionC;
    public String optionD;

    @PropertyName("correct_answer")
    public String correctAnswer;  // A, B, C, or D

    public int questionNumber;
    public long createdAt;

    // Constructors
    public Question() {
        this.questionId = "";
    }

    public Question(String quizId, String questionText, String optionA, String optionB,
                    String optionC, String optionD, String correctAnswer, int questionNumber) {
        this.questionId = "";
        this.quizId = quizId;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.questionNumber = questionNumber;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    @NonNull
    public String getQuestionId() { return questionId; }
    public String getQuizId() { return quizId; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public int getQuestionNumber() { return questionNumber; }
    public long getCreatedAt() { return createdAt; }

    // Setters
    public void setQuestionId(@NonNull String questionId) { this.questionId = questionId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
}
