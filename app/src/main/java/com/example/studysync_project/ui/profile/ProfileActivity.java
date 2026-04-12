package com.example.studysync_project.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.data.repository.UserRepository;
import com.example.studysync_project.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private UserRepository userRepository;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        userRepository = new UserRepository(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        loadProfile();
        loadStats();

        binding.btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        userRepository.getUserProfile(userId).observe(this, profile -> {
            if (profile != null) {
                binding.etName.setText(profile.getFullName());
                binding.etBio.setText(profile.getBio());
                bindProgress(profile);
            } else {
                binding.tvProgressIndex.setText("0/100");
                binding.tvProgressState.setText("Starting");
                binding.tvProgressFocus.setText("Build activity this week to unlock focus insights.");
                binding.tvProgressBadges.setText("No badges yet");
            }
        });
    }

    private void bindProgress(UserProfile profile) {
        int roundedIndex = (int) Math.round(profile.getProgressionIndex());
        binding.tvProgressIndex.setText(roundedIndex + "/100");

        String stateLabel = ProgressionRepository.formatStateLabel(profile.getProgressionState());
        binding.tvProgressState.setText(
                stateLabel + " • " + profile.getCurrentStreakDays() + " day streak"
        );

        int stateColorRes;
        if ("Improving".equals(stateLabel)) {
            stateColorRes = com.example.studysync_project.R.color.success;
        } else if ("Declining".equals(stateLabel)) {
            stateColorRes = com.example.studysync_project.R.color.warning;
        } else if ("Inactive".equals(stateLabel)) {
            stateColorRes = com.example.studysync_project.R.color.inactive;
        } else {
            stateColorRes = com.example.studysync_project.R.color.info;
        }
        binding.tvProgressState.setTextColor(ContextCompat.getColor(this, stateColorRes));

        String focus = profile.getFocusSubject() != null ? profile.getFocusSubject().trim() : "";
        String strongest = profile.getStrongestSubject() != null ? profile.getStrongestSubject().trim() : "";
        if (!focus.isEmpty() || !strongest.isEmpty()) {
            StringBuilder insight = new StringBuilder();
            if (!focus.isEmpty()) {
                insight.append("Focus next: ").append(focus);
            }
            if (!strongest.isEmpty()) {
                if (insight.length() > 0) insight.append(" • ");
                insight.append("Strongest: ").append(strongest);
            }
            binding.tvProgressFocus.setText(insight.toString());
        } else {
            binding.tvProgressFocus.setText("Build activity this week to unlock focus insights.");
        }

        String badgesCsv = profile.getUnlockedBadgesCsv() != null ? profile.getUnlockedBadgesCsv().trim() : "";
        if (badgesCsv.isEmpty()) {
            binding.tvProgressBadges.setText("No badges yet");
        } else {
            int badgeCount = badgesCsv.split(",").length;
            String lastBadge = profile.getLastUnlockedBadge() != null ? profile.getLastUnlockedBadge().trim() : "";
            if (lastBadge.isEmpty()) {
                binding.tvProgressBadges.setText("Badges unlocked: " + badgeCount);
            } else {
                binding.tvProgressBadges.setText("Badges: " + badgeCount + " • Latest: " + lastBadge.replace('_', ' '));
            }
        }
    }

    private void loadStats() {
        new QuizAttemptRepository(this).getTotalQuizAttemptsForUser(userId)
                .observe(this, count -> binding.tvQuizzesTaken.setText(String.valueOf(count != null ? count : 0)));

        new TaskRepository(this).getCompletedTaskCountForUser(userId)
                .observe(this, count -> binding.tvTasksDone.setText(String.valueOf(count != null ? count : 0)));

        new TimerRepository(this).getTotalStudyMinutesForUser(userId)
                .observe(this, mins -> {
                    int m = mins != null ? mins : 0;
                    binding.tvStudyHours.setText(m >= 60 ? (m / 60) + "h" : m + "m");
                });
    }

    private void saveProfile() {
        String name = binding.etName.getText() != null ? binding.etName.getText().toString().trim() : "";
        String bio = binding.etBio.getText() != null ? binding.etBio.getText().toString().trim() : "";

        if (name.isEmpty()) {
            binding.tilName.setError("Name is required");
            return;
        }
        binding.tilName.setError(null);

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        userRepository.updateNameAndBio(userId, email, name, bio);
        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
    }
}
