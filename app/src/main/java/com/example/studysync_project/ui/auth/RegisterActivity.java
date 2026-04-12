package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.R;
import com.example.studysync_project.SplashActivity;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.databinding.ActivityRegisterBinding;
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

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
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
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> registerUser());
        binding.tvLogin.setOnClickListener(v -> finish());
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
                    db.collection("users").document(uid).set(profile)
                        .addOnSuccessListener(task -> {
                            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
                            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION,
                                    TermsAndConditionsActivity.DEST_VERIFY_EMAIL);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    // Existing Google user — go straight to app
                    Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, SplashActivity.class);
                    intent.putExtra(SplashActivity.EXTRA_SKIP_DELAY, true);
                    startActivity(intent);
                    finish();
                }
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void registerUser() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (name.isEmpty()) { binding.etName.setError("Full name is required"); return; }
        if (email.isEmpty()) { binding.etEmail.setError("Email is required"); return; }
        if (password.isEmpty()) { binding.etPassword.setError("Password is required"); return; }
        if (password.length() < 6) { binding.etPassword.setError("Password must be at least 6 characters"); return; }
        if (!password.equals(confirmPassword)) { binding.etConfirmPassword.setError("Passwords do not match"); return; }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(r -> {
                String uid = r.getUser().getUid();
                UserProfile userProfile = new UserProfile(uid, email, name);
                r.getUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
                    r.getUser().sendEmailVerification();
                    db.collection("users").document(uid).set(userProfile)
                        .addOnSuccessListener(task -> {
                            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
                            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION,
                                    TermsAndConditionsActivity.DEST_VERIFY_EMAIL);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e ->
                            Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                });
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
