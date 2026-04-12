package com.example.studysync_project.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.studysync_project.R;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.model.UserProfile;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.data.repository.UserRepository;
import com.example.studysync_project.databinding.FragmentHomeBinding;
import com.example.studysync_project.ui.auth.LoginActivity;
import com.example.studysync_project.ui.profile.ProfileActivity;
import com.example.studysync_project.ui.progress.ProgressActivity;
import com.example.studysync_project.ui.quiz.ModuleDetailActivity;
import com.example.studysync_project.ui.quiz.StudyModuleAdapter;
import com.example.studysync_project.ui.quiz.UploadModuleActivity;
import com.example.studysync_project.utils.ConsentManager;
import com.example.studysync_project.utils.ReadyModuleCatalog;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    private ReadyModuleAdapter readyModuleAdapter;
    private StudyModuleAdapter savedModuleAdapter;
    private StudyModule topSavedModule;
    private ReadyModule recommendedModule;
    private int recommendationAction = ACTION_UPLOAD;

    private static final int ACTION_UPLOAD = 0;
    private static final int ACTION_READY_MODULE = 1;
    private static final int ACTION_SAVED_MODULE = 2;
    private static final String[][] BADGE_DEFINITIONS = new String[][]{
            {"STREAK_3", "3-Day Streak", "Study on 3 consecutive days."},
            {"STREAK_7", "7-Day Streak", "Study on 7 consecutive days."},
            {"STREAK_14", "14-Day Streak", "Study on 14 consecutive days."},
            {"QUIZ_ACE_80", "Quiz Ace", "Average at least 80% across 3 recent quiz attempts."},
            {"MOMENTUM_70", "Momentum 70", "Reach a progression index of 70."},
            {"MOMENTUM_85", "Momentum 85", "Reach a progression index of 85."}
    };

    private Double latestAverageScore;
    private String latestProgressStateLabel;
    private int latestProgressIndex;
    private int latestStreakDays;
    private int latestPendingTasks;
    private int latestStudyMinutesToday;
    private int latestSessionsToday;
    private int latestWeeklyStudyMinutes;
    private double latestWeeklyQuizAverage;
    private int latestSavedModuleCount;
    private String profileGradeLevel;
    private String profileStrand;
    private String profileGoal;
    private String profileSubject;
    private String profileTopicsCsv;
    private final Set<String> unlockedBadges = new LinkedHashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = auth.getUid();
        if (userId == null) return;

        loadUserData();
        loadLiveStats();
        setupClickListeners();
        setupReadyModules();
        setupSavedModules();
    }

    private void loadUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    if (name == null) name = doc.getString("fullName");
                    binding.tvWelcome.setText("Welcome back, " + (name != null ? name : "Student") + "!");
                    if (email != null) binding.tvUserEmail.setText(email);

                profileGradeLevel = doc.getString("gradeLevel");
                profileStrand = doc.getString("strand");
                profileGoal = doc.getString("goal");
                profileSubject = doc.getString("subjectsCsv");
                profileTopicsCsv = doc.getString("topicsOfInterestCsv");

                // Cache locally to support limited/offline behavior.
                ConsentManager.storeOnboarding(requireContext(), userId,
                    profileGradeLevel != null ? profileGradeLevel : "",
                    profileStrand != null ? profileStrand : "",
                    profileGoal != null ? profileGoal : "",
                    profileSubject != null ? profileSubject : "",
                    profileTopicsCsv != null ? profileTopicsCsv : "");

                updateReadyModules();
                updateRecommendationCard();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show());
    }

    private void loadLiveStats() {
        TaskRepository taskRepo = new TaskRepository(requireContext());
        TimerRepository timerRepo = new TimerRepository(requireContext());
        QuizAttemptRepository attemptRepo = new QuizAttemptRepository(requireContext());

        taskRepo.getActiveTaskCountForUser(userId).observe(getViewLifecycleOwner(), count -> {
            int c = count != null ? count : 0;
            latestPendingTasks = c;
            binding.tvTasksSummary.setText(c + " pending task" + (c == 1 ? "" : "s"));
            updateRecommendationCard();
        });

        timerRepo.getTodayStudyMinutesForUser(userId).observe(getViewLifecycleOwner(), mins -> {
            latestStudyMinutesToday = mins != null ? mins : 0;
            bindStudySummary();
            updateRecommendationCard();
        });

        timerRepo.getTodayCompletedSessionCountForUser(userId).observe(getViewLifecycleOwner(), count -> {
            latestSessionsToday = count != null ? count : 0;
            bindStudySummary();
            updateRecommendationCard();
        });

        attemptRepo.getAverageScoreForUser(userId).observe(getViewLifecycleOwner(), avg -> {
            if (avg != null && avg > 0) {
                binding.tvQuizScore.setText(String.format("Avg score: %.0f%%", avg));
                latestAverageScore = avg;
            } else {
                binding.tvQuizScore.setText("No quizzes yet");
                latestAverageScore = null;
            }

            updateRecommendationCard();
        });

        new UserRepository(requireContext()).getUserProfile(userId).observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                binding.tvProgressIndex.setText("0/100");
                binding.tvProgressState.setText("Starting");
                binding.tvProgressState.setTextColor(ContextCompat.getColor(requireContext(), com.example.studysync_project.R.color.text_secondary));
                binding.tvProgressMeta.setText("Build activity to unlock trend insights");
                binding.tvStreakHighlight.setText(getString(R.string.home_streak_format, 0));
                binding.tvWeeklyAnalytics.setText(getString(R.string.home_weekly_metrics_format, "0m", 0.0));
                binding.progressWeeklyStudy.setProgress(0);
                latestProgressStateLabel = "Starting";
                latestProgressIndex = 0;
                latestStreakDays = 0;
                latestWeeklyStudyMinutes = 0;
                latestWeeklyQuizAverage = 0.0;
                updateBadgeState(null);
                updateRecommendationCard();
                return;
            }

            String stateLabel = ProgressionRepository.formatStateLabel(profile.getProgressionState());
            int roundedIndex = (int) Math.round(profile.getProgressionIndex());
            binding.tvProgressIndex.setText(roundedIndex + "/100");
            binding.tvProgressState.setText(stateLabel);

            int stateColorRes;
            if ("Improving".equals(stateLabel)) {
                stateColorRes = com.example.studysync_project.R.color.success;
            } else if ("Declining".equals(stateLabel)) {
                stateColorRes = com.example.studysync_project.R.color.warning;
            } else if ("Inactive".equals(stateLabel)) {
                stateColorRes = com.example.studysync_project.R.color.inactive;
            } else {
                stateColorRes = com.example.studysync_project.R.color.info;
            }
            binding.tvProgressState.setTextColor(ContextCompat.getColor(requireContext(), stateColorRes));

            StringBuilder meta = new StringBuilder();
            meta.append(profile.getCurrentStreakDays()).append(" day streak");
            if (profile.getFocusSubject() != null && !profile.getFocusSubject().trim().isEmpty()) {
                meta.append(" • Focus: ").append(profile.getFocusSubject().trim());
            }
            if (profile.getLastUnlockedBadge() != null && !profile.getLastUnlockedBadge().trim().isEmpty()) {
                meta.append(" • Badge: ").append(formatBadgeLabel(profile.getLastUnlockedBadge()));
            }
            binding.tvProgressMeta.setText(meta.toString());

            latestStreakDays = profile.getCurrentStreakDays();
            binding.tvStreakHighlight.setText(getString(R.string.home_streak_format, latestStreakDays));

            latestWeeklyStudyMinutes = profile.getStudyMinutesLast7Days();
            latestWeeklyQuizAverage = profile.getAverageQuizScoreLast7Days();
            binding.tvWeeklyAnalytics.setText(getString(
                    R.string.home_weekly_metrics_format,
                    formatMinutesShort(latestWeeklyStudyMinutes),
                    latestWeeklyQuizAverage
            ));

            int weeklyTarget = profile.getWeeklyStudyTargetMinutes() > 0 ? profile.getWeeklyStudyTargetMinutes() : 180;
            int weeklyProgress = (int) Math.round((latestWeeklyStudyMinutes * 100.0) / weeklyTarget);
            binding.progressWeeklyStudy.setProgress(Math.max(0, Math.min(100, weeklyProgress)));

            latestProgressStateLabel = stateLabel;
            latestProgressIndex = roundedIndex;
            updateBadgeState(profile);
            updateRecommendationCard();
        });
    }

    private void bindStudySummary() {
        if (binding == null) return;
        String todayText = formatMinutesShort(latestStudyMinutesToday);
        if (latestSessionsToday > 0) {
            binding.tvStudyTime.setText(getString(R.string.home_study_today_with_sessions_format, todayText, latestSessionsToday));
        } else {
            binding.tvStudyTime.setText(getString(R.string.home_study_today_format, todayText));
        }
    }

    private String formatMinutesShort(int minutes) {
        if (minutes >= 60) {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }

    private String formatBadgeLabel(String raw) {
        if (raw == null) return "";
        return raw.replace('_', ' ').trim();
    }

    private void setupReadyModules() {
        readyModuleAdapter = new ReadyModuleAdapter(module -> openReadyModule(module));
        binding.rvReadyModules.setAdapter(readyModuleAdapter);

        binding.btnRecommendationAction.setOnClickListener(v -> handleRecommendationAction());

        updateReadyModules();
        updateRecommendationCard();
    }

    private void setupSavedModules() {
        savedModuleAdapter = new StudyModuleAdapter(new StudyModuleAdapter.OnStudyModuleClickListener() {
            @Override
            public void onStudyModuleClick(StudyModule module) {
                openSavedModule(module);
            }

            @Override
            public void onGenerateQuizFromModule(StudyModule module) {
                generateQuizFromSavedModule(module);
            }
        });
        binding.rvSavedModules.setAdapter(savedModuleAdapter);

        StudyModuleRepository repository = new StudyModuleRepository(requireContext());
        repository.syncStudyModulesFromFirestore(userId);
        repository.getAllStudyModulesForUser(userId).observe(getViewLifecycleOwner(), modules -> {
            List<StudyModule> source = modules != null ? modules : new ArrayList<>();
            latestSavedModuleCount = source.size();

            List<StudyModule> top3 = new ArrayList<>();
            for (int i = 0; i < source.size() && i < 3; i++) {
                top3.add(source.get(i));
            }

            savedModuleAdapter.submitList(top3);
            binding.tvSavedModulesEmpty.setVisibility(top3.isEmpty() ? View.VISIBLE : View.GONE);

            topSavedModule = top3.isEmpty() ? null : top3.get(0);
            binding.tvModulesSummary.setText(getString(
                    R.string.home_modules_summary_format,
                    latestSavedModuleCount,
                    top3.size()
            ));
            updateRecommendationCard();
        });
    }

    private void updateReadyModules() {
        if (binding == null || userId == null) return;

        String gradeLevel = profileGradeLevel;
        String strand = profileStrand;
        String subject = profileSubject;
        String topicsCsv = profileTopicsCsv;

        // Fall back to locally stored onboarding for limited mode/offline.
        if (gradeLevel == null || gradeLevel.trim().isEmpty()) {
            gradeLevel = ConsentManager.getStoredGradeLevel(requireContext(), userId);
        }
        if (strand == null || strand.trim().isEmpty()) {
            strand = ConsentManager.getStoredStrand(requireContext(), userId);
        }
        if (subject == null || subject.trim().isEmpty()) {
            subject = ConsentManager.getStoredSubject(requireContext(), userId);
        }
        if (topicsCsv == null) {
            topicsCsv = ConsentManager.getStoredTopicsCsv(requireContext(), userId);
        }

        List<ReadyModule> filtered = filterModules(gradeLevel, strand, subject, topicsCsv);
        // Keep list short on Home.
        if (filtered.size() > 3) {
            filtered = filtered.subList(0, 3);
        }
        readyModuleAdapter.submitList(filtered);
        binding.tvReadyModulesEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);

        recommendedModule = filtered.isEmpty() ? null : filtered.get(0);
    }

    private List<ReadyModule> filterModules(String gradeLevel, String strand, String subject, String topicsCsv) {
        List<ReadyModule> all = ReadyModuleCatalog.getAllModules();
        List<ReadyModule> out = new ArrayList<>();

        String grade = gradeLevel != null ? gradeLevel.trim() : "";
        String strandKey = strand != null ? strand.trim() : "";
        String subj = subject != null ? subject.trim() : "";
        String topics = topicsCsv != null ? topicsCsv.trim() : "";

        String[] tokens = topics.isEmpty() ? new String[0] : topics.split(",");

        for (ReadyModule m : all) {
            if (!grade.isEmpty() && m.gradeLevel != null && !m.gradeLevel.equalsIgnoreCase(grade)) {
                continue;
            }
            // For SHS grades, also filter by strand if the module has a strand
            if (!strandKey.isEmpty() && m.strand != null && !m.strand.isEmpty()) {
                // Match on the strand abbreviation (e.g. "STEM" matches "STEM (Science...)"
                String mStrand = m.strand.toUpperCase();
                String sKey = strandKey.toUpperCase();
                if (!sKey.contains(mStrand) && !mStrand.contains(sKey.split("[\\ (]")[0])) {
                    continue;
                }
            }
            if (!subj.isEmpty() && m.subject != null && !m.subject.equalsIgnoreCase(subj)) {
                continue;
            }
            if (tokens.length > 0) {
                boolean match = false;
                for (String t : tokens) {
                    String tok = t != null ? t.trim().toLowerCase() : "";
                    if (tok.isEmpty()) continue;
                    String hay = (m.title + " " + m.topic + " " + m.description).toLowerCase();
                    if (hay.contains(tok)) {
                        match = true;
                        break;
                    }
                }
                if (!match) continue;
            }
            out.add(m);
        }

        // If topics filter returns nothing, fall back to grade+strand+subject only.
        if (out.isEmpty() && tokens.length > 0) {
            for (ReadyModule m : all) {
                if (!grade.isEmpty() && m.gradeLevel != null && !m.gradeLevel.equalsIgnoreCase(grade)) continue;
                if (!strandKey.isEmpty() && m.strand != null && !m.strand.isEmpty()) {
                    String mStrand = m.strand.toUpperCase();
                    String sKey = strandKey.toUpperCase();
                    if (!sKey.contains(mStrand) && !mStrand.contains(sKey.split("[\\ (]")[0])) continue;
                }
                if (!subj.isEmpty() && m.subject != null && !m.subject.equalsIgnoreCase(subj)) continue;
                out.add(m);
            }
        }

        // If still empty, return unfiltered list.
        if (out.isEmpty()) out.addAll(all);

        return out;
    }

    private void updateRecommendationCard() {
        if (binding == null || userId == null) return;

        boolean personalizationEnabled = ConsentManager.isPersonalizationEnabled(requireContext(), userId);
        binding.tvRecommendedHeader.setVisibility(personalizationEnabled ? View.VISIBLE : View.GONE);
        binding.cardRecommendation.setVisibility(personalizationEnabled ? View.VISIBLE : View.GONE);

        if (!personalizationEnabled) {
            return;
        }

        String subject = profileSubject;
        if (subject == null || subject.trim().isEmpty()) {
            subject = ConsentManager.getStoredSubject(requireContext(), userId);
        }
        if (subject == null || subject.trim().isEmpty()) subject = "your subject";

        String message;
        if (topSavedModule != null && latestStudyMinutesToday < 20) {
            recommendationAction = ACTION_SAVED_MODULE;
            binding.btnRecommendationAction.setText(R.string.home_action_continue_saved_module);
            message = "You have only " + formatMinutesShort(latestStudyMinutesToday)
                    + " of study today. Continue \"" + topSavedModule.getTitle() + "\" for a 20-minute push.";
        } else if (latestPendingTasks >= 3) {
            recommendationAction = topSavedModule != null ? ACTION_SAVED_MODULE : ACTION_READY_MODULE;
            binding.btnRecommendationAction.setText(topSavedModule != null
                ? R.string.home_action_continue_saved_module
                : R.string.home_action_open_recommended_module);
            message = "You have " + latestPendingTasks + " pending tasks. Complete one task, then run a focused "
                    + subject + " review session.";
        } else if (latestAverageScore == null) {
            recommendationAction = topSavedModule != null ? ACTION_SAVED_MODULE : ACTION_READY_MODULE;
            binding.btnRecommendationAction.setText(topSavedModule != null
                ? R.string.home_action_continue_saved_module
                : R.string.home_action_open_recommended_module);
            message = topSavedModule != null
                    ? "Start with your latest saved module to build momentum."
                    : "Start with a quick " + subject + " module to build momentum.";
        } else if (latestAverageScore < 70.0) {
            recommendationAction = topSavedModule != null ? ACTION_SAVED_MODULE : ACTION_READY_MODULE;
            binding.btnRecommendationAction.setText(topSavedModule != null
                ? R.string.home_action_review_saved_module
                : R.string.home_action_open_recommended_module);
            message = String.format(Locale.getDefault(), "Your recent average is %.0f%%. Review a core %s module next.", latestAverageScore, subject);
        } else {
            recommendationAction = ACTION_READY_MODULE;
            binding.btnRecommendationAction.setText(R.string.home_action_open_recommended_module);
            message = String.format(Locale.getDefault(), "Nice work (%.0f%% average). Try a slightly harder %s module next.", latestAverageScore, subject);
        }

        if (latestProgressStateLabel != null) {
            if ("Improving".equals(latestProgressStateLabel)) {
                message = "You are improving (" + latestProgressIndex + "/100). Keep the " + latestStreakDays + " day streak going. " + message;
            } else if ("Declining".equals(latestProgressStateLabel)) {
                message = "Momentum dipped to " + latestProgressIndex + "/100. A focused review can reverse this quickly. " + message;
            } else if ("Inactive".equals(latestProgressStateLabel)) {
                message = "Your momentum is inactive. A short session today will restart your streak. " + message;
            }
        }

        if (recommendationAction == ACTION_READY_MODULE && recommendedModule != null) {
            message = message + " Recommended: " + recommendedModule.title + ".";
        } else if (recommendationAction == ACTION_SAVED_MODULE && topSavedModule != null) {
            String title = topSavedModule.getTitle() != null ? topSavedModule.getTitle().trim() : "";
            if (!title.isEmpty() && !message.contains(title)) {
                message = message + " Continue with: " + title + ".";
            }
        }

        binding.tvRecommendationBody.setText(message);
    }

    private void handleRecommendationAction() {
        if (recommendationAction == ACTION_SAVED_MODULE && topSavedModule != null) {
            openSavedModule(topSavedModule);
            return;
        }

        if (recommendationAction == ACTION_READY_MODULE && recommendedModule != null) {
            openReadyModule(recommendedModule);
            return;
        }

        startActivity(new Intent(requireContext(), UploadModuleActivity.class));
    }

    private void openSavedModule(@Nullable StudyModule module) {
        if (module == null || module.getModuleId() == null || module.getModuleId().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Module not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), ModuleDetailActivity.class);
        intent.putExtra(ModuleDetailActivity.EXTRA_MODULE_ID, module.getModuleId());
        startActivity(intent);
    }

    private void generateQuizFromSavedModule(@Nullable StudyModule module) {
        if (module == null || module.getContentText() == null || module.getContentText().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Module content is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), UploadModuleActivity.class);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_ID, module.getModuleId());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TITLE, module.getTitle());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_SUBJECT, module.getSubject());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TEXT, module.getContentText());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_TYPE, module.getSourceType());
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_REF, module.getSourceRef());
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_QUESTION_COUNT, 10);
        startActivity(intent);
    }

    private void openReadyModule(@NonNull ReadyModule module) {
        Intent intent = new Intent(requireContext(), UploadModuleActivity.class);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TITLE, module.title);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_SUBJECT, module.subject);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_TEXT, module.content);
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_TYPE, "READY_MADE");
        intent.putExtra(UploadModuleActivity.EXTRA_MODULE_SOURCE_REF, module.id);
        intent.putExtra(UploadModuleActivity.EXTRA_READY_MODULE_QUESTION_COUNT, 10);
        startActivity(intent);
    }

    private void setupClickListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        binding.cardAvatar.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ProfileActivity.class)));
        binding.cardHomeOverview.setOnClickListener(v -> openProgressAnalytics());
        binding.layoutOpenAnalytics.setOnClickListener(v -> openProgressAnalytics());
        binding.tvWeeklyAnalytics.setOnClickListener(v -> openProgressAnalytics());
        binding.progressWeeklyStudy.setOnClickListener(v -> openProgressAnalytics());
        binding.btnBadgeNotice.setOnClickListener(v -> showBadgeCenter());
    }

    private void openProgressAnalytics() {
        startActivity(new Intent(requireContext(), ProgressActivity.class));
    }

    private void updateBadgeState(@Nullable UserProfile profile) {
        unlockedBadges.clear();
        if (profile != null) {
            String badgesCsv = profile.getUnlockedBadgesCsv() != null
                    ? profile.getUnlockedBadgesCsv().trim()
                    : "";
            if (!badgesCsv.isEmpty()) {
                for (String rawPart : badgesCsv.split(",")) {
                    String badge = rawPart != null ? rawPart.trim() : "";
                    if (!badge.isEmpty()) {
                        unlockedBadges.add(badge);
                    }
                }
            }
        }
        updateBadgeNoticeUi();
    }

    private void updateBadgeNoticeUi() {
        if (binding == null) {
            return;
        }

        int count = unlockedBadges.size();
        if (count > 0) {
            binding.tvBadgeNoticeCount.setVisibility(View.VISIBLE);
            binding.tvBadgeNoticeCount.setText(String.valueOf(Math.min(count, 99)));
            binding.btnBadgeNotice.setContentDescription(getString(R.string.home_badge_notice_icon_desc_count, count));
        } else {
            binding.tvBadgeNoticeCount.setVisibility(View.GONE);
            binding.btnBadgeNotice.setContentDescription(getString(R.string.home_badge_notice_icon_desc));
        }
    }

    private void showBadgeCenter() {
        StringBuilder message = new StringBuilder();
        message.append(getString(
                R.string.home_badge_dialog_progress,
                unlockedBadges.size(),
                BADGE_DEFINITIONS.length
        ));

        if (unlockedBadges.isEmpty()) {
            message.append("\n\n").append(getString(R.string.home_badge_dialog_empty_hint));
        }

        message.append("\n\n").append(getString(R.string.home_badge_dialog_section_title));
        for (String[] badge : BADGE_DEFINITIONS) {
            boolean unlocked = unlockedBadges.contains(badge[0]);
            message.append("\n\n")
                    .append(unlocked ? "[Unlocked] " : "[Locked] ")
                    .append(badge[1])
                    .append("\n")
                    .append(badge[2]);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_badge_dialog_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.home_badge_dialog_open_profile, (dialog, which) ->
                        startActivity(new Intent(requireContext(), ProfileActivity.class)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (d, w) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
