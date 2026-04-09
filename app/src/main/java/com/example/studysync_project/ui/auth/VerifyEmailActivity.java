package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.databinding.ActivityVerifyEmailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private ActivityVerifyEmailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            binding.tvEmailSent.setText(
                    "A verification link has been sent to " + user.getEmail() +
                            ". Please check your inbox and click the link to continue."
            );
        }

        binding.btnCheckVerified.setOnClickListener(v -> checkVerification());
        binding.btnResend.setOnClickListener(v -> resendEmail());
        binding.tvBackLogin.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void checkVerification() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        user.reload().addOnSuccessListener(unused -> {
            if (user.isEmailVerified()) {
                Toast.makeText(this, "Email verified! Let's set up your profile.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, com.example.studysync_project.ui.onboarding.OnboardingActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        user.sendEmailVerification()
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Verification email resent.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to resend: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
