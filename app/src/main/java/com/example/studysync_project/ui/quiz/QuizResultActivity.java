package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.databinding.ActivityQuizResultBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.IdUtil;
import com.example.studysync_project.utils.PdfExportUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizResultActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "score";
    public static final String EXTRA_TOTAL = "total";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_WRONG_QUESTIONS = "wrong_questions";
    public static final String EXTRA_QUESTIONS = "questions";

    private ActivityQuizResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuizResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int score = getIntent().getIntExtra(EXTRA_SCORE, 0);
        int total = getIntent().getIntExtra(EXTRA_TOTAL, 1);
        String subject = getIntent().getStringExtra(EXTRA_SUBJECT);
        ArrayList<String> wrongQuestions = getIntent().getStringArrayListExtra(EXTRA_WRONG_QUESTIONS);
        ArrayList<Bundle> questions = getIntent().getParcelableArrayListExtra(EXTRA_QUESTIONS);

        int percent = (score * 100) / total;

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Display score
        binding.tvScorePercent.setText(percent + "%");
        binding.tvScoreDetail.setText(score + " out of " + total + " correct");
        binding.tvScoreLabel.setText(getScoreLabel(percent));

        // Save attempt
        saveAttempt(score, total, percent, subject);

        // Get AI feedback
        String wrongTopics = wrongQuestions != null ? String.join("\n", wrongQuestions) : "";
        fetchAiFeedback(subject, score, total, wrongTopics);

        binding.btnDone.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.studysync_project.MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        binding.btnRetake.setOnClickListener(v -> {
            if (questions != null) {
                Intent intent = new Intent(this, QuizTakeActivity.class);
                intent.putExtra(QuizTakeActivity.EXTRA_SUBJECT, subject);
                intent.putParcelableArrayListExtra(QuizTakeActivity.EXTRA_QUESTIONS, questions);
                startActivity(intent);
                finish();
            }
        });

        // Export PDF
        binding.toolbar.inflateMenu(com.example.studysync_project.R.menu.menu_export);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == com.example.studysync_project.R.id.action_export_pdf) {
                exportResultPdf(subject, score, total, percent);
                return true;
            }
            return false;
        });
    }

    private void fetchAiFeedback(String subject, int score, int total, String wrongTopics) {
        binding.progressFeedback.setVisibility(View.VISIBLE);
        binding.tvAiFeedback.setVisibility(View.GONE);

        GeminiApiClient.analyzePerformance(subject, score, total, wrongTopics)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        binding.progressFeedback.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String feedback = response.body()
                                        .getAsJsonArray("candidates").get(0).getAsJsonObject()
                                        .getAsJsonObject("content")
                                        .getAsJsonArray("parts").get(0).getAsJsonObject()
                                        .get("text").getAsString();
                                binding.tvAiFeedback.setText(feedback.trim());
                                binding.tvAiFeedback.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                showFallbackFeedback(score, total);
                            }
                        } else {
                            showFallbackFeedback(score, total);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        binding.progressFeedback.setVisibility(View.GONE);
                        showFallbackFeedback(score, total);
                    }
                });
    }

    private void showFallbackFeedback(int score, int total) {
        int percent = (score * 100) / total;
        String feedback;
        if (percent >= 80) {
            feedback = "Great work! You have a strong understanding of this topic. Keep reviewing to maintain your knowledge and explore more advanced concepts.";
        } else if (percent >= 60) {
            feedback = "Good effort! You have a decent grasp of the basics. Review the questions you missed and focus on understanding the underlying concepts.";
        } else {
            feedback = "Keep going! This topic needs more attention. Re-read your module carefully, take notes on key points, and try the quiz again after reviewing.";
        }
        binding.tvAiFeedback.setText(feedback);
        binding.tvAiFeedback.setVisibility(View.VISIBLE);
    }

    private String getScoreLabel(int percent) {
        if (percent >= 90) return "Excellent!";
        if (percent >= 75) return "Good job!";
        if (percent >= 60) return "Keep it up!";
        return "Needs improvement";
    }

    private void saveAttempt(int score, int total, int percent, String subject) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) return;

        QuizAttempt attempt = new QuizAttempt(userId, subject != null ? subject : "", total, score, percent, 0);
        attempt.setAttemptId(IdUtil.generateId("attempt"));
        attempt.setPassed(percent >= 60);
        new QuizAttemptRepository(this).saveQuizAttempt(attempt, userId);
    }

    private void exportResultPdf(String subject, int score, int total, int percent) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Subject: " + (subject != null ? subject : "N/A"));
        lines.add("Score: " + score + " / " + total + "  (" + percent + "%)");
        lines.add("Result: " + (percent >= 60 ? "PASSED" : "FAILED"));
        lines.add("");
        lines.add("## AI Feedback");
        String feedback = binding.tvAiFeedback.getText() != null
                ? binding.tvAiFeedback.getText().toString() : "";
        // Split long feedback into lines
        for (int i = 0; i < feedback.length(); i += 80) {
            lines.add(feedback.substring(i, Math.min(i + 80, feedback.length())));
        }
        PdfExportUtil.exportAndShare(this, "Quiz Result — " + (subject != null ? subject : "Quiz"),
                lines, "quiz_result_" + System.currentTimeMillis());
    }
}
