package com.example.studysync_project.utils;

import android.os.Handler;
import android.os.Looper;

import com.example.studysync_project.data.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArQuizManager {

    public interface ArQuizCallback {
        void onQuestionDisplayed(Question question, List<String> options);
        void onCorrectAnswer(Question question);
        void onIncorrectAnswer(Question question, String selectedAnswer, String correctAnswer);
        void onQuizCompleted(int correctAnswers, int totalQuestions);
        void onFaceNotDetected();
        void onWaitingForAnswer();
    }

    public enum QuizState {
        WAITING_FOR_FACE,
        DISPLAYING_QUESTION,
        WAITING_FOR_ANSWER,
        SHOWING_RESULT,
        QUIZ_COMPLETED
    }

    private final List<Question> questions;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private QuizState currentState = QuizState.WAITING_FOR_FACE;
    private final ArQuizCallback callback;
    private Question currentQuestion;
    private List<String> currentOptions;
    private long lastTiltTime = 0;
    private static final long TILT_COOLDOWN = 1000;

    public ArQuizManager(List<Question> questions, ArQuizCallback callback) {
        this.questions = questions;
        this.callback = callback;
    }

    public void startQuiz() {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        currentQuestionIndex = 0;
        correctAnswers = 0;
        currentState = QuizState.WAITING_FOR_FACE;
        displayCurrentQuestion();
    }

    public void onFaceDetected(boolean isFacingCamera) {
        if (currentState == QuizState.WAITING_FOR_FACE) {
            if (isFacingCamera) {
                currentState = QuizState.DISPLAYING_QUESTION;
                displayCurrentQuestion();
            } else {
                if (callback != null) callback.onFaceNotDetected();
            }
        }
    }

    public void onNoFaceDetected() {
        if (currentState != QuizState.WAITING_FOR_FACE) {
            currentState = QuizState.WAITING_FOR_FACE;
            if (callback != null) callback.onFaceNotDetected();
        }
    }

    public void onHeadTilt(FaceDetectionUtil.HeadTiltDirection direction) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTiltTime < TILT_COOLDOWN) {
            return;
        }

        if (currentState == QuizState.WAITING_FOR_ANSWER && currentOptions != null) {
            int selectedIndex = -1;

            switch (direction) {
                case UP:    selectedIndex = 0; break;
                case LEFT:  selectedIndex = 1; break;
                case RIGHT: selectedIndex = 2; break;
                case DOWN:  selectedIndex = 3; break;
            }

            if (selectedIndex >= 0 && selectedIndex < currentOptions.size()) {
                lastTiltTime = currentTime;
                processAnswer(selectedIndex);
            }
        }
    }

    private void displayCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            currentState = QuizState.QUIZ_COMPLETED;
            if (callback != null) callback.onQuizCompleted(correctAnswers, questions.size());
            return;
        }

        currentQuestion = questions.get(currentQuestionIndex);
        currentOptions = buildOptions(currentQuestion);

        currentState = QuizState.WAITING_FOR_ANSWER;
        if (callback != null) {
            callback.onQuestionDisplayed(currentQuestion, currentOptions);
            callback.onWaitingForAnswer();
        }
    }

    // Build a shuffled list of option texts from optionA/B/C/D
    private List<String> buildOptions(Question question) {
        List<String> options = new ArrayList<>();
        if (question.getOptionA() != null && !question.getOptionA().trim().isEmpty())
            options.add(question.getOptionA());
        if (question.getOptionB() != null && !question.getOptionB().trim().isEmpty())
            options.add(question.getOptionB());
        if (question.getOptionC() != null && !question.getOptionC().trim().isEmpty())
            options.add(question.getOptionC());
        if (question.getOptionD() != null && !question.getOptionD().trim().isEmpty())
            options.add(question.getOptionD());
        Collections.shuffle(options);
        return options;
    }

    // Resolve the correct answer text from the letter stored in correctAnswer field
    private String resolveCorrectAnswerText(Question question) {
        String letter = question.getCorrectAnswer();
        if (letter == null) return "";
        switch (letter.toUpperCase().trim()) {
            case "A": return question.getOptionA() != null ? question.getOptionA() : "";
            case "B": return question.getOptionB() != null ? question.getOptionB() : "";
            case "C": return question.getOptionC() != null ? question.getOptionC() : "";
            case "D": return question.getOptionD() != null ? question.getOptionD() : "";
            default:  return letter; // fallback if full text is stored
        }
    }

    private void processAnswer(int selectedIndex) {
        if (currentQuestion == null || currentOptions == null) return;

        String selectedAnswer = currentOptions.get(selectedIndex);
        String correctAnswerText = resolveCorrectAnswerText(currentQuestion);

        currentState = QuizState.SHOWING_RESULT;

        if (selectedAnswer.equals(correctAnswerText)) {
            correctAnswers++;
            if (callback != null) callback.onCorrectAnswer(currentQuestion);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                currentQuestionIndex++;
                currentState = QuizState.DISPLAYING_QUESTION;
                displayCurrentQuestion();
            }, 2000);
        } else {
            if (callback != null) {
                callback.onIncorrectAnswer(currentQuestion, selectedAnswer, correctAnswerText);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                currentQuestionIndex++;
                currentState = QuizState.DISPLAYING_QUESTION;
                displayCurrentQuestion();
            }, 4000);
        }
    }

    public QuizState getCurrentState() { return currentState; }
    public Question getCurrentQuestion() { return currentQuestion; }
    public int getCurrentQuestionIndex() { return currentQuestionIndex; }
    public int getTotalQuestions() { return questions != null ? questions.size() : 0; }
    public int getCorrectAnswers() { return correctAnswers; }
}
