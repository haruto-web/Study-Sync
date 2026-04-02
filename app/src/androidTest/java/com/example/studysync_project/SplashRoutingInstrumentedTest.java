package com.example.studysync_project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.studysync_project.utils.ConsentManager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SplashRoutingInstrumentedTest {

    @Test
    public void existingUserWithFirestoreRequiredFields_doesNotShowOnboarding() {
        boolean shouldShowOnboarding = SplashActivity.shouldShowOnboardingForExistingUser(
                ConsentManager.TERMS_VERSION,
                "Grade 10",
                "Exam prep",
                "Mathematics",
                null,
                null,
                null
        );

        assertFalse(shouldShowOnboarding);
    }

    @Test
    public void existingUserWithoutRequiredFields_showsOnboarding() {
        boolean shouldShowOnboarding = SplashActivity.shouldShowOnboardingForExistingUser(
                ConsentManager.TERMS_VERSION,
                "",
                " ",
                null,
                null,
                "",
                ""
        );

        assertTrue(shouldShowOnboarding);
    }

    @Test
    public void existingUserWithLocalRequiredFields_doesNotShowOnboarding() {
        boolean shouldShowOnboarding = SplashActivity.shouldShowOnboardingForExistingUser(
                ConsentManager.TERMS_VERSION,
                null,
                null,
                null,
                "Grade 9",
                "Concept mastery",
                "Science"
        );

        assertFalse(shouldShowOnboarding);
    }

    @Test
    public void termsOutdated_doesNotRouteToOnboardingBranch() {
        boolean shouldShowOnboarding = SplashActivity.shouldShowOnboardingForExistingUser(
                0,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(shouldShowOnboarding);
    }
}
