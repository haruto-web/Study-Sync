package com.example.studysync_project.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.databinding.FragmentHomeBinding;
import com.example.studysync_project.ui.auth.LoginActivity;
import com.example.studysync_project.ui.profile.ProfileActivity;
import com.example.studysync_project.ui.quiz.UploadModuleActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = auth.getUid();
        if (userId == null) return;

        loadUserData();
        loadLiveStats();
        setupClickListeners();
    }

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    if (name == null) name = doc.getString("fullName");
                    binding.tvWelcome.setText("Welcome back, " + (name != null ? name : "Student") + "!");
                    if (email != null) binding.tvUserEmail.setText(email);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show());
    }

    private void loadLiveStats() {
        TaskRepository taskRepo = new TaskRepository(requireContext());
        TimerRepository timerRepo = new TimerRepository(requireContext());
        QuizAttemptRepository attemptRepo = new QuizAttemptRepository(requireContext());

        taskRepo.getActiveTaskCountForUser(userId).observe(getViewLifecycleOwner(), count -> {
            int c = count != null ? count : 0;
            binding.tvTasksSummary.setText(c + " pending task" + (c == 1 ? "" : "s"));
        });

        timerRepo.getTotalStudyMinutesForUser(userId).observe(getViewLifecycleOwner(), mins -> {
            int m = mins != null ? mins : 0;
            if (m >= 60) {
                binding.tvStudyTime.setText((m / 60) + "h " + (m % 60) + "m today");
            } else {
                binding.tvStudyTime.setText(m + " minutes today");
            }
        });

        attemptRepo.getAverageScoreForUser(userId).observe(getViewLifecycleOwner(), avg -> {
            if (avg != null && avg > 0) {
                binding.tvQuizScore.setText(String.format("Avg score: %.0f%%", avg));
            } else {
                binding.tvQuizScore.setText("No quizzes yet");
            }
        });
    }

    private void setupClickListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        binding.cardAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
        binding.btnStartQuiz.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), UploadModuleActivity.class)));
        binding.btnProgress.setOnClickListener(v ->
                startActivity(new Intent(requireContext(),
                        com.example.studysync_project.ui.progress.ProgressActivity.class)));
        binding.btnStudyTimer.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.studysync_project.MainActivity) {
                ((com.example.studysync_project.MainActivity) getActivity())
                        .navigateTo(com.example.studysync_project.R.id.timerFragment);
            }
        });
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
