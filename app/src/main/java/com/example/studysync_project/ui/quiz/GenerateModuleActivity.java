package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.BuildConfig;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.databinding.ActivityGenerateModuleBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.IdUtil;
import com.example.studysync_project.utils.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Allows users to ask Gemini for a topic-based study module and optionally convert it to a quiz.
 */
public class GenerateModuleActivity extends AppCompatActivity {

    private static final int MIN_QUESTION_COUNT = 5;
    private static final int MAX_QUESTION_COUNT = 15;
    private static final int MAX_TOPIC_LENGTH = 80;
    private static final int MAX_GOAL_LENGTH = 180;

    private ActivityGenerateModuleBinding binding;
    private StudyModuleRepository studyModuleRepository;

    private String generatedModuleId;
    private String generatedModuleTitle;
    private String generatedModuleSubject;
    private String generatedModuleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGenerateModuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studyModuleRepository = new StudyModuleRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnGenerateModule.setOnClickListener(v -> generateStudyModule());
        binding.btnGenerateQuizNow.setOnClickListener(v -> turnIntoQuizNow());
        binding.btnOpenSavedModule.setOnClickListener(v -> openSavedModule());
    }

    private void generateStudyModule() {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.quiz_result_missing_api_key), Toast.LENGTH_LONG).show();
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, getString(R.string.quiz_result_offline), Toast.LENGTH_LONG).show();
            return;
        }

        String currentTopic = textOrEmpty(binding.etCurrentTopic.getText() != null
                ? binding.etCurrentTopic.getText().toString() : null);
        String interestTopic = textOrEmpty(binding.etInterestTopic.getText() != null
                ? binding.etInterestTopic.getText().toString() : null);
        String learningGoal = textOrEmpty(binding.etLearningGoal.getText() != null
                ? binding.etLearningGoal.getText().toString() : null);

        binding.etCurrentTopic.setError(null);
        binding.etInterestTopic.setError(null);
        binding.etLearningGoal.setError(null);

        if (currentTopic.isEmpty() && interestTopic.isEmpty()) {
            binding.etCurrentTopic.setError(getString(R.string.generate_module_topic_required));
            return;
        }
        if (!currentTopic.isEmpty() && currentTopic.length() > MAX_TOPIC_LENGTH) {
            binding.etCurrentTopic.setError(getString(R.string.generate_module_topic_too_long, MAX_TOPIC_LENGTH));
            return;
        }
        if (!interestTopic.isEmpty() && interestTopic.length() > MAX_TOPIC_LENGTH) {
            binding.etInterestTopic.setError(getString(R.string.generate_module_topic_too_long, MAX_TOPIC_LENGTH));
            return;
        }
        if (!learningGoal.isEmpty() && learningGoal.length() > MAX_GOAL_LENGTH) {
            binding.etLearningGoal.setError(getString(R.string.generate_module_goal_too_long, MAX_GOAL_LENGTH));
            return;
        }

        setLoading(true, getString(R.string.generate_module_loading));

        GeminiApiClient.generateTopicStudyModule(currentTopic, interestTopic, learningGoal)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        setLoading(false, "");

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(GenerateModuleActivity.this,
                                    getString(R.string.generate_module_failed),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String moduleText = extractGeminiText(response.body());
                        if (moduleText != null) {
                            moduleText = moduleText.replaceAll("```", "").trim();
                        }
                        if (moduleText == null || moduleText.isEmpty()) {
                            Toast.makeText(GenerateModuleActivity.this,
                                    getString(R.string.generate_module_empty),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String subject = !currentTopic.isEmpty() ? currentTopic : interestTopic;
                        String topic = !interestTopic.isEmpty() ? interestTopic : subject;
                        String moduleId = saveGeneratedModule(subject, topic, moduleText);
                        if (moduleId == null) {
                            return;
                        }

                        bindGeneratedModule(subject, topic, moduleText);
                        Toast.makeText(GenerateModuleActivity.this,
                                getString(R.string.generate_module_saved),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        setLoading(false, "");
                        Toast.makeText(GenerateModuleActivity.this,
                                getString(R.string.generate_module_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String saveGeneratedModule(String subject, String topic, String moduleText) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, R.string.generate_module_user_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        String moduleId = IdUtil.generateId("module");
        String title = getString(R.string.generate_module_title_format, topic);

        StudyModule module = new StudyModule(
                userId,
                title,
                subject,
                topic,
                getString(R.string.generate_module_description),
                moduleText,
                "AI_TOPIC_REQUEST",
                "gemini_topic_prompt"
        );
        module.setModuleId(moduleId);
        studyModuleRepository.upsertStudyModule(module, userId);

        generatedModuleId = moduleId;
        generatedModuleTitle = title;
        generatedModuleSubject = subject;
        generatedModuleText = moduleText;
        return moduleId;
    }

    private void bindGeneratedModule(String subject, String topic, String moduleText) {
        binding.cardGeneratedModule.setVisibility(View.VISIBLE);
        binding.tvGeneratedModuleTitle.setText(generatedModuleTitle);
        binding.tvGeneratedModuleMeta.setText(subject + " • " + topic);
        binding.tvGeneratedModuleContent.setText(moduleText);
    }

    private void turnIntoQuizNow() {
        if (generatedModuleText == null || generatedModuleText.trim().isEmpty()) {
            Toast.makeText(this, R.string.generate_module_no_generated_content, Toast.LENGTH_SHORT).show();
            return;
        }

        Integer questionCount = validateQuestionCount();
        if (questionCount == null) {
            return;
        }

        Intent intent = new Intent(this, UploadModuleActivity.class);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_ID, generatedModuleId);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TITLE, generatedModuleTitle);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_SUBJECT, generatedModuleSubject);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TEXT, generatedModuleText);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_TYPE, "AI_TOPIC_REQUEST");
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_REF, "gemini_topic_prompt");
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_QUESTION_COUNT, questionCount);
        startActivity(intent);
    }

    private void openSavedModule() {
        if (generatedModuleId == null || generatedModuleId.trim().isEmpty()) {
            Toast.makeText(this, R.string.generate_module_no_saved_module, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ModuleDetailActivity.class);
        intent.putExtra(ModuleDetailActivity.EXTRA_MODULE_ID, generatedModuleId);
        startActivity(intent);
    }

    @Nullable
    private Integer validateQuestionCount() {
        String countText = binding.etQuestionCount.getText() != null
                ? binding.etQuestionCount.getText().toString().trim()
                : "10";

        binding.etQuestionCount.setError(null);
        if (countText.isEmpty()) {
            binding.etQuestionCount.setError(getString(R.string.generate_module_question_count_required));
            return null;
        }

        int questionCount;
        try {
            questionCount = Integer.parseInt(countText);
        } catch (NumberFormatException ignored) {
            binding.etQuestionCount.setError(getString(R.string.generate_module_question_count_invalid));
            return null;
        }

        if (questionCount < MIN_QUESTION_COUNT || questionCount > MAX_QUESTION_COUNT) {
            binding.etQuestionCount.setError(getString(
                    R.string.generate_module_question_count_range,
                    MIN_QUESTION_COUNT,
                    MAX_QUESTION_COUNT
            ));
            return null;
        }
        return questionCount;
    }

    private void setLoading(boolean loading, String message) {
        binding.layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGenerateModule.setEnabled(!loading);
        binding.tvLoadingStatus.setText(message);
    }

    private static String textOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private static String extractGeminiText(JsonObject body) {
        if (body == null) return null;
        try {
            JsonArray candidates = body.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) return null;
            JsonObject candidate0 = candidates.get(0).getAsJsonObject();
            if (candidate0 == null) return null;
            JsonObject content = candidate0.getAsJsonObject("content");
            if (content == null) return null;
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.size() == 0) return null;
            JsonObject part0 = parts.get(0).getAsJsonObject();
            if (part0 == null || part0.get("text") == null) return null;
            return part0.get("text").getAsString();
        } catch (Exception e) {
            return null;
        }
    }
}