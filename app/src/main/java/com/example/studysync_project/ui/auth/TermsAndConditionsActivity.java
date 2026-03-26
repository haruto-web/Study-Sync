package com.example.studysync_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.databinding.ActivityTermsAndConditionsBinding;
import com.example.studysync_project.utils.ConsentManager;
import com.example.studysync_project.utils.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TermsAndConditionsActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINATION = "extra_destination";
    public static final String DEST_MAIN = "main";
    public static final String DEST_VERIFY_EMAIL = "verify_email";

    private ActivityTermsAndConditionsBinding binding;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTermsAndConditionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAccept.setOnClickListener(v -> saveDecision(true));
        binding.btnDecline.setOnClickListener(v -> saveDecision(false));
    }

    private void saveDecision(boolean accepted) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = user.getUid();
        int termsVersion = ConsentManager.TERMS_VERSION;
        boolean personalizationEnabled = accepted;

        ConsentManager.storeConsent(this, userId, termsVersion, accepted, personalizationEnabled);

        Map<String, Object> updates = new HashMap<>();
        updates.put("termsAccepted", accepted);
        updates.put("personalizationEnabled", personalizationEnabled);
        updates.put("termsVersion", termsVersion);
        updates.put("termsAcceptedAt", accepted ? System.currentTimeMillis() : 0L);
        updates.put("updatedAt", System.currentTimeMillis());

        // Fire-and-forget: if offline, we still allow the user to proceed in the chosen mode.
        if (NetworkUtil.isNetworkAvailable(this)) {
            firestore.collection("users").document(userId)
                    .update(updates)
                    .addOnFailureListener(e -> {
                        // Non-blocking: user can continue; sync will happen later.
                        e.printStackTrace();
                        Toast.makeText(this, "Saved locally; will sync when online.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener(unused -> routeAfterDecision());
        } else {
            Toast.makeText(this, "Offline: saved locally.", Toast.LENGTH_SHORT).show();
            routeAfterDecision();
        }
    }

    private void routeAfterDecision() {
        String destination = getIntent().getStringExtra(EXTRA_DESTINATION);
        if (DEST_VERIFY_EMAIL.equals(destination)) {
            startActivity(new Intent(this, VerifyEmailActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
