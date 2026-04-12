package com.example.studysync_project.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.databinding.ActivityOnboardingBinding;
import com.example.studysync_project.utils.ConsentManager;
import com.example.studysync_project.utils.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;
    private boolean isSaving;
    private Set<String> allowedGradeLevels;
    private Set<String> allowedShsStrands;
    private Set<String> allowedGoals;
    private Set<String> allowedSubjects;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ArrayAdapter<CharSequence> gradeAdapter = ArrayAdapter.createFromResource(
                this,
                com.example.studysync_project.R.array.grade_levels,
                android.R.layout.simple_list_item_1
        );
        binding.actGradeLevel.setAdapter(gradeAdapter);

        ArrayAdapter<CharSequence> strandAdapter = ArrayAdapter.createFromResource(
                this,
                com.example.studysync_project.R.array.shs_strands,
                android.R.layout.simple_list_item_1
        );
        binding.actStrand.setAdapter(strandAdapter);

        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(
                this,
                com.example.studysync_project.R.array.study_goals,
                android.R.layout.simple_list_item_1
        );
        binding.actGoal.setAdapter(goalAdapter);

        ArrayAdapter<CharSequence> subjectAdapter = ArrayAdapter.createFromResource(
                this,
                com.example.studysync_project.R.array.subjects,
                android.R.layout.simple_list_item_1
        );
        binding.actSubject.setAdapter(subjectAdapter);

        allowedGradeLevels = buildOptionSet(com.example.studysync_project.R.array.grade_levels);
        allowedShsStrands = buildOptionSet(com.example.studysync_project.R.array.shs_strands);
        allowedGoals = buildOptionSet(com.example.studysync_project.R.array.study_goals);
        allowedSubjects = buildOptionSet(com.example.studysync_project.R.array.subjects);

        binding.actGradeLevel.setOnItemClickListener((parent, view, position, id) -> {
            String selected = parent.getItemAtPosition(position).toString();
            boolean isShs = selected.equals("Grade 11") || selected.equals("Grade 12");
            binding.tilStrand.setVisibility(isShs ? View.VISIBLE : View.GONE);
            if (!isShs) binding.actStrand.setText("", false);
        });

        binding.btnContinue.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {
        if (isSaving) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, com.example.studysync_project.ui.auth.LoginActivity.class));
            finish();
            return;
        }

        String userId = user.getUid();
        String gradeLevel = binding.actGradeLevel.getText() != null ? binding.actGradeLevel.getText().toString().trim() : "";
        String strand = binding.actStrand.getText() != null ? binding.actStrand.getText().toString().trim() : "";
        String goal = binding.actGoal.getText() != null ? binding.actGoal.getText().toString().trim() : "";
        String subject = binding.actSubject.getText() != null ? binding.actSubject.getText().toString().trim() : "";
        String topicsCsv = binding.etTopics.getText() != null ? binding.etTopics.getText().toString().trim() : "";

        clearInputErrors();

        if (gradeLevel.isEmpty()) {
            binding.actGradeLevel.setError("Please select your grade level");
            binding.actGradeLevel.requestFocus();
            return;
        }
        if (!isAllowedOption(gradeLevel, allowedGradeLevels)) {
            binding.actGradeLevel.setError("Choose a grade level from the suggestions");
            binding.actGradeLevel.requestFocus();
            return;
        }
        boolean isShs = gradeLevel.equals("Grade 11") || gradeLevel.equals("Grade 12");
        if (isShs && strand.isEmpty()) {
            binding.actStrand.setError("Please select your SHS strand");
            binding.actStrand.requestFocus();
            return;
        }
        if (isShs && !isAllowedOption(strand, allowedShsStrands)) {
            binding.actStrand.setError("Choose a strand from the suggestions");
            binding.actStrand.requestFocus();
            return;
        }
        if (goal.isEmpty()) {
            binding.actGoal.setError("Please select your study goal");
            binding.actGoal.requestFocus();
            return;
        }
        if (!isAllowedOption(goal, allowedGoals)) {
            binding.actGoal.setError("Choose a goal from the suggestions");
            binding.actGoal.requestFocus();
            return;
        }
        if (subject.isEmpty()) {
            binding.actSubject.setError("Please select a subject");
            binding.actSubject.requestFocus();
            return;
        }
        if (!isAllowedOption(subject, allowedSubjects)) {
            binding.actSubject.setError("Choose a subject from the suggestions");
            binding.actSubject.requestFocus();
            return;
        }
        if (topicsCsv.length() > 160) {
            binding.etTopics.setError("Keep topics under 160 characters");
            binding.etTopics.requestFocus();
            return;
        }

        topicsCsv = normalizeTopicsCsv(topicsCsv);

        setSavingState(true);

        ConsentManager.setOnboardedV1(this, userId, true);
        ConsentManager.storeOnboarding(this, userId, gradeLevel, strand, goal, subject, topicsCsv);

        boolean personalizationEnabled = ConsentManager.isPersonalizationEnabled(this, userId);
        if (personalizationEnabled && NetworkUtil.isNetworkAvailable(this)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("gradeLevel", gradeLevel);
            updates.put("strand", strand);
            updates.put("goal", goal);
            updates.put("subjectsCsv", subject);
            updates.put("topicsOfInterestCsv", topicsCsv);
            updates.put("updatedAt", System.currentTimeMillis());

            firestore.collection("users").document(userId)
                    .update(updates)
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Saved locally; will sync when online.", Toast.LENGTH_SHORT).show();
                        goToMain();
                    })
                    .addOnSuccessListener(unused -> goToMain());
            return;
        }

        goToMain();
    }

    private void clearInputErrors() {
        binding.actGradeLevel.setError(null);
        binding.actStrand.setError(null);
        binding.actGoal.setError(null);
        binding.actSubject.setError(null);
        binding.etTopics.setError(null);
    }

    private Set<String> buildOptionSet(int arrayResId) {
        Set<String> options = new HashSet<>();
        String[] values = getResources().getStringArray(arrayResId);
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    options.add(trimmed.toLowerCase());
                }
            }
        }
        return options;
    }

    private boolean isAllowedOption(String value, Set<String> allowedOptions) {
        if (value == null) {
            return false;
        }
        return allowedOptions.contains(value.trim().toLowerCase());
    }

    private String normalizeTopicsCsv(String rawTopics) {
        if (rawTopics == null || rawTopics.trim().isEmpty()) {
            return "";
        }

        String[] pieces = rawTopics.split(",");
        StringBuilder output = new StringBuilder();
        int added = 0;
        for (String piece : pieces) {
            if (piece == null) {
                continue;
            }
            String topic = piece.trim();
            if (topic.isEmpty()) {
                continue;
            }
            if (topic.length() > 24) {
                topic = topic.substring(0, 24).trim();
            }
            if (topic.isEmpty()) {
                continue;
            }
            if (added > 0) {
                output.append(", ");
            }
            output.append(topic);
            added++;
            if (added >= 8) {
                break;
            }
        }
        return output.toString();
    }

    private void setSavingState(boolean saving) {
        isSaving = saving;
        binding.btnContinue.setEnabled(!saving);
        binding.actGradeLevel.setEnabled(!saving);
        binding.actStrand.setEnabled(!saving);
        binding.actGoal.setEnabled(!saving);
        binding.actSubject.setEnabled(!saving);
        binding.etTopics.setEnabled(!saving);
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
