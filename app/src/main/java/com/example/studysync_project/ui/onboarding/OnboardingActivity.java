package com.example.studysync_project.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
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
import java.util.Map;

public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

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

        binding.btnContinue.setOnClickListener(v -> saveAndContinue());
    }

    private void saveAndContinue() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, com.example.studysync_project.ui.auth.LoginActivity.class));
            finish();
            return;
        }

        String userId = user.getUid();
        String gradeLevel = binding.actGradeLevel.getText() != null ? binding.actGradeLevel.getText().toString().trim() : "";
        String goal = binding.actGoal.getText() != null ? binding.actGoal.getText().toString().trim() : "";
        String subject = binding.actSubject.getText() != null ? binding.actSubject.getText().toString().trim() : "";
        String topicsCsv = binding.etTopics.getText() != null ? binding.etTopics.getText().toString().trim() : "";

        if (gradeLevel.isEmpty()) {
            Toast.makeText(this, "Please select your grade level", Toast.LENGTH_SHORT).show();
            return;
        }
        if (goal.isEmpty()) {
            Toast.makeText(this, "Please select your study goal", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subject.isEmpty()) {
            Toast.makeText(this, "Please select a subject", Toast.LENGTH_SHORT).show();
            return;
        }

        ConsentManager.setOnboardedV1(this, userId, true);
        ConsentManager.storeOnboarding(this, userId, gradeLevel, goal, subject, topicsCsv);

        boolean personalizationEnabled = ConsentManager.isPersonalizationEnabled(this, userId);
        if (personalizationEnabled && NetworkUtil.isNetworkAvailable(this)) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("gradeLevel", gradeLevel);
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

        // Limited mode or offline: store locally only.
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
