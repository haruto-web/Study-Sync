package com.example.studysync_project.ui.progress;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.studysync_project.BuildConfig;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.data.repository.UserRepository;
import com.example.studysync_project.databinding.ActivityProgressBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.NetworkUtil;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProgressActivity extends AppCompatActivity {

    private ActivityProgressBinding binding;
    private String userId;
    private int activeTaskCount;
    private int completedTaskCount;
    private int totalStudyMinutes;
    private double averageScoreQuizNormalized;
    private Double averageScoreModuleNormalized;
    private boolean analyticsInsightLoading;
    private UserProfile latestProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProgressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvInteractiveInsight.setText(getString(R.string.progress_interactive_hint));
        binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_placeholder));
        binding.btnGenerateAnalyticsInsight.setOnClickListener(v -> requestAnalyticsInsight());
        updateSummaryAverageCard();

        loadSummaryStats();
        loadProgressionSummary();
        loadStudyTimeChart();
        loadQuizScoreChart();
        loadTaskPieChart();
    }

    private void loadProgressionSummary() {
        new UserRepository(this).getUserProfile(userId).observe(this, this::bindProgression);
    }

    private void bindProgression(UserProfile profile) {
        latestProfile = profile;
        if (profile == null) {
            binding.tvProgressionIndex.setText("0/100");
            binding.tvProgressionState.setText("Starting");
            binding.tvProgressionDelta.setText("No trend yet");
            binding.tvProgressionState.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            binding.tvProgressionDelta.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            return;
        }

        int roundedIndex = (int) Math.round(profile.getProgressionIndex());
        binding.tvProgressionIndex.setText(roundedIndex + "/100");
        String stateLabel = ProgressionRepository.formatStateLabel(profile.getProgressionState());
        binding.tvProgressionState.setText(
                stateLabel +
                        " • " + profile.getCurrentStreakDays() + " day streak"
        );

        int stateColorRes;
        if ("Improving".equals(stateLabel)) {
            stateColorRes = R.color.success;
        } else if ("Declining".equals(stateLabel)) {
            stateColorRes = R.color.warning;
        } else if ("Inactive".equals(stateLabel)) {
            stateColorRes = R.color.inactive;
        } else {
            stateColorRes = R.color.info;
        }
        binding.tvProgressionState.setTextColor(ContextCompat.getColor(this, stateColorRes));

        double delta = profile.getProgressionDelta();
        String deltaPrefix = delta > 0 ? "+" : "";
        binding.tvProgressionDelta.setText(String.format(Locale.getDefault(), "%s%.1f vs last update", deltaPrefix, delta));
        int deltaColorRes = delta > 0
            ? R.color.success
            : (delta < 0 ? R.color.warning : R.color.text_secondary);
        binding.tvProgressionDelta.setTextColor(ContextCompat.getColor(this, deltaColorRes));

        String focus = profile.getFocusSubject() != null ? profile.getFocusSubject().trim() : "";
        String strongest = profile.getStrongestSubject() != null ? profile.getStrongestSubject().trim() : "";
        if (!focus.isEmpty() || !strongest.isEmpty()) {
            StringBuilder insight = new StringBuilder();
            if (!focus.isEmpty()) {
                insight.append("Focus next: ").append(focus);
            }
            if (!strongest.isEmpty()) {
                if (insight.length() > 0) insight.append(" • ");
                insight.append("Strongest: ").append(strongest);
            }
            binding.tvProgressionFocus.setText(insight.toString());
        } else {
            binding.tvProgressionFocus.setText("Keep practicing to unlock subject-specific insights.");
        }

            int weeklyMinutes = profile.getStudyMinutesLast7Days();
            int weeklyHours = weeklyMinutes / 60;
            int weeklyRemMinutes = weeklyMinutes % 60;
            String weeklyStudyText = weeklyHours > 0
                ? weeklyHours + "h " + weeklyRemMinutes + "m"
                : weeklyMinutes + "m";
            binding.tvProgressionWeekly.setText(String.format(
                Locale.getDefault(),
                "Last 7d: %.0f%% normalized avg • %s study",
                profile.getAverageQuizScoreLast7Days(),
                weeklyStudyText
            ));
    }

    private void loadSummaryStats() {
        QuizAttemptRepository attemptRepository = new QuizAttemptRepository(this);
        attemptRepository.getAverageScoreForUser(userId).observe(this, avg -> {
            averageScoreQuizNormalized = avg != null ? avg : 0.0;
            updateSummaryAverageCard();
        });
        attemptRepository.getAverageScoreForUserByModule(userId).observe(this, avg -> {
            averageScoreModuleNormalized = avg;
            updateSummaryAverageCard();
        });
        new TimerRepository(this).getTotalStudyMinutesForUser(userId).observe(this, mins -> {
            int m = mins != null ? mins : 0;
            totalStudyMinutes = m;
            binding.tvTotalStudy.setText(m >= 60 ? (m / 60) + "h" : m + "m");
        });
    }

    private void updateSummaryAverageCard() {
        double shownAverage = averageScoreModuleNormalized != null
                ? averageScoreModuleNormalized
                : averageScoreQuizNormalized;
        binding.tvAvgScore.setText(String.format(Locale.getDefault(), "%.0f%%", shownAverage));
        binding.tvAvgScoreScope.setText(getString(
                averageScoreModuleNormalized != null
                        ? R.string.progress_avg_scope_module
                        : R.string.progress_avg_scope_quiz
        ));
    }

    private void loadStudyTimeChart() {
        long sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        new TimerRepository(this).getSessionsInRange(userId, sevenDaysAgo).observe(this, sessions -> {
            // Bucket minutes by day-of-week label
            String[] dayLabels = new String[7];
            float[] minutesByDay = new float[7];
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
            for (int i = 6; i >= 0; i--) {
                cal.setTimeInMillis(System.currentTimeMillis() - (long) i * 24 * 60 * 60 * 1000);
                dayLabels[6 - i] = sdf.format(cal.getTime());
            }
            if (sessions != null) {
                for (TimerSession s : sessions) {
                    cal.setTimeInMillis(s.getStartTime());
                    long diff = System.currentTimeMillis() - s.getStartTime();
                    int dayIndex = (int) (diff / (24 * 60 * 60 * 1000));
                    if (dayIndex < 7) minutesByDay[6 - dayIndex] += s.getActualDurationMinutes();
                }
            }
            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 7; i++) entries.add(new BarEntry(i, minutesByDay[i]));

            BarDataSet dataSet = new BarDataSet(entries, "Minutes");
            dataSet.setColor(0xFF6750A4);
            dataSet.setValueTextColor(Color.DKGRAY);

            binding.chartStudyTime.setData(new BarData(dataSet));
            binding.chartStudyTime.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dayLabels));
            binding.chartStudyTime.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            binding.chartStudyTime.getXAxis().setGranularity(1f);
            binding.chartStudyTime.getXAxis().setDrawGridLines(false);
            binding.chartStudyTime.getAxisRight().setEnabled(false);
            binding.chartStudyTime.getDescription().setEnabled(false);
            binding.chartStudyTime.getLegend().setEnabled(false);
            binding.chartStudyTime.setFitBars(true);
            binding.chartStudyTime.setPinchZoom(false);
            binding.chartStudyTime.setScaleEnabled(false);
            binding.chartStudyTime.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    int index = Math.round(e.getX());
                    if (index < 0 || index >= dayLabels.length) {
                        return;
                    }
                    setInteractiveInsight(getString(
                            R.string.progress_interactive_study_format,
                            dayLabels[index],
                            Math.round(e.getY())
                    ));
                }

                @Override
                public void onNothingSelected() {
                    setInteractiveInsight(getString(R.string.progress_interactive_hint));
                }
            });
            binding.chartStudyTime.animateY(600);
            binding.chartStudyTime.invalidate();
        });
    }

    private void loadQuizScoreChart() {
        new QuizAttemptRepository(this).getAllQuizAttemptsForUser(userId).observe(this, attempts -> {
            List<Entry> entries = new ArrayList<>();
            if (attempts != null) {
                int max = Math.min(attempts.size(), 10);
                for (int i = 0; i < max; i++) {
                    QuizAttempt a = attempts.get(attempts.size() - max + i);
                    entries.add(new Entry(i, (float) a.getScorePercentage()));
                }
            }
            if (entries.isEmpty()) entries.add(new Entry(0, 0));

            LineDataSet dataSet = new LineDataSet(entries, "Score %");
            dataSet.setColor(0xFF625B71);
            dataSet.setCircleColor(0xFF625B71);
            dataSet.setValueTextColor(Color.DKGRAY);
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            binding.chartQuizScores.setData(new LineData(dataSet));
            binding.chartQuizScores.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            binding.chartQuizScores.getAxisRight().setEnabled(false);
            binding.chartQuizScores.getDescription().setEnabled(false);
            binding.chartQuizScores.getLegend().setEnabled(false);
            binding.chartQuizScores.getAxisLeft().setAxisMinimum(0f);
            binding.chartQuizScores.getAxisLeft().setAxisMaximum(100f);
            binding.chartQuizScores.setPinchZoom(false);
            binding.chartQuizScores.setScaleEnabled(false);
            binding.chartQuizScores.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    int attemptNumber = Math.max(1, Math.round(e.getX()) + 1);
                    setInteractiveInsight(getString(
                            R.string.progress_interactive_quiz_format,
                            attemptNumber,
                            Math.round(e.getY())
                    ));
                }

                @Override
                public void onNothingSelected() {
                    setInteractiveInsight(getString(R.string.progress_interactive_hint));
                }
            });
            binding.chartQuizScores.animateX(600);
            binding.chartQuizScores.invalidate();
        });
    }

    private void loadTaskPieChart() {
        TaskRepository taskRepo = new TaskRepository(this);
        taskRepo.getActiveTaskCountForUser(userId).observe(this, active -> {
            activeTaskCount = active != null ? active : 0;
            bindTaskPieChart();
        });
        taskRepo.getCompletedTaskCountForUser(userId).observe(this, completed -> {
            completedTaskCount = completed != null ? completed : 0;
            bindTaskPieChart();
        });
    }

    private void bindTaskPieChart() {
        int a = activeTaskCount;
        int c = completedTaskCount;

        if (a == 0 && c == 0) {
            binding.chartTasks.clear();
            binding.chartTasks.setNoDataText(getString(R.string.progress_no_tasks_data));
            binding.chartTasks.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        if (c > 0) entries.add(new PieEntry(c, "Done"));
        if (a > 0) entries.add(new PieEntry(a, "Pending"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(0xFF6750A4, 0xFFE8DEF8);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        binding.chartTasks.setData(new PieData(dataSet));
        binding.chartTasks.getDescription().setEnabled(false);
        binding.chartTasks.setHoleRadius(40f);
        binding.chartTasks.setTransparentCircleRadius(45f);
        binding.chartTasks.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (!(e instanceof PieEntry)) {
                    return;
                }
                PieEntry entry = (PieEntry) e;
                setInteractiveInsight(getString(
                        R.string.progress_interactive_tasks_format,
                        entry.getLabel(),
                        Math.round(entry.getValue())
                ));
            }

            @Override
            public void onNothingSelected() {
                setInteractiveInsight(getString(R.string.progress_interactive_hint));
            }
        });
        binding.chartTasks.animateY(600);
        binding.chartTasks.invalidate();
    }

    private void requestAnalyticsInsight() {
        if (analyticsInsightLoading) {
            return;
        }

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_missing_key));
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(this)) {
            binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_offline));
            return;
        }

        setAnalyticsInsightLoading(true);
        String snapshot = buildAnalyticsSnapshot();
        GeminiApiClient.generateAnalyticsInsight(snapshot).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                setAnalyticsInsightLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_failed));
                    return;
                }

                String text = extractGeminiText(response.body());
                if (text == null || text.trim().isEmpty()) {
                    binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_failed));
                    return;
                }
                binding.tvAnalyticsAiInsight.setText(text.trim());
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setAnalyticsInsightLoading(false);
                binding.tvAnalyticsAiInsight.setText(getString(R.string.progress_ai_failed));
            }
        });
    }

    private void setAnalyticsInsightLoading(boolean loading) {
        analyticsInsightLoading = loading;
        binding.progressAnalyticsAi.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnGenerateAnalyticsInsight.setEnabled(!loading);
        binding.btnGenerateAnalyticsInsight.setText(
                loading ? R.string.progress_ai_generating : R.string.progress_ai_generate
        );
    }

    private String buildAnalyticsSnapshot() {
        StringBuilder snapshot = new StringBuilder();
        snapshot.append("User ID: ").append(userId != null ? userId : "unknown").append("\n");

        snapshot.append("Average score (quiz-normalized): ")
                .append(String.format(Locale.getDefault(), "%.1f%%", averageScoreQuizNormalized))
                .append("\n");
        if (averageScoreModuleNormalized != null) {
            snapshot.append("Average score (module-normalized): ")
                    .append(String.format(Locale.getDefault(), "%.1f%%", averageScoreModuleNormalized))
                    .append("\n");
        } else {
            snapshot.append("Average score (module-normalized): N/A\n");
        }

        snapshot.append("Total study minutes: ").append(totalStudyMinutes).append("\n");
        snapshot.append("Tasks pending/completed: ")
                .append(activeTaskCount)
                .append("/")
                .append(completedTaskCount)
                .append("\n");

        if (latestProfile != null) {
            snapshot.append("Progression index: ")
                    .append(String.format(Locale.getDefault(), "%.1f", latestProfile.getProgressionIndex()))
                    .append("\n");
            snapshot.append("Progression state: ")
                    .append(ProgressionRepository.formatStateLabel(latestProfile.getProgressionState()))
                    .append("\n");
            snapshot.append("Progression delta: ")
                    .append(String.format(Locale.getDefault(), "%+.1f", latestProfile.getProgressionDelta()))
                    .append("\n");
            snapshot.append("Weekly study minutes: ")
                    .append(latestProfile.getStudyMinutesLast7Days())
                    .append("\n");
            snapshot.append("Weekly normalized average score: ")
                    .append(String.format(Locale.getDefault(), "%.1f%%", latestProfile.getAverageQuizScoreLast7Days()))
                    .append("\n");

            String focus = latestProfile.getFocusSubject() != null ? latestProfile.getFocusSubject().trim() : "";
            String strongest = latestProfile.getStrongestSubject() != null ? latestProfile.getStrongestSubject().trim() : "";
            snapshot.append("Focus subject: ").append(focus.isEmpty() ? "N/A" : focus).append("\n");
            snapshot.append("Strongest subject: ").append(strongest.isEmpty() ? "N/A" : strongest).append("\n");
        }

        return snapshot.toString();
    }

    private static String extractGeminiText(JsonObject body) {
        if (body == null) {
            return null;
        }
        try {
            return body.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private void setInteractiveInsight(String message) {
        if (message == null || message.trim().isEmpty()) {
            binding.tvInteractiveInsight.setText(getString(R.string.progress_interactive_hint));
            return;
        }
        binding.tvInteractiveInsight.setText(message);
    }
}
