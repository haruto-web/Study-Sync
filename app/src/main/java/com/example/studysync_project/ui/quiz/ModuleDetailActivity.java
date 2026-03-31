package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.databinding.ActivityModuleDetailBinding;

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
        binding.btnGenerateQuiz.setOnClickListener(v -> generateQuizFromModule());

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
                return;
            }

            binding.btnGenerateQuiz.setEnabled(true);
            bindModule(module);
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
        binding.tvDescription.setText(description);

        String content = notBlank(module.getContentText())
                ? module.getContentText()
                : "No module content available.";
        binding.tvContent.setText(content);
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

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
