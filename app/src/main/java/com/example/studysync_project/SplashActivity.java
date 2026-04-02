package com.example.studysync_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
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
                routeWithLocalFallback(userId, false);
                return;
            }

            firestore.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        Long termsVersion = doc.getLong("termsVersion");
                        Boolean termsAccepted = doc.getBoolean("termsAccepted");
                        Boolean personalizationEnabled = doc.getBoolean("personalizationEnabled");
                        String gradeLevel = doc.getString("gradeLevel");
                        String goal = doc.getString("goal");
                        String subjectsCsv = doc.getString("subjectsCsv");
                        String topicsCsv = doc.getString("topicsOfInterestCsv");

                        int version = termsVersion != null ? termsVersion.intValue() : 0;
                        boolean accepted = termsAccepted != null && termsAccepted;
                        boolean personalization = personalizationEnabled != null && personalizationEnabled;

                        // Cache locally for offline behavior.
                        ConsentManager.storeConsent(this, userId, version, accepted, personalization);

                        // Keep local onboarding cache in sync with profile completeness from Firestore.
                        boolean hasRequiredFirestoreOnboardingData = hasRequiredOnboardingData(gradeLevel, goal, subjectsCsv);
                        if (hasRequiredFirestoreOnboardingData) {
                            ConsentManager.storeOnboarding(this, userId, gradeLevel, goal, subjectsCsv, topicsCsv);
                        }

                        String localGradeLevel = ConsentManager.getStoredGradeLevel(this, userId);
                        String localGoal = ConsentManager.getStoredGoal(this, userId);
                        String localSubject = ConsentManager.getStoredSubject(this, userId);

                        boolean shouldShowOnboarding = shouldShowOnboardingForExistingUser(
                            version,
                            gradeLevel,
                            goal,
                            subjectsCsv,
                            localGradeLevel,
                            localGoal,
                            localSubject
                        );

                        boolean hasRequiredLocalOnboardingData = hasRequiredOnboardingData(
                            localGradeLevel,
                            localGoal,
                            localSubject
                        );
                        boolean isOnboarded = hasRequiredFirestoreOnboardingData || hasRequiredLocalOnboardingData;
                        ConsentManager.setOnboardedV1(this, userId, isOnboarded);

                        if (version < ConsentManager.TERMS_VERSION) {
                            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
                            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION, TermsAndConditionsActivity.DEST_MAIN);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        if (shouldShowOnboarding) {
                            startActivity(new Intent(this, OnboardingActivity.class));
                            finish();
                            return;
                        }

                        startMain();
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        Toast.makeText(this, "Could not sync profile; using local settings.", Toast.LENGTH_SHORT).show();
                        routeWithLocalFallback(userId, true);
                    });

        }, delayMs);
    }

    private void routeWithLocalFallback(String userId, boolean fromSyncFailure) {
        int localVersion = ConsentManager.getStoredTermsVersion(this, userId);
        if (localVersion < ConsentManager.TERMS_VERSION) {
            Intent intent = new Intent(this, TermsAndConditionsActivity.class);
            intent.putExtra(TermsAndConditionsActivity.EXTRA_DESTINATION, TermsAndConditionsActivity.DEST_MAIN);
            startActivity(intent);
            finish();
            return;
        }

        if (!ConsentManager.isOnboardedV1(this, userId)) {
            String message = fromSyncFailure
                    ? "Continuing to main while setup sync is unavailable."
                    : "Offline mode: continuing to main. You can finish setup later.";
            startMainWithDeferredSetupMessage(message);
            return;
        }

        startMain();
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void startMainWithDeferredSetupMessage(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DEFERRED_SETUP_MESSAGE, message);
        startActivity(intent);
        finish();
    }

    @VisibleForTesting
    static boolean shouldShowOnboardingForExistingUser(
            int termsVersion,
            String firestoreGradeLevel,
            String firestoreGoal,
            String firestoreSubjectsCsv,
            String localGradeLevel,
            String localGoal,
            String localSubject
    ) {
        if (termsVersion < ConsentManager.TERMS_VERSION) {
            return false;
        }
        return !isOnboardingComplete(
                firestoreGradeLevel,
                firestoreGoal,
                firestoreSubjectsCsv,
                localGradeLevel,
                localGoal,
                localSubject
        );
    }

    @VisibleForTesting
    static boolean isOnboardingComplete(
            String firestoreGradeLevel,
            String firestoreGoal,
            String firestoreSubjectsCsv,
            String localGradeLevel,
            String localGoal,
            String localSubject
    ) {
        boolean hasRequiredFirestoreOnboardingData = hasRequiredOnboardingData(
                firestoreGradeLevel,
                firestoreGoal,
                firestoreSubjectsCsv
        );
        boolean hasRequiredLocalOnboardingData = hasRequiredOnboardingData(
                localGradeLevel,
                localGoal,
                localSubject
        );
        return hasRequiredFirestoreOnboardingData || hasRequiredLocalOnboardingData;
    }

    @VisibleForTesting
    static boolean hasRequiredOnboardingData(String gradeLevel, String goal, String subjectsCsv) {
        return hasText(gradeLevel) && hasText(goal) && hasText(subjectsCsv);
    }

    @VisibleForTesting
    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
