package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.databinding.ActivityQuizTakeBinding;

import java.util.ArrayList;

public class QuizTakeActivity extends AppCompatActivity {

    public static final String EXTRA_QUESTIONS = "questions";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_QUIZ_ID = "quiz_id";

    private ActivityQuizTakeBinding binding;
    private ArrayList<Bundle> questions;
    private String subject;
    private String quizId;
    private int currentIndex = 0;
    private String[] userAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizTakeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        subject = getIntent().getStringExtra(EXTRA_SUBJECT);
        quizId = getIntent().getStringExtra(EXTRA_QUIZ_ID);
        questions = getIntent().getParcelableArrayListExtra(EXTRA_QUESTIONS);

        if (questions == null || questions.isEmpty()) {
            finish();
            return;
        }

        userAnswers = new String[questions.size()];
        binding.toolbar.setTitle(subject != null ? subject + " Quiz" : "Quiz");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        showQuestion(0);

        binding.btnNext.setOnClickListener(v -> {
            if (!hasSelectedAnswer()) {
                Toast.makeText(this, "Select an answer to continue", Toast.LENGTH_SHORT).show();
                return;
            }
            saveAnswer();
            if (currentIndex < questions.size() - 1) {
                showQuestion(currentIndex + 1);
            } else {
                submitQuiz();
            }
        });

        binding.btnPrevious.setOnClickListener(v -> {
            saveAnswer();
            if (currentIndex > 0) showQuestion(currentIndex - 1);
        });
    }

    private void showQuestion(int index) {
        currentIndex = index;
        Bundle q = questions.get(index);

        binding.tvQuestionCounter.setText("Question " + (index + 1) + " of " + questions.size());
        binding.tvQuestion.setText(q.getString("question"));
        binding.progressQuiz.setProgress((int) (((index + 1) * 100.0) / questions.size()));

        // Rebuild radio buttons
        binding.rgOptions.removeAllViews();
        String[] optionKeys = {"optionA", "optionB", "optionC", "optionD"};
        String[] labels = {"A", "B", "C", "D"};

        for (int i = 0; i < 4; i++) {
            String optionText = q.getString(optionKeys[i]);
            if (optionText == null || optionText.isEmpty()) continue;

            RadioButton rb = new RadioButton(this);
            rb.setText(labels[i] + ".  " + optionText);
            rb.setTag(labels[i]);
            rb.setPadding(16, 24, 16, 24);
            rb.setTextSize(15f);
            rb.setTextColor(getColor(com.example.studysync_project.R.color.text_primary));
            binding.rgOptions.addView(rb);

            // Restore previous answer
            if (labels[i].equals(userAnswers[index])) rb.setChecked(true);
        }

        // Require an answer before moving forward
        binding.btnNext.setEnabled(hasSelectedAnswer());
        binding.rgOptions.setOnCheckedChangeListener((group, checkedId) ->
                binding.btnNext.setEnabled(checkedId != -1));

        binding.btnPrevious.setEnabled(index > 0);
        binding.btnNext.setText(index == questions.size() - 1 ? "Submit" : "Next");
    }

    private void saveAnswer() {
        int selectedId = binding.rgOptions.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton rb = findViewById(selectedId);
            if (rb != null) userAnswers[currentIndex] = (String) rb.getTag();
        }
    }

    private void submitQuiz() {
        saveAnswer();

        int correct = 0;
        ArrayList<String> wrongQuestions = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            String correctAnswer = questions.get(i).getString("correctAnswer");
            if (correctAnswer != null && correctAnswer.equals(userAnswers[i])) {
                correct++;
            } else {
                wrongQuestions.add(questions.get(i).getString("question", ""));
            }
        }

        Intent intent = new Intent(this, QuizResultActivity.class);
        intent.putExtra(QuizResultActivity.EXTRA_SCORE, correct);
        intent.putExtra(QuizResultActivity.EXTRA_TOTAL, questions.size());
        intent.putExtra(QuizResultActivity.EXTRA_SUBJECT, subject);
        intent.putExtra(QuizResultActivity.EXTRA_QUIZ_ID, quizId);
        intent.putStringArrayListExtra(QuizResultActivity.EXTRA_WRONG_QUESTIONS, wrongQuestions);
        intent.putParcelableArrayListExtra(QuizResultActivity.EXTRA_QUESTIONS, questions);
        intent.putStringArrayListExtra(QuizResultActivity.EXTRA_USER_ANSWERS, toStringArrayList(userAnswers));
        startActivity(intent);
        finish();
    }

    private boolean hasSelectedAnswer() {
        int selectedId = binding.rgOptions.getCheckedRadioButtonId();
        return selectedId != -1;
    }

    private static ArrayList<String> toStringArrayList(String[] arr) {
        ArrayList<String> list = new ArrayList<>();
        if (arr == null) return list;
        for (String s : arr) list.add(s);
        return list;
    }
}
