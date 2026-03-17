package com.example.studysync_project.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.databinding.FragmentQuizBinding;
import com.example.studysync_project.utils.IdUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class QuizFragment extends Fragment implements QuizAdapter.OnQuizClickListener {

    private FragmentQuizBinding binding;
    private QuizViewModel viewModel;
    private QuizAdapter adapter;
    private FirebaseAuth auth;
    private String userId;

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
        binding.rvQuizzes.setAdapter(adapter);

        // Observe quizzes
        viewModel.getAllQuizzesForUser(userId).observe(getViewLifecycleOwner(), quizzes -> {
            if (quizzes != null && !quizzes.isEmpty()) {
                adapter.submitList(quizzes);
                binding.emptyState.setVisibility(View.GONE);
                binding.rvQuizzes.setVisibility(View.VISIBLE);
            } else {
                binding.rvQuizzes.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
            }
        });

        // FAB click listener
        binding.fabAddQuiz.setOnClickListener(v -> createSampleQuiz());
    }

    /**
     * Create a sample quiz for demo purposes
     * In production, this would open a dialog/activity for creating quizzes
     */
    private void createSampleQuiz() {
        Quiz quiz = new Quiz(
            userId,
            "Sample Quiz: Math Basics",
            "Test your knowledge of basic math concepts",
            5,
            60.0,
            "Mathematics",
            2
        );
        quiz.setQuizId(IdUtil.generateId("quiz"));

        viewModel.createQuiz(quiz, userId);
        Toast.makeText(requireContext(), "Quiz created!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onQuizClick(Quiz quiz) {
        // Start quiz detail activity
        Toast.makeText(requireContext(), "Opening: " + quiz.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Start QuizDetailActivity with quiz ID
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

