package com.example.studysync_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.studysync_project.ui.auth.LoginActivity;
import com.example.studysync_project.ui.auth.TermsAndConditionsActivity;
import com.example.studysync_project.ui.onboarding.OnboardingActivity;
import com.example.studysync_project.utils.ConsentManager;
import com.example.studysync_project.utils.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    public static final String EXTRA_SKIP_DELAY = "extra_skip_delay";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        long delayMs = getIntent().getBooleanExtra(EXTRA_SKIP_DELAY, false) ? 0 : 2000;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            boolean isLoggedIn = user != null && user.isEmailVerified();
            if (!isLoggedIn) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }

            String userId = user.getUid();

            // If offline, fall back to local flags.
            if (!NetworkUtil.isNetworkAvailable(this)) {
                routeWithLocalFallback(userId);
                return;
            }

            firestore.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        Long termsVersion = doc.getLong("termsVersion");
                        Boolean termsAccepted = doc.getBoolean("termsAccepted");
                        Boolean personalizationEnabled = doc.getBoolean("personalizationEnabled");

                        int version = termsVersion != null ? termsVersion.intValue() : 0;
                        boolean accepted = termsAccepted != null && termsAccepted;
                        boolean personalization = personalizationEnabled != null && personalizationEnabled;

                        // Cache locally for offline behavior.
                        ConsentManager.storeConsent(this, userId, version, accepted, personalization);

                        if (version < ConsentManager.TERMS_VERSION) {
                            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
                            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION, TermsAndConditionsActivity.DEST_MAIN);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        if (!ConsentManager.isOnboardedV1(this, userId)) {
                            startActivity(new Intent(this, OnboardingActivity.class));
                            finish();
                            return;
                        }

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Could not sync profile; using local settings.", Toast.LENGTH_SHORT).show();
                        routeWithLocalFallback(userId);
                    });

        }, delayMs);
    }

    private void routeWithLocalFallback(String userId) {
        int localVersion = ConsentManager.getStoredTermsVersion(this, userId);
        if (localVersion < ConsentManager.TERMS_VERSION) {
            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION, TermsAndConditionsActivity.DEST_MAIN);
            startActivity(intent);
            finish();
            return;
        }

        if (!ConsentManager.isOnboardedV1(this, userId)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
