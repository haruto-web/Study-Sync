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
import androidx.fragment.app.Fragment;

import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.databinding.FragmentHomeBinding;
import com.example.studysync_project.ui.auth.LoginActivity;
import com.example.studysync_project.ui.profile.ProfileActivity;
import com.example.studysync_project.ui.quiz.UploadModuleActivity;
import com.example.studysync_project.utils.ConsentManager;
import com.example.studysync_project.utils.ReadyModuleCatalog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    private ReadyModuleAdapter readyModuleAdapter;
    private ReadyModule recommendedModule;
    private Double latestAverageScore;
    private String profileGradeLevel;
    private String profileStrand;
    private String profileGoal;
    private String profileSubject;
    private String profileTopicsCsv;

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
            binding.tvTasksSummary.setText(c + " pending task" + (c == 1 ? "" : "s"));
        });

        timerRepo.getTotalStudyMinutesForUser(userId).observe(getViewLifecycleOwner(), mins -> {
            int m = mins != null ? mins : 0;
            if (m >= 60) {
                binding.tvStudyTime.setText((m / 60) + "h " + (m % 60) + "m today");
            } else {
                binding.tvStudyTime.setText(m + " minutes today");
            }
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
    }

    private void setupReadyModules() {
        readyModuleAdapter = new ReadyModuleAdapter(module -> openReadyModule(module));
        binding.rvReadyModules.setAdapter(readyModuleAdapter);

        binding.btnRecommendationAction.setOnClickListener(v -> {
            if (recommendedModule != null) {
                openReadyModule(recommendedModule);
            } else {
                startActivity(new Intent(requireContext(), UploadModuleActivity.class));
            }
        });

        updateReadyModules();
        updateRecommendationCard();
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
        if (latestAverageScore == null) {
            message = "Start with a quick " + subject + " module to build momentum.";
        } else if (latestAverageScore < 70.0) {
            message = String.format("Your recent average is %.0f%%. Review a core %s module next.", latestAverageScore, subject);
        } else {
            message = String.format("Nice work (%.0f%% average). Try a slightly harder %s module next.", latestAverageScore, subject);
        }

        if (recommendedModule != null) {
            message = message + " Recommended: " + recommendedModule.title + ".";
        }

        binding.tvRecommendationBody.setText(message);
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
