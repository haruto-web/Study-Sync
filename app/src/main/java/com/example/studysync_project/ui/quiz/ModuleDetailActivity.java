package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.R;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.databinding.ActivityModuleDetailBinding;

import java.io.File;
import java.util.Locale;

/**
 * Displays module content for review and supports quiz generation from that module.
 */
public class ModuleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MODULE_ID = "extra_module_id";

    private ActivityModuleDetailBinding binding;
    private StudyModuleRepository studyModuleRepository;
    private StudyModule loadedModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityModuleDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        studyModuleRepository = new StudyModuleRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnBackToLibrary.setOnClickListener(v -> finish());
        binding.btnGenerateQuiz.setOnClickListener(v -> promptReadyForQuiz());
        binding.btnViewOriginalPdf.setOnClickListener(v -> openOriginalPdf());
        binding.btnDeleteModule.setOnClickListener(v -> confirmDeleteModule());

        String moduleId = getIntent().getStringExtra(EXTRA_MODULE_ID);
        if (moduleId == null || moduleId.trim().isEmpty()) {
            Toast.makeText(this, "Module not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        observeModule(moduleId);
    }

    private void observeModule(String moduleId) {
        studyModuleRepository.getStudyModuleById(moduleId).observe(this, module -> {
            loadedModule = module;
            if (module == null) {
                binding.tvTitle.setText("Loading module...");
                binding.tvMeta.setText("");
                binding.tvDescription.setText("Preparing module content for review.");
                binding.tvContent.setText("");
                binding.btnGenerateQuiz.setEnabled(false);
                binding.btnViewOriginalPdf.setVisibility(View.GONE);
                return;
            }

            binding.btnGenerateQuiz.setEnabled(module.isUnlocked());

            if (module.isUnlocked()
                    && StudyModule.PROGRESSION_NEW.equalsIgnoreCase(textOrEmpty(module.getProgressionState()))) {
                studyModuleRepository.markModuleAsStarted(module.getModuleId());
            }

            bindModule(module);
            updatePdfButtonVisibility(module);
        });
    }

    private void bindModule(StudyModule module) {
        String title = notBlank(module.getTitle()) ? module.getTitle() : "Untitled Module";
        String subject = notBlank(module.getSubject()) ? module.getSubject() : "General";
        String topic = notBlank(module.getTopic()) ? module.getTopic() : "Mixed Topics";

        binding.tvTitle.setText(title);
        binding.tvMeta.setText(subject + " • " + topic);

        String description = notBlank(module.getDescription())
                ? module.getDescription()
                : "Review the module below, then generate a quiz when you are ready.";
        if (!module.isUnlocked()) {
            description = "Locked for progression. Master the previous module in this subject to unlock this one.";
        }
        binding.tvDescription.setText(description);

        String content = notBlank(module.getContentText())
                ? module.getContentText()
                : "No module content available.";
        binding.tvContent.setText(content);
    }

    private void updatePdfButtonVisibility(StudyModule module) {
        binding.btnViewOriginalPdf.setVisibility(canViewOriginalPdf(module) ? View.VISIBLE : View.GONE);
    }

    private boolean canViewOriginalPdf(StudyModule module) {
        if (module == null) {
            return false;
        }
        String sourceType = textOrEmpty(module.getSourceType());
        String sourceRef = textOrEmpty(module.getSourceRef());

        if (!"UPLOADED_FILE".equalsIgnoreCase(sourceType) || sourceRef.isEmpty()) {
            return false;
        }

        return isPdfSourceRef(sourceRef);
    }

    private boolean isPdfSourceRef(String sourceRef) {
        try {
            Uri uri = Uri.parse(sourceRef);
            String scheme = uri.getScheme();

            // If no scheme is present, only allow when it points to an actual local file.
            if (scheme == null || scheme.trim().isEmpty()) {
                File file = new File(sourceRef);
                return file.exists() && sourceRef.toLowerCase(Locale.US).endsWith(".pdf");
            }

            if (!"content".equalsIgnoreCase(scheme) && !"file".equalsIgnoreCase(scheme)) {
                return false;
            }

            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && "application/pdf".equalsIgnoreCase(mimeType)) {
                return true;
            }

            String displayName = queryDisplayName(uri);
            if (displayName != null && displayName.toLowerCase(Locale.US).endsWith(".pdf")) {
                return true;
            }

            return sourceRef.toLowerCase(Locale.US).endsWith(".pdf");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String queryDisplayName(Uri uri) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) {
            return null;
        }

        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private void openOriginalPdf() {
        if (loadedModule == null) {
            Toast.makeText(this, R.string.pdf_viewer_open_error, Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceRef = textOrEmpty(loadedModule.getSourceRef());
        if (sourceRef.isEmpty()) {
            Toast.makeText(this, R.string.pdf_viewer_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.putExtra(PdfViewerActivity.EXTRA_PDF_URI, sourceRef);
        intent.putExtra(PdfViewerActivity.EXTRA_PDF_TITLE, loadedModule.getTitle());
        startActivity(intent);
    }

    private void confirmDeleteModule() {
        if (loadedModule == null || !notBlank(loadedModule.getModuleId())) {
            Toast.makeText(this, R.string.module_delete_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.module_delete_confirm_title)
                .setMessage(R.string.module_delete_confirm_message)
                .setPositiveButton(R.string.module_delete_confirm_action, (dialog, which) -> {
                    studyModuleRepository.deleteStudyModule(loadedModule.getModuleId());
                    Toast.makeText(this, R.string.module_deleted, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void generateQuizFromModule() {
        if (loadedModule == null) {
            Toast.makeText(this, "Module is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, UploadModuleActivity.class);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_ID, loadedModule.getModuleId());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TITLE, loadedModule.getTitle());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_SUBJECT, loadedModule.getSubject());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TEXT, loadedModule.getContentText());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_TYPE, loadedModule.getSourceType());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_REF, loadedModule.getSourceRef());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_QUESTION_COUNT, 10);
        startActivity(intent);
    }

    private void promptReadyForQuiz() {
        if (loadedModule == null) {
            Toast.makeText(this, "Module is still loading", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!loadedModule.isUnlocked()) {
            Toast.makeText(this,
                    "This module is locked. Master the previous module first.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.module_ready_prompt_title)
                .setMessage(R.string.module_ready_prompt_message)
                .setPositiveButton(R.string.module_ready_prompt_take_quiz, (dialog, which) ->
                        generateQuizFromModule())
                .setNegativeButton(R.string.module_ready_prompt_keep_reading, (dialog, which) ->
                        dialog.dismiss())
                .show();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String textOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }
}
