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

import com.example.studysync_project.databinding.ActivityUploadModuleBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.TextExtractorUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadModuleActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUploadModuleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.cardPickFile.setOnClickListener(v ->
                filePicker.launch(new String[]{"application/pdf", "text/plain",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"}));

        binding.btnGenerate.setOnClickListener(v -> startGeneration());
    }

    private void startGeneration() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
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

        setLoading(true, "Extracting text from file...");

        int finalCount = questionCount;
        String finalSubject = subject;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String text = TextExtractorUtil.extract(this, selectedFileUri);

            runOnUiThread(() -> {
                if (text == null || text.trim().isEmpty()) {
                    setLoading(false, "");
                    Toast.makeText(this, "Could not read file content. Try a TXT or PDF file.", Toast.LENGTH_LONG).show();
                    return;
                }

                extractedText = text.length() > 8000 ? text.substring(0, 8000) : text;
                setLoading(true, "AI is analyzing your module...");

                GeminiApiClient.generateQuiz(extractedText, finalSubject, finalCount)
                        .enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                setLoading(false, "");
                                if (!response.isSuccessful() || response.body() == null) {
                                    Toast.makeText(UploadModuleActivity.this,
                                            "AI request failed. Check your API key.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                try {
                                    String rawText = response.body()
                                            .getAsJsonArray("candidates").get(0).getAsJsonObject()
                                            .getAsJsonObject("content")
                                            .getAsJsonArray("parts").get(0).getAsJsonObject()
                                            .get("text").getAsString();

                                    rawText = rawText.replaceAll("```json", "").replaceAll("```", "").trim();

                                    JsonArray questionsJson = JsonParser.parseString(rawText).getAsJsonArray();
                                    ArrayList<String[]> questions = new ArrayList<>();
                                    for (JsonElement el : questionsJson) {
                                        JsonObject q = el.getAsJsonObject();
                                        questions.add(new String[]{
                                                q.get("question").getAsString(),
                                                q.get("optionA").getAsString(),
                                                q.get("optionB").getAsString(),
                                                q.get("optionC").getAsString(),
                                                q.get("optionD").getAsString(),
                                                q.get("correctAnswer").getAsString()
                                        });
                                    }

                                    Intent intent = new Intent(UploadModuleActivity.this, QuizTakeActivity.class);
                                    intent.putExtra(QuizTakeActivity.EXTRA_SUBJECT, finalSubject);
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
                                Toast.makeText(UploadModuleActivity.this,
                                        "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            });
        });
        executor.shutdown();
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
