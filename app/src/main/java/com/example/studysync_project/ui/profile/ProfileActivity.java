package com.example.studysync_project.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.data.model.UserProfile;
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
            }
        });
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

        UserProfile profile = new UserProfile(userId,
                FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "", name);
        profile.setBio(bio);
        userRepository.saveUserProfile(profile);
        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
    }
}
