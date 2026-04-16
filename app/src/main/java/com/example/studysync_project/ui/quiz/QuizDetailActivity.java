package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.repository.QuestionRepository;
import com.example.studysync_project.data.repository.QuizRepository;
import com.example.studysync_project.databinding.ActivityQuizDetailBinding;
import com.example.studysync_project.utils.AppExecutors;
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
    private final Map<String, String> answers = new HashMap<>(); // questionId -> answer (A, B, C, D)
    private long startTime;
    private QuestionRepository questionRepository;
    private QuizRepository quizRepository;

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
        quizRepository = new QuizRepository(this);

        startTime = System.currentTimeMillis();

        // Load quiz and questions
        loadQuizData();
        setupUI();
    }

    private void loadQuizData() {
        AppExecutors.diskIO().execute(() -> {
            Quiz loadedQuiz = quizRepository.getQuizByIdSync(quizId);
            List<Question> loaded = questionRepository.getQuestionsForQuizSync(quizId);
            runOnUiThread(() -> {
                this.quiz = loadedQuiz;
                this.questions = loaded != null ? loaded : new ArrayList<>();

                if (quiz == null) {
                    Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (!quiz.isUnlocked()) {
                    Toast.makeText(this,
                            "This quiz is locked. Read the module first.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                if (questions.isEmpty()) {
                    Toast.makeText(this, "No questions found for this quiz", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                displayQuestion();
            });
        });
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

        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "No questions found for this quiz", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate score
        int correctAnswers = 0;
        ArrayList<Bundle> questionBundles = new ArrayList<>();
        ArrayList<String> userAnswers = new ArrayList<>();
        ArrayList<String> wrongQuestions = new ArrayList<>();

        for (Question question : questions) {
            String correctAnswer = normalizeAnswerLetter(question.getCorrectAnswer());
            String userAnswer = normalizeAnswerLetter(answers.get(question.getQuestionId()));

            Bundle bundle = new Bundle();
            bundle.putString("question", question.getQuestionText());
            bundle.putString("optionA", question.getOptionA());
            bundle.putString("optionB", question.getOptionB());
            bundle.putString("optionC", question.getOptionC());
            bundle.putString("optionD", question.getOptionD());
            bundle.putString("correctAnswer", correctAnswer != null ? correctAnswer : "");
            questionBundles.add(bundle);

            userAnswers.add(userAnswer != null ? userAnswer : "");

            if (correctAnswer != null && correctAnswer.equals(userAnswer)) {
                correctAnswers++;
            } else {
                wrongQuestions.add(question.getQuestionText() != null ? question.getQuestionText() : "");
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        int timeTakenMinutes = (int) (timeTaken / 60000L);

        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra(QuizResultActivity.EXTRA_SCORE, correctAnswers);
        intent.putExtra(QuizResultActivity.EXTRA_TOTAL, questionBundles.size());
        intent.putExtra(QuizResultActivity.EXTRA_SUBJECT, quiz != null ? quiz.getSubject() : null);
        intent.putExtra(QuizResultActivity.EXTRA_QUIZ_ID, quizId);
        intent.putStringArrayListExtra(QuizResultActivity.EXTRA_WRONG_QUESTIONS, wrongQuestions);
        intent.putParcelableArrayListExtra(QuizResultActivity.EXTRA_QUESTIONS, questionBundles);
        intent.putStringArrayListExtra(QuizResultActivity.EXTRA_USER_ANSWERS, userAnswers);
        intent.putExtra(QuizResultActivity.EXTRA_TIME_TAKEN_MINUTES, Math.max(timeTakenMinutes, 0));
        startActivity(intent);
        finish();
    }

    private static String normalizeAnswerLetter(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        char c = Character.toUpperCase(value.charAt(0));
        if (c == 'A' || c == 'B' || c == 'C' || c == 'D') {
            return String.valueOf(c);
        }
        return null;
    }
}
