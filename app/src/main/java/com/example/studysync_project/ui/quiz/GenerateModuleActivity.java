package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.BuildConfig;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.data.repository.UserRepository;
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
 * Lets learners create their own modules or generate one with AI using learner context.
 */
public class GenerateModuleActivity extends AppCompatActivity {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_TOPIC_LENGTH = 80;
    private static final int MAX_GOAL_LENGTH = 180;
    private static final int MAX_CONTENT_LENGTH = 20000;
    private static final int MAX_GRADE_LENGTH = 40;
    private static final int MAX_FORMATIVE_LENGTH = 1500;

    private ActivityGenerateModuleBinding binding;
    private StudyModuleRepository studyModuleRepository;
    private UserRepository userRepository;

    private String generatedModuleId;
    private String generatedModuleTitle;
    private String generatedModuleSubject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGenerateModuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studyModuleRepository = new StudyModuleRepository(this);
        userRepository = new UserRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnSaveModule.setOnClickListener(v -> saveManualModule());
        binding.btnGenerateModule.setOnClickListener(v -> generateStudyModuleWithAi());
        binding.btnOpenExternalAnalysis.setOnClickListener(v -> openExternalUpload());
        binding.btnOpenSavedModule.setOnClickListener(v -> openSavedModule());

        prefillGradeFromProfile();
    }

    private void prefillGradeFromProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (userId == null) {
            return;
        }

        userRepository.getUserProfile(userId).observe(this, profile -> {
            if (profile == null) {
                return;
            }
            maybePrefillGrade(profile);
        });
    }

    private void maybePrefillGrade(UserProfile profile) {
        if (binding.etGradeLevel.getText() != null
                && !textOrEmpty(binding.etGradeLevel.getText().toString()).isEmpty()) {
            return;
        }

        String gradeLevel = textOrEmpty(profile.getGradeLevel());
        if (!gradeLevel.isEmpty()) {
            binding.etGradeLevel.setText(gradeLevel);
        }
    }

    private void saveManualModule() {
        String title = textOrEmpty(binding.etModuleTitle.getText() != null
                ? binding.etModuleTitle.getText().toString() : null);
        String subject = textOrEmpty(binding.etCurrentTopic.getText() != null
                ? binding.etCurrentTopic.getText().toString() : null);
        String topic = textOrEmpty(binding.etInterestTopic.getText() != null
                ? binding.etInterestTopic.getText().toString() : null);
        String moduleText = textOrEmpty(binding.etModuleContent.getText() != null
                ? binding.etModuleContent.getText().toString() : null);

        clearManualInputErrors();

        if (title.length() > MAX_TITLE_LENGTH) {
            binding.etModuleTitle.setError(getString(R.string.generate_module_title_too_long, MAX_TITLE_LENGTH));
            return;
        }
        if (subject.isEmpty() && topic.isEmpty()) {
            binding.etCurrentTopic.setError(getString(R.string.generate_module_topic_required));
            return;
        }
        if (!subject.isEmpty() && subject.length() > MAX_TOPIC_LENGTH) {
            binding.etCurrentTopic.setError(getString(R.string.generate_module_topic_too_long, MAX_TOPIC_LENGTH));
            return;
        }
        if (!topic.isEmpty() && topic.length() > MAX_TOPIC_LENGTH) {
            binding.etInterestTopic.setError(getString(R.string.generate_module_topic_too_long, MAX_TOPIC_LENGTH));
            return;
        }
        if (moduleText.isEmpty()) {
            binding.etModuleContent.setError(getString(R.string.generate_module_content_required));
            return;
        }
        if (moduleText.length() > MAX_CONTENT_LENGTH) {
            binding.etModuleContent.setError(getString(
                    R.string.generate_module_content_too_long,
                    MAX_CONTENT_LENGTH
            ));
            return;
        }

        String resolvedSubject = !subject.isEmpty() ? subject : topic;
        String resolvedTopic = !topic.isEmpty() ? topic : resolvedSubject;
        String resolvedTitle = !title.isEmpty()
                ? title
                : getString(R.string.generate_module_manual_title_format, resolvedTopic);

        String moduleId = saveModule(
                resolvedTitle,
                resolvedSubject,
                resolvedTopic,
                getString(R.string.generate_module_manual_description),
                moduleText,
                "MANUAL",
                "manual_entry"
        );
        if (moduleId == null) {
            return;
        }

        bindSavedModuleCard(resolvedSubject, resolvedTopic, moduleText);
        Toast.makeText(this, getString(R.string.generate_module_saved), Toast.LENGTH_SHORT).show();
    }

    private void generateStudyModuleWithAi() {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.quiz_result_missing_api_key), Toast.LENGTH_LONG).show();
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, getString(R.string.quiz_result_offline), Toast.LENGTH_LONG).show();
            return;
        }

        String moduleTitle = textOrEmpty(binding.etModuleTitle.getText() != null
                ? binding.etModuleTitle.getText().toString() : null);
        String currentTopic = textOrEmpty(binding.etCurrentTopic.getText() != null
                ? binding.etCurrentTopic.getText().toString() : null);
        String interestTopic = textOrEmpty(binding.etInterestTopic.getText() != null
                ? binding.etInterestTopic.getText().toString() : null);
        String gradeLevel = textOrEmpty(binding.etGradeLevel.getText() != null
                ? binding.etGradeLevel.getText().toString() : null);
        String formativeAssessment = textOrEmpty(binding.etFormativeAssessment.getText() != null
                ? binding.etFormativeAssessment.getText().toString() : null);
        String learningGoal = textOrEmpty(binding.etLearningGoal.getText() != null
                ? binding.etLearningGoal.getText().toString() : null);

        clearAiInputErrors();

        if (!moduleTitle.isEmpty() && moduleTitle.length() > MAX_TITLE_LENGTH) {
            binding.etModuleTitle.setError(getString(R.string.generate_module_title_too_long, MAX_TITLE_LENGTH));
            return;
        }
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
        if (gradeLevel.isEmpty()) {
            binding.etGradeLevel.setError(getString(R.string.generate_module_grade_required));
            return;
        }
        if (gradeLevel.length() > MAX_GRADE_LENGTH) {
            binding.etGradeLevel.setError(getString(R.string.generate_module_grade_too_long, MAX_GRADE_LENGTH));
            return;
        }
        if (formativeAssessment.isEmpty()) {
            binding.etFormativeAssessment.setError(getString(R.string.generate_module_formative_required));
            return;
        }
        if (formativeAssessment.length() > MAX_FORMATIVE_LENGTH) {
            binding.etFormativeAssessment.setError(getString(
                    R.string.generate_module_formative_too_long,
                    MAX_FORMATIVE_LENGTH
            ));
            return;
        }
        if (!learningGoal.isEmpty() && learningGoal.length() > MAX_GOAL_LENGTH) {
            binding.etLearningGoal.setError(getString(R.string.generate_module_goal_too_long, MAX_GOAL_LENGTH));
            return;
        }

        setLoading(true, getString(R.string.generate_module_loading));

        GeminiApiClient.generateTopicStudyModule(
                        currentTopic,
                        interestTopic,
                        learningGoal,
                        gradeLevel,
                        formativeAssessment
                )
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
                        moduleText = GeminiApiClient.sanitizeModuleOutput(moduleText);
                        if (moduleText == null || moduleText.isEmpty()) {
                            Toast.makeText(GenerateModuleActivity.this,
                                    getString(R.string.generate_module_empty),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String subject = !currentTopic.isEmpty() ? currentTopic : interestTopic;
                        String topic = !interestTopic.isEmpty() ? interestTopic : subject;
                        String title = !moduleTitle.isEmpty()
                                ? moduleTitle
                                : getString(R.string.generate_module_title_format, topic);

                        String moduleId = saveModule(
                                title,
                                subject,
                                topic,
                                getString(R.string.generate_module_description),
                                moduleText,
                                "AI_FORMATIVE",
                                "gemini_formative_prompt"
                        );
                        if (moduleId == null) {
                            return;
                        }

                        binding.etModuleContent.setText(moduleText);
                        bindSavedModuleCard(subject, topic, moduleText);
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

    private void clearManualInputErrors() {
        binding.etModuleTitle.setError(null);
        binding.etCurrentTopic.setError(null);
        binding.etInterestTopic.setError(null);
        binding.etModuleContent.setError(null);
    }

    private void clearAiInputErrors() {
        binding.etModuleTitle.setError(null);
        binding.etCurrentTopic.setError(null);
        binding.etInterestTopic.setError(null);
        binding.etGradeLevel.setError(null);
        binding.etFormativeAssessment.setError(null);
        binding.etLearningGoal.setError(null);
    }

    private String saveModule(
            String title,
            String subject,
            String topic,
            String description,
            String moduleText,
            String sourceType,
            String sourceRef
    ) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (userId == null) {
            Toast.makeText(this, R.string.generate_module_user_missing, Toast.LENGTH_LONG).show();
            return null;
        }

        String moduleId = IdUtil.generateId("module");
        StudyModule module = new StudyModule(
                userId,
                title,
                subject,
                topic,
                description,
                moduleText,
                sourceType,
                sourceRef
        );
        module.setModuleId(moduleId);
        studyModuleRepository.upsertStudyModule(module, userId);

        generatedModuleId = moduleId;
        generatedModuleTitle = title;
        generatedModuleSubject = subject;
        return moduleId;
    }

    private void bindSavedModuleCard(String subject, String topic, String moduleText) {
        binding.cardGeneratedModule.setVisibility(View.VISIBLE);
        binding.tvGeneratedModuleTitle.setText(generatedModuleTitle);
        binding.tvGeneratedModuleMeta.setText(subject + " • " + topic);
        binding.tvGeneratedModuleContent.setText(moduleText);
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

    private void openExternalUpload() {
        startActivity(new Intent(this, UploadModuleActivity.class));
    }

    private void setLoading(boolean loading, String message) {
        binding.layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSaveModule.setEnabled(!loading);
        binding.btnGenerateModule.setEnabled(!loading);
        binding.btnOpenExternalAnalysis.setEnabled(!loading);
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
