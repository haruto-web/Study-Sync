package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        boolean termsAccepted = binding.cbTerms.isChecked();

        // Validation
        if (name.isEmpty()) {
            binding.etName.setError("Full name is required");
            return;
        }
        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            return;
        }
        if (!termsAccepted) {
            Toast.makeText(this, "Please accept the Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user account
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> {
                String uid = r.getUser().getUid();
                // Create profile using our UserProfile model
                UserProfile userProfile = new UserProfile(uid, email, name);
                
                db.collection("users").document(uid).set(userProfile)
                    .addOnSuccessListener(task -> {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .addOnFailureListener(e -> {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("already in use")) {
                    binding.etEmail.setError("Email is already registered");
                } else {
                    Toast.makeText(this, "Registration failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }
}
