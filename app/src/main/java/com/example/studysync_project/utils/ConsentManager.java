package com.example.studysync_project.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Centralizes consent/onboarding flags.
 *
 * Source of truth should remain Firestore, but these preferences provide fast gating and
 * offline-friendly behavior.
 */
public final class ConsentManager {

    private ConsentManager() {}

    public static final int TERMS_VERSION = 1;

    private static final String PREFS_NAME = "studysync_consent";

    private static String key(@NonNull String prefix, @NonNull String userId) {
        return prefix + "_" + userId;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getStoredTermsVersion(Context context, @NonNull String userId) {
        return prefs(context).getInt(key("terms_version", userId), 0);
    }

    public static boolean getStoredTermsAccepted(Context context, @NonNull String userId) {
        return prefs(context).getBoolean(key("terms_accepted", userId), false);
    }

    public static boolean isPersonalizationEnabled(Context context, @NonNull String userId) {
        return prefs(context).getBoolean(key("personalization_enabled", userId), false);
    }

    public static void storeConsent(
            Context context,
            @NonNull String userId,
            int termsVersion,
            boolean termsAccepted,
            boolean personalizationEnabled
    ) {
        prefs(context).edit()
                .putInt(key("terms_version", userId), termsVersion)
                .putBoolean(key("terms_accepted", userId), termsAccepted)
                .putBoolean(key("personalization_enabled", userId), personalizationEnabled)
                .apply();
    }

    public static boolean isOnboardedV1(Context context, @NonNull String userId) {
        return prefs(context).getBoolean(key("onboarded_v1", userId), false);
    }

    public static void setOnboardedV1(Context context, @NonNull String userId, boolean onboarded) {
        prefs(context).edit()
                .putBoolean(key("onboarded_v1", userId), onboarded)
                .apply();
    }

    public static void storeOnboarding(
            Context context,
            @NonNull String userId,
            String gradeLevel,
            String goal,
            String subject,
            String topicsCsv
    ) {
        prefs(context).edit()
                .putString(key("grade_level", userId), gradeLevel)
                .putString(key("goal", userId), goal)
                .putString(key("subject", userId), subject)
                .putString(key("topics_csv", userId), topicsCsv)
                .apply();
    }

    public static String getStoredGradeLevel(Context context, @NonNull String userId) {
        return prefs(context).getString(key("grade_level", userId), null);
    }

    public static String getStoredSubject(Context context, @NonNull String userId) {
        return prefs(context).getString(key("subject", userId), null);
    }

    public static String getStoredTopicsCsv(Context context, @NonNull String userId) {
        return prefs(context).getString(key("topics_csv", userId), null);
    }
}
