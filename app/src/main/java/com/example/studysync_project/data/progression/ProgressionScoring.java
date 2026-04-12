package com.example.studysync_project.data.progression;

import java.util.Locale;

public final class ProgressionScoring {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private ProgressionScoring() {
        // Utility class.
    }

    public static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String deriveProgressionState(
            double progressionDelta,
            int attempts7Days,
            int studyMinutes7Days,
            long latestActivityAt,
            long now
    ) {
        if (latestActivityAt <= 0L || (now - latestActivityAt) > (7L * DAY_MS)) {
            return "INACTIVE";
        }
        if (attempts7Days == 0 && studyMinutes7Days < 30) {
            return "STARTING";
        }
        if (progressionDelta >= 3.0) {
            return "IMPROVING";
        }
        if (progressionDelta <= -3.0) {
            return "DECLINING";
        }
        return "STABLE";
    }

    public static double computeMomentumScore(long latestActivityAt, long now) {
        if (latestActivityAt <= 0L) return 0.0;

        double hours = (now - latestActivityAt) / 3600000.0;
        if (hours <= 24.0) return 100.0;
        if (hours <= 48.0) return 85.0;
        if (hours <= 72.0) return 70.0;
        if (hours <= 168.0) {
            double ratio = (hours - 72.0) / (168.0 - 72.0);
            return 70.0 - (ratio * 40.0);
        }
        return 10.0;
    }

    public static String formatStateLabel(String state) {
        if (state == null || state.trim().isEmpty()) return "Starting";
        String normalized = state.trim().toLowerCase(Locale.getDefault());
        if ("improving".equals(normalized)) return "Improving";
        if ("declining".equals(normalized)) return "Declining";
        if ("stable".equals(normalized)) return "Stable";
        if ("inactive".equals(normalized)) return "Inactive";
        return "Starting";
    }
}
