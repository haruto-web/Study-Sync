package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.repository.QuestionRepository;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.databinding.ActivityQuizDetailBinding;
import com.example.studysync_project.utils.IdUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for taking a quiz
 * Displays questions one by one and calculates score at the end
 */
public class QuizDetailActivity extends AppCompatActivity {

    private ActivityQuizDetailBinding binding;
    private String quizId;
    private String userId;
    private Quiz quiz;
    private List<Question> questions;
    private int currentQuestionIndex = 0;
    private Map<String, String> answers = new HashMap<>(); // questionId -> answer (A, B, C, D)
    private long startTime;
    private QuestionRepository questionRepository;
    private QuizAttemptRepository quizAttemptRepository;

    public static final String EXTRA_QUIZ_ID = "quiz_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        quizId = getIntent().getStringExtra(EXTRA_QUIZ_ID);
        if (quizId == null) {
            Toast.makeText(this, "Quiz ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        questionRepository = new QuestionRepository(this);
        quizAttemptRepository = new QuizAttemptRepository(this);

        startTime = System.currentTimeMillis();

        // Load quiz and questions
        loadQuizData();
        setupUI();
    }

    private void loadQuizData() {
        // In production, fetch from repository
        // For now, using placeholder
        quiz = new Quiz();
        quiz.setQuizId(quizId);
        quiz.setTitle("Sample Quiz");
        quiz.setTotalQuestions(3);
        quiz.setPassingScore(60);

        // Load questions from repository (synchronously)
        List<Question> loadedQuestions = questionRepository.getQuestionsForQuizSync(quizId);
        this.questions = loadedQuestions != null ? loadedQuestions : new ArrayList<>();
        
        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions found for this quiz", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        displayQuestion();
    }

    private void setupUI() {
        binding.toolbarQuizDetail.setNavigationOnClickListener(v -> finish());

        binding.btnPrevious.setOnClickListener(v -> previousQuestion());
        binding.btnNext.setOnClickListener(v -> nextQuestion());
        binding.btnSubmit.setOnClickListener(v -> submitQuiz());
    }

    private void displayQuestion() {
        if (questions == null || currentQuestionIndex >= questions.size()) {
            return;
        }

        Question question = questions.get(currentQuestionIndex);
        binding.tvQuestion.setText((currentQuestionIndex + 1) + ". " + question.getQuestionText());

        // Setup radio buttons for options
        binding.rgOptions.removeAllViews();

        String[] options = {question.getOptionA(), question.getOptionB(), 
                           question.getOptionC(), question.getOptionD()};
        String[] optionLabels = {"A", "B", "C", "D"};

        for (int i = 0; i < options.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(optionLabels[i] + ". " + options[i]);
            rb.setTag(optionLabels[i]);
            binding.rgOptions.addView(rb);
        }

        // Update progress
        binding.tvProgress.setText((currentQuestionIndex + 1) + " / " + questions.size());

        // Enable/disable buttons
        binding.btnPrevious.setEnabled(currentQuestionIndex > 0);
        binding.btnNext.setEnabled(currentQuestionIndex < questions.size() - 1);
        binding.btnSubmit.setEnabled(currentQuestionIndex == questions.size() - 1);
    }

    private void previousQuestion() {
        if (currentQuestionIndex > 0) {
            saveCurrentAnswer();
            currentQuestionIndex--;
            displayQuestion();
        }
    }

    private void nextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            saveCurrentAnswer();
            currentQuestionIndex++;
            displayQuestion();
        }
    }

    private void saveCurrentAnswer() {
        int selectedId = binding.rgOptions.getCheckedRadioButtonId();
        if (selectedId != -1) {
            android.widget.RadioButton rb = findViewById(selectedId);
            String answer = (String) rb.getTag();
            answers.put(questions.get(currentQuestionIndex).getQuestionId(), answer);
        }
    }

    private void submitQuiz() {
        saveCurrentAnswer();

        // Calculate score
        int correctAnswers = 0;
        for (Question question : questions) {
            String userAnswer = answers.get(question.getQuestionId());
            if (question.getCorrectAnswer().equals(userAnswer)) {
                correctAnswers++;
            }
        }

        double scorePercentage = (correctAnswers * 100.0) / questions.size();
        boolean passed = scorePercentage >= quiz.getPassingScore();

        long timeTaken = System.currentTimeMillis() - startTime;
        int timeTakenMinutes = (int) (timeTaken / 60000);

        // Create and save quiz attempt
        QuizAttempt attempt = new QuizAttempt(
            userId,
            quizId,
            questions.size(),
            correctAnswers,
            scorePercentage,
            timeTakenMinutes
        );
        attempt.setAttemptId(IdUtil.generateId("attempt"));
        attempt.setPassed(passed);

        // Convert answers map to JSON string (simplified)
        StringBuilder answersJson = new StringBuilder("{");
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            answersJson.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\",");
        }
        if (answersJson.length() > 1) {
            answersJson.deleteCharAt(answersJson.length() - 1);
        }
        answersJson.append("}");
        attempt.setAnswers(answersJson.toString());

        // Save attempt
        quizAttemptRepository.saveQuizAttempt(attempt, userId);

        // Show result
        Toast.makeText(this, "Score: " + (int)scorePercentage + "% - " + 
                (passed ? "PASSED" : "FAILED"), Toast.LENGTH_LONG).show();

        // Go back
        finish();
    }
}
