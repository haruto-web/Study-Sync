package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.BuildConfig;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.QuestionRepository;
import com.example.studysync_project.data.repository.QuizRepository;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.databinding.ActivityUploadModuleBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.IdUtil;
import com.example.studysync_project.utils.NetworkUtil;
import com.example.studysync_project.utils.TextExtractorUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadModuleActivity extends AppCompatActivity {

    public static final String EXTRA_MODULE_ID = "extra_module_id";
    public static final String EXTRA_MODULE_SOURCE_TYPE = "extra_module_source_type";
    public static final String EXTRA_MODULE_SOURCE_REF = "extra_module_source_ref";
    public static final String EXTRA_READY_MODULE_TEXT = "extra_ready_module_text";
    public static final String EXTRA_READY_MODULE_TITLE = "extra_ready_module_title";
    public static final String EXTRA_READY_MODULE_SUBJECT = "extra_ready_module_subject";
    public static final String EXTRA_READY_MODULE_QUESTION_COUNT = "extra_ready_module_question_count";

    private ActivityUploadModuleBinding binding;
    private Uri selectedFileUri;
    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                selectedFileUri = uri;
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                binding.tvFileName.setText(getFileName(uri));
            });
    private String extractedText;
    private String providedModuleId;
    private String providedModuleText;
    private String providedModuleTitle;
    private String providedModuleSourceType;
    private String providedModuleSourceRef;
    private StudyModuleRepository studyModuleRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadModuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studyModuleRepository = new StudyModuleRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        providedModuleId = getIntent().getStringExtra(EXTRA_MODULE_ID);
        providedModuleText = getIntent().getStringExtra(EXTRA_READY_MODULE_TEXT);
        providedModuleTitle = getIntent().getStringExtra(EXTRA_READY_MODULE_TITLE);
        providedModuleSourceType = getIntent().getStringExtra(EXTRA_MODULE_SOURCE_TYPE);
        providedModuleSourceRef = getIntent().getStringExtra(EXTRA_MODULE_SOURCE_REF);
        String providedSubject = getIntent().getStringExtra(EXTRA_READY_MODULE_SUBJECT);
        int providedCount = getIntent().getIntExtra(EXTRA_READY_MODULE_QUESTION_COUNT, 10);

        if (providedModuleText != null && !providedModuleText.trim().isEmpty()) {
            binding.tvFileName.setText("Ready module: " + (providedModuleTitle != null ? providedModuleTitle : "Module"));
            binding.cardPickFile.setEnabled(false);
            binding.cardPickFile.setAlpha(0.6f);
            if (providedSubject != null && binding.etSubject.getText() != null) {
                binding.etSubject.setText(providedSubject);
            }
            if (binding.etQuestionCount.getText() != null) {
                binding.etQuestionCount.setText(String.valueOf(providedCount));
            }
        }
        binding.cardPickFile.setOnClickListener(v ->
                filePicker.launch(new String[]{"application/pdf", "text/plain",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"}));

        binding.btnGenerate.setOnClickListener(v -> startGeneration());
    }

    private void startGeneration() {
        boolean usingProvidedText = providedModuleText != null && !providedModuleText.trim().isEmpty();
        if (!usingProvidedText && selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            Toast.makeText(this,
                    "Missing GEMINI_API_KEY. Add GEMINI_API_KEY=... to local.properties and rebuild.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
            return;
        }

        String subject = binding.etSubject.getText() != null
                ? binding.etSubject.getText().toString().trim() : "";
        if (subject.isEmpty()) {
            binding.etSubject.setError("Please enter a subject");
            return;
        }
        String countStr = binding.etQuestionCount.getText() != null
                ? binding.etQuestionCount.getText().toString().trim() : "10";
        int questionCount = 10;
        try {
            questionCount = Integer.parseInt(countStr);
            if (questionCount < 5) questionCount = 5;
            if (questionCount > 15) questionCount = 15;
        } catch (NumberFormatException ignored) {
        }

        int finalCount = questionCount;
        String finalSubject = subject;

        if (usingProvidedText) {
            String moduleContent = normalizeForStorage(providedModuleText);
            if (moduleContent.isEmpty()) {
                Toast.makeText(this, "Module content is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            String moduleTitle = resolveModuleTitle(null);
            String moduleId = upsertStudyModule(moduleContent, finalSubject, moduleTitle);
            extractedText = toAiPromptText(moduleContent);

            setLoading(true, "AI is analyzing your module...");
            callGemini(extractedText, finalSubject, finalCount, moduleId, moduleTitle);
            return;
        }

        setLoading(true, "Extracting text from file...");

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String text = TextExtractorUtil.extract(this, selectedFileUri);

            runOnUiThread(() -> {
                if (text == null || text.trim().isEmpty()) {
                    setLoading(false, "");
                    Toast.makeText(this, "Could not read file content. Try a TXT or PDF file.", Toast.LENGTH_LONG).show();
                    return;
                }

                String moduleContent = normalizeForStorage(text);
                String sourceName = getFileName(selectedFileUri);
                String moduleTitle = resolveModuleTitle(sourceName);
                String moduleId = upsertStudyModule(moduleContent, finalSubject, sourceName);
                extractedText = toAiPromptText(moduleContent);

                setLoading(true, "AI is analyzing your module...");
                callGemini(extractedText, finalSubject, finalCount, moduleId, moduleTitle);
            });
        });
        executor.shutdown();
    }

    private void callGemini(
            String moduleText,
            String subject,
            int questionCount,
            String moduleId,
            String sourceModuleTitle
    ) {
        GeminiApiClient.generateQuiz(moduleText, subject, questionCount)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        setLoading(false, "");
                        if (!response.isSuccessful() || response.body() == null) {
                            String msg;
                            int code = response.code();
                            if (code == 401 || code == 403) {
                                msg = "Invalid API key (" + code + ").";
                            } else if (code == 404) {
                                msg = "Gemini endpoint/model not found (404). Updating model fallback may fix this.";
                            } else if (code == 429) {
                                msg = "API quota exceeded (429). Try again later.";
                            } else if (code >= 500) {
                                msg = "Gemini server error (" + code + "). Try again later.";
                            } else {
                                msg = "AI request failed (" + code + ").";
                            }

                            try {
                                if (response.errorBody() != null) {
                                    String err = response.errorBody().string();
                                    if (err != null) {
                                        err = err.trim().replaceAll("\\s+", " ");
                                        if (err.length() > 200) err = err.substring(0, 200) + "…";
                                        msg = msg + " " + err;
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                            Toast.makeText(UploadModuleActivity.this, msg, Toast.LENGTH_LONG).show();
                            return;
                        }
                        try {
                            String rawText = extractGeminiText(response.body());
                            if (rawText == null || rawText.trim().isEmpty()) {
                                Toast.makeText(UploadModuleActivity.this,
                                        "AI returned an empty response. Try again.", Toast.LENGTH_LONG).show();
                                return;
                            }

                            rawText = rawText.replaceAll("```json", "").replaceAll("```", "").trim();

                            JsonArray questionsJson = JsonParser.parseString(rawText).getAsJsonArray();
                            ArrayList<String[]> questions = new ArrayList<>();
                            List<Question> questionEntities = new ArrayList<>();
                            for (JsonElement el : questionsJson) {
                                JsonObject q = el.getAsJsonObject();
                                String questionText = q.get("question").getAsString();
                                String optionA = q.get("optionA").getAsString();
                                String optionB = q.get("optionB").getAsString();
                                String optionC = q.get("optionC").getAsString();
                                String optionD = q.get("optionD").getAsString();
                                String correctAnswer = q.get("correctAnswer").getAsString();

                                questions.add(new String[]{
                                        questionText,
                                        optionA,
                                        optionB,
                                        optionC,
                                        optionD,
                                        correctAnswer
                                });
                            }

                            // Persist generated quiz/questions for reuse in other features (e.g., AR deck selection)
                            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                            String savedQuizId = null;
                            if (userId != null) {
                                String quizId = IdUtil.generateId("quiz");
                                savedQuizId = quizId;

                                String cleanTitle = sourceModuleTitle != null ? sourceModuleTitle.trim() : "";
                                if (cleanTitle.isEmpty()) {
                                    cleanTitle = subject + " Module";
                                }

                                Quiz quiz = new Quiz(userId,
                                        cleanTitle + " Quiz",
                                        "AI generated quiz from a saved module",
                                        questions.size(),
                                        60.0,
                                        subject,
                                        3);
                                quiz.setQuizId(quizId);
                                quiz.setModuleId(moduleId);

                                for (int i = 0; i < questions.size(); i++) {
                                    String[] q = questions.get(i);
                                    Question qe = new Question(quizId, q[0], q[1], q[2], q[3], q[4], q[5], i + 1);
                                    qe.setQuestionId(IdUtil.generateId("question"));
                                    questionEntities.add(qe);
                                }

                                new QuizRepository(UploadModuleActivity.this).createQuiz(quiz, userId);
                                new QuestionRepository(UploadModuleActivity.this).createAllQuestions(questionEntities);
                            }

                            Intent intent = new Intent(UploadModuleActivity.this, QuizTakeActivity.class);
                            intent.putExtra(QuizTakeActivity.EXTRA_SUBJECT, subject);
                            if (savedQuizId != null) {
                                intent.putExtra(QuizTakeActivity.EXTRA_QUIZ_ID, savedQuizId);
                            }
                            intent.putParcelableArrayListExtra(QuizTakeActivity.EXTRA_QUESTIONS,
                                    toParcelableList(questions));
                            startActivity(intent);

                        } catch (Exception e) {
                            Toast.makeText(UploadModuleActivity.this,
                                    "Failed to parse quiz. Try again.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        setLoading(false, "");
                        String message = t != null && t.getMessage() != null ? t.getMessage() : "Unknown error";
                        Toast.makeText(UploadModuleActivity.this, "Network error: " + message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static String extractGeminiText(JsonObject body) {
        if (body == null) return null;
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
    }

    private ArrayList<android.os.Bundle> toParcelableList(ArrayList<String[]> questions) {
        ArrayList<android.os.Bundle> list = new ArrayList<>();
        for (String[] q : questions) {
            android.os.Bundle b = new android.os.Bundle();
            b.putString("question", q[0]);
            b.putString("optionA", q[1]);
            b.putString("optionB", q[2]);
            b.putString("optionC", q[3]);
            b.putString("optionD", q[4]);
            b.putString("correctAnswer", q[5]);
            list.add(b);
        }
        return list;
    }

    private void setLoading(boolean loading, String message) {
        binding.layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGenerate.setEnabled(!loading);
        binding.tvLoadingStatus.setText(message);
    }

    private String upsertStudyModule(String contentText, String subject, String sourceRefCandidate) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            return null;
        }

        String moduleId = providedModuleId != null && !providedModuleId.trim().isEmpty()
                ? providedModuleId
                : IdUtil.generateId("module");

        String moduleTitle = resolveModuleTitle(sourceRefCandidate);
        String sourceType = resolveModuleSourceType();
        String sourceRef = resolveModuleSourceRef(sourceRefCandidate);

        StudyModule module = new StudyModule(
                userId,
                moduleTitle,
                subject,
                subject,
                "Saved module for review and quiz practice.",
                contentText,
                sourceType,
                sourceRef
        );
        module.setModuleId(moduleId);

        studyModuleRepository.upsertStudyModule(module, userId);
        return moduleId;
    }

    private String resolveModuleTitle(String fallbackTitle) {
        if (providedModuleTitle != null && !providedModuleTitle.trim().isEmpty()) {
            return providedModuleTitle.trim();
        }
        if (fallbackTitle != null && !fallbackTitle.trim().isEmpty()) {
            return fallbackTitle.trim();
        }
        if (selectedFileUri != null) {
            String fileName = getFileName(selectedFileUri);
            if (fileName != null && !fileName.trim().isEmpty()) {
                return fileName.trim();
            }
        }
        return "Study Module";
    }

    private String resolveModuleSourceType() {
        if (providedModuleSourceType != null && !providedModuleSourceType.trim().isEmpty()) {
            return providedModuleSourceType.trim();
        }
        if (selectedFileUri != null) {
            return "UPLOADED_FILE";
        }
        if (providedModuleText != null && !providedModuleText.trim().isEmpty()) {
            return "READY_MADE";
        }
        return "MANUAL";
    }

    private String resolveModuleSourceRef(String fallbackRef) {
        if (providedModuleSourceRef != null && !providedModuleSourceRef.trim().isEmpty()) {
            return providedModuleSourceRef.trim();
        }
        if (fallbackRef != null && !fallbackRef.trim().isEmpty()) {
            return fallbackRef.trim();
        }
        if (selectedFileUri != null) {
            return selectedFileUri.toString();
        }
        return "";
    }

    private String normalizeForStorage(String text) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim()
                .replaceAll("\\n{3,}", "\n\n");
        return truncate(normalized, 20000);
    }

    private String toAiPromptText(String text) {
        if (text == null) return "";
        return truncate(text.replaceAll("\\s+", " "), 8000);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "Selected file";
    }
}
