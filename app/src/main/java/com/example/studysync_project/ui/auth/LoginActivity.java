package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.R;
import com.example.studysync_project.SplashActivity;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                        .getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if user is already logged in and verified
        FirebaseUser current = auth.getCurrentUser();
        if (current != null && current.isEmailVerified()) {
            goToSplash();
            return;
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
        binding.tvForgotPassword.setOnClickListener(v -> resetPassword());
        binding.btnGoogleSignIn.setOnClickListener(v ->
            googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
            .addOnSuccessListener(result -> {
                FirebaseUser user = result.getUser();
                boolean isNewUser = result.getAdditionalUserInfo() != null
                        && result.getAdditionalUserInfo().isNewUser();
                if (isNewUser) {
                    String uid = user.getUid();
                    String name = account.getDisplayName() != null ? account.getDisplayName() : "";
                    UserProfile profile = new UserProfile(uid, user.getEmail(), name);
                    db.collection("users").document(uid).set(profile);
                }
                Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show();
                goToSplash();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        binding.etEmail.setError(null);
        binding.etPassword.setError(null);

        if (email.isEmpty()) { binding.etEmail.setError("Email is required"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            return;
        }
        if (password.isEmpty()) { binding.etPassword.setError("Password is required"); return; }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> {
                if (r.getUser() == null) {
                    Toast.makeText(this, "Sign in failed: missing user profile", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!r.getUser().isEmailVerified()) {
                    Toast.makeText(this, "Please verify your email before signing in.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, VerifyEmailActivity.class));
                    return;
                }
                Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                goToSplash();
            })
            .addOnFailureListener(e -> {
                String errorMsg = e.getMessage();
                String normalizedMsg = errorMsg != null ? errorMsg.toLowerCase(Locale.US) : "";
                if (normalizedMsg.contains("user") || normalizedMsg.contains("no user record")) {
                    binding.etEmail.setError("User not found");
                } else if (normalizedMsg.contains("password") || normalizedMsg.contains("credential")) {
                    binding.etPassword.setError("Incorrect password");
                } else {
                    Toast.makeText(this, "Sign in failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        binding.etEmail.setError(null);
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Enter a valid email address");
            return;
        }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener(task ->
                Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void goToSplash() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra(SplashActivity.EXTRA_SKIP_DELAY, true);
        startActivity(intent);
        finish();
    }
}
