package com.example.studysync_project.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.data.repository.UserRepository;
import com.example.studysync_project.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private UserRepository userRepository;
    private String userId;
    private String pendingImageUrl;

    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) encodeImageToBase64(uri);
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) { finish(); return; }

        userRepository = new UserRepository(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.cardAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        binding.btnSave.setOnClickListener(v -> saveProfile());

        loadProfile();
        loadStats();
    }

    private void loadProfile() {
        userRepository.getUserProfile(userId).observe(this, profile -> {
            if (profile == null) {
                resetProgressCard();
                return;
            }

            binding.etName.setText(profile.getFullName());
            binding.etUsername.setText(profile.getUsername());
            binding.etAge.setText(profile.getAge() > 0 ? String.valueOf(profile.getAge()) : "");
            binding.etGradeLevel.setText(profile.getGradeLevel());
            binding.etStrand.setText(profile.getStrand());
            binding.etInterests.setText(profile.getTopicsOfInterestCsv());

            if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) {
                binding.ivAvatar.clearColorFilter();
                String imageData = profile.getProfileImageUrl();
                if (imageData.startsWith("data:image")) {
                    // Base64 stored in Firestore
                    String base64 = imageData.substring(imageData.indexOf(",") + 1);
                    byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
                    Glide.with(this).load(bytes).circleCrop()
                            .placeholder(R.drawable.ic_account_circle).into(binding.ivAvatar);
                } else {
                    Glide.with(this).load(imageData).circleCrop()
                            .placeholder(R.drawable.ic_account_circle).into(binding.ivAvatar);
                }
            }

            bindProgress(profile);
        });
    }

    private void bindProgress(UserProfile profile) {
        int roundedIndex = (int) Math.round(profile.getProgressionIndex());
        binding.tvProgressIndex.setText(roundedIndex + "/100");

        String stateLabel = ProgressionRepository.formatStateLabel(profile.getProgressionState());
        binding.tvProgressState.setText(stateLabel + " • " + profile.getCurrentStreakDays() + " day streak");

        int stateColorRes;
        if ("Improving".equals(stateLabel)) stateColorRes = R.color.success;
        else if ("Declining".equals(stateLabel)) stateColorRes = R.color.warning;
        else if ("Inactive".equals(stateLabel)) stateColorRes = R.color.inactive;
        else stateColorRes = R.color.info;
        binding.tvProgressState.setTextColor(ContextCompat.getColor(this, stateColorRes));

        String focus = profile.getFocusSubject() != null ? profile.getFocusSubject().trim() : "";
        String strongest = profile.getStrongestSubject() != null ? profile.getStrongestSubject().trim() : "";
        if (!focus.isEmpty() || !strongest.isEmpty()) {
            StringBuilder insight = new StringBuilder();
            if (!focus.isEmpty()) insight.append("Focus next: ").append(focus);
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

    private void resetProgressCard() {
        binding.tvProgressIndex.setText("0/100");
        binding.tvProgressState.setText("Starting");
        binding.tvProgressFocus.setText("Build activity this week to unlock focus insights.");
        binding.tvProgressBadges.setText("No badges yet");
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

    private void encodeImageToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Resize to max 256x256 to keep Firestore document size small
            int maxSize = 256;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min((float) maxSize / width, (float) maxSize / height);
            bitmap = Bitmap.createScaledBitmap(bitmap,
                    (int) (width * scale), (int) (height * scale), true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            pendingImageUrl = "data:image/jpeg;base64,"
                    + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

            binding.ivAvatar.clearColorFilter();
            Glide.with(this).load(uri).circleCrop().into(binding.ivAvatar);
            Toast.makeText(this, "Photo ready — tap Save to apply.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String name = text(binding.etName);
        String username = text(binding.etUsername);
        String ageStr = text(binding.etAge);
        String gradeLevel = text(binding.etGradeLevel);
        String strand = text(binding.etStrand);
        String interests = text(binding.etInterests);

        if (name.isEmpty()) {
            binding.tilName.setError("Name is required");
            return;
        }
        binding.tilName.setError(null);

        int age = 0;
        if (!ageStr.isEmpty()) {
            try {
                age = Integer.parseInt(ageStr);
            } catch (NumberFormatException e) {
                binding.tilAge.setError("Enter a valid age");
                return;
            }
        }
        binding.tilAge.setError(null);

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";

        userRepository.updateProfileFields(userId, email, name, username, age,
            gradeLevel, strand, interests, pendingImageUrl);

        Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
    }

    private String text(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
