package com.example.studysync_project.data.progression;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProgressionScoringTest {

    @Test
    public void momentumScore_respectsTimeBuckets() {
        long now = 1_000_000_000_000L;

        assertEquals(100.0, ProgressionScoring.computeMomentumScore(now - 20L * 3600000L, now), 0.001);
        assertEquals(85.0, ProgressionScoring.computeMomentumScore(now - 36L * 3600000L, now), 0.001);
        assertEquals(70.0, ProgressionScoring.computeMomentumScore(now - 60L * 3600000L, now), 0.001);
        assertEquals(50.0, ProgressionScoring.computeMomentumScore(now - 120L * 3600000L, now), 0.001);
        assertEquals(10.0, ProgressionScoring.computeMomentumScore(now - 200L * 3600000L, now), 0.001);
    }

    @Test
    public void deriveProgressionState_handlesCoreCases() {
        long now = 2_000_000_000_000L;
        long recent = now - 2L * 24L * 60L * 60L * 1000L;
        long stale = now - 10L * 24L * 60L * 60L * 1000L;

        assertEquals("INACTIVE", ProgressionScoring.deriveProgressionState(0.0, 3, 90, stale, now));
        assertEquals("STARTING", ProgressionScoring.deriveProgressionState(0.0, 0, 20, recent, now));
        assertEquals("IMPROVING", ProgressionScoring.deriveProgressionState(3.0, 1, 40, recent, now));
        assertEquals("DECLINING", ProgressionScoring.deriveProgressionState(-3.0, 1, 40, recent, now));
        assertEquals("STABLE", ProgressionScoring.deriveProgressionState(1.2, 1, 40, recent, now));
    }

    @Test
    public void formatStateLabel_mapsValuesSafely() {
        assertEquals("Improving", ProgressionScoring.formatStateLabel("IMPROVING"));
        assertEquals("Declining", ProgressionScoring.formatStateLabel("declining"));
        assertEquals("Stable", ProgressionScoring.formatStateLabel("stable"));
        assertEquals("Inactive", ProgressionScoring.formatStateLabel("INACTIVE"));
        assertEquals("Starting", ProgressionScoring.formatStateLabel(""));
        assertEquals("Starting", ProgressionScoring.formatStateLabel(null));
        assertEquals("Starting", ProgressionScoring.formatStateLabel("unknown"));
    }

    @Test
    public void clampAndRound_helpersBehavePredictably() {
        assertEquals(100.0, ProgressionScoring.clamp(124.0, 0.0, 100.0), 0.001);
        assertEquals(0.0, ProgressionScoring.clamp(-4.0, 0.0, 100.0), 0.001);
        assertEquals(72.3, ProgressionScoring.round1(72.34), 0.001);
        assertEquals(72.4, ProgressionScoring.round1(72.35), 0.001);
    }
}
