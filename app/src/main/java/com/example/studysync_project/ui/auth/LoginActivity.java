package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.studysync_project.MainActivity;
import com.example.studysync_project.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if user is already logged in
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validation
        if (email.isEmpty()) {
            binding.etEmail.setError("Email is required");
            return;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Password is required");
            return;
        }

        // Sign in
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> {
                Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("user")) {
                    binding.etEmail.setError("User not found");
                } else if (errorMsg != null && errorMsg.contains("password")) {
                    binding.etPassword.setError("Incorrect password");
                } else {
                    Toast.makeText(this, "Sign in failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
            return;
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(task ->
                Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
