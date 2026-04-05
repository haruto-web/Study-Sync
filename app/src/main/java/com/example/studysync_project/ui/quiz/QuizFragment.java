package com.example.studysync_project.ui.quiz;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.databinding.FragmentQuizBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;

public class QuizFragment extends Fragment implements
        QuizAdapter.OnQuizClickListener,
        StudyModuleAdapter.OnStudyModuleClickListener {

    private FragmentQuizBinding binding;
    private QuizViewModel viewModel;
    private QuizAdapter adapter;
    private StudyModuleAdapter studyModuleAdapter;
    private FirebaseAuth auth;
    private String userId;
    private boolean hasQuizzes;
    private boolean hasModules;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this, new androidx.lifecycle.ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new QuizViewModel(requireContext());
            }
        }).get(QuizViewModel.class);

        // Setup RecyclerView
        adapter = new QuizAdapter(this);
        studyModuleAdapter = new StudyModuleAdapter(this);
        binding.rvQuizzes.setAdapter(adapter);
        binding.rvModules.setAdapter(studyModuleAdapter);

        viewModel.syncStudyModules(userId);
        viewModel.syncQuizzes(userId);

        // Observe modules
        viewModel.getAllStudyModulesForUser(userId).observe(getViewLifecycleOwner(), modules -> {
            hasModules = modules != null && !modules.isEmpty();
            if (hasModules) {
                studyModuleAdapter.submitList(modules);
                binding.rvModules.setVisibility(View.VISIBLE);
                binding.tvModulesEmpty.setVisibility(View.GONE);
            } else {
                studyModuleAdapter.submitList(Collections.emptyList());
                binding.rvModules.setVisibility(View.GONE);
                binding.tvModulesEmpty.setVisibility(View.VISIBLE);
            }
            updateOverallEmptyState();
        });

        // Observe quizzes
        viewModel.getAllQuizzesForUser(userId).observe(getViewLifecycleOwner(), quizzes -> {
            hasQuizzes = quizzes != null && !quizzes.isEmpty();
            if (hasQuizzes) {
                adapter.submitList(quizzes);
                binding.rvQuizzes.setVisibility(View.VISIBLE);
                binding.tvQuizzesEmpty.setVisibility(View.GONE);
            } else {
                adapter.submitList(Collections.emptyList());
                binding.rvQuizzes.setVisibility(View.GONE);
                binding.tvQuizzesEmpty.setVisibility(View.VISIBLE);
            }
            updateOverallEmptyState();
        });

        // FAB opens upload module flow
        binding.fabAddQuiz.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UploadModuleActivity.class)));

        binding.btnOpenGeminiModuleGenerator.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), GenerateModuleActivity.class)));
    }

    private void updateOverallEmptyState() {
        if (binding == null) return;
        binding.emptyState.setVisibility(hasModules || hasQuizzes ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onStudyModuleClick(StudyModule module) {
        if (module == null || module.getModuleId() == null || module.getModuleId().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Module not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), ModuleDetailActivity.class);
        intent.putExtra(ModuleDetailActivity.EXTRA_MODULE_ID, module.getModuleId());
        startActivity(intent);
    }

    @Override
    public void onGenerateQuizFromModule(StudyModule module) {
        if (module == null || module.getContentText() == null || module.getContentText().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Module has no content to generate a quiz from", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), UploadModuleActivity.class);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_ID, module.getModuleId());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TITLE, module.getTitle());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_SUBJECT, module.getSubject());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TEXT, module.getContentText());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_TYPE, module.getSourceType());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_REF, module.getSourceRef());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_QUESTION_COUNT, 10);
        startActivity(intent);
    }

    @Override
    public void onQuizClick(Quiz quiz) {
        Intent intent = new Intent(requireContext(), QuizDetailActivity.class);
        intent.putExtra(QuizDetailActivity.EXTRA_QUIZ_ID, quiz.getQuizId());
        startActivity(intent);
    }

    @Override
    public void onQuizDelete(Quiz quiz) {
        viewModel.deleteQuiz(quiz.getQuizId());
        Toast.makeText(requireContext(), "Quiz deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

