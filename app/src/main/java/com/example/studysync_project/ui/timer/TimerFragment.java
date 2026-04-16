package com.example.studysync_project.ui.timer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.studysync_project.R;
import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.databinding.FragmentTimerBinding;
import com.example.studysync_project.utils.FocusTimerSessionStore;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimerFragment extends Fragment {

    private static final String PREFS_STUDYSYNC = "studysync_prefs";
    private static final String PREF_PENDING_TIMER_SUBJECT = "pending_timer_subject";
    private static final String PREF_PENDING_TIMER_TASK_ID = "pending_timer_task_id";
    private static final String PREF_PENDING_TIMER_TASK_TITLE = "pending_timer_task_title";
    private static final String PREF_PENDING_TIMER_TASK_PRIORITY = "pending_timer_task_priority";
    private static final String PREF_PENDING_TIMER_TASK_DUE = "pending_timer_task_due";
    private static final String PREF_PENDING_TIMER_AUTO_START = "pending_timer_auto_start";

    private FragmentTimerBinding binding;
    private TimerViewModel viewModel;
    private final Handler uiSyncHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerUiSyncRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding == null || !serviceBound || timerService == null) {
                return;
            }

            syncTimerUiFromService();
            if (timerService.isRunning()) {
                uiSyncHandler.postDelayed(this, 500L);
            }
        }
    };

    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_TICK.equals(intent.getAction())) {
                long millisLeft = intent.getLongExtra(TimerService.EXTRA_MILLIS_LEFT, 0);
                int prog = intent.getIntExtra(TimerService.EXTRA_PROGRESS, 100);
                viewModel.onTick(millisLeft, prog);
            } else if (TimerService.ACTION_FINISH.equals(intent.getAction())) {
                viewModel.onSessionFinished();
                syncTimerUiFromService();
                stopUiSyncLoop();
                binding.btnStartPause.setText("Start");
                updateSessionLabel();
            }
        }
    };

    private TimerService timerService;
    private boolean serviceBound = false;
    private final List<StudyModule> focusModules = new ArrayList<>();
    private String selectedModuleId = "";
    private String selectedModuleTitle = "";
    private String selectedModuleSubject = "";
    private String pendingTaskId = "";
    private String pendingTaskTitle = "";
    private String pendingTaskPriority = "";
    private String pendingTaskSubject = "";
    private long pendingTaskDueMillis = 0L;
    private boolean pendingTaskAutoStart = false;
    private boolean pendingTaskAutoStartConsumed = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            timerService = ((TimerService.LocalBinder) binder).getService();
            serviceBound = true;
            syncTimerUiFromService();
            // Restore UI if timer was already running
            if (timerService.isRunning()) {
                binding.btnStartPause.setText("Pause");
                startUiSyncLoop();
            }
            updateStartButtonEnabledState();
            maybeAutoStartFromTaskContext();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            updateStartButtonEnabledState();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        viewModel = new TimerViewModel(requireContext(), userId);
        prefillSubjectFromTaskStarter();
        updatePendingTaskContextUi();
        loadFocusModules(userId);

        // Start & bind service
        Intent serviceIntent = new Intent(requireContext(), TimerService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        requireContext().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        observeViewModel();
        setupControls();
        setupTipCard();
        updateStartButtonEnabledState();
    }

    private void observeViewModel() {
        viewModel.getTimerText().observe(getViewLifecycleOwner(), t -> binding.tvTimer.setText(t));
        viewModel.getProgress().observe(getViewLifecycleOwner(), p -> {
            int safeProgress = p != null ? p : 0;
            binding.progressTimer.setProgress(safeProgress);
            binding.tvFocusProgress.setText((100 - safeProgress) + "% complete");
        });
        viewModel.getIsRunning().observe(getViewLifecycleOwner(), running -> {
            binding.btnStartPause.setText(running ? "Pause" : "Start");
            updateStartButtonEnabledState();
        });
        viewModel.getSessionType().observe(getViewLifecycleOwner(), type -> {
            updateSessionLabel();
            updateStartButtonEnabledState();
        });
        viewModel.getSessionCount().observe(getViewLifecycleOwner(), count ->
                binding.tvSessionCount.setText("Session " + count + " of 4"));
        viewModel.getTotalMinutesToday().observe(getViewLifecycleOwner(), mins ->
                binding.tvMinutesToday.setText(String.valueOf(mins != null ? mins : 0)));
        viewModel.getSessionsCompletedToday().observe(getViewLifecycleOwner(), count ->
                binding.tvSessionsCompleted.setText(String.valueOf(count != null ? count : 0)));
    }

    private void setupTipCard() {
        // Dismiss tip card using SharedPreferences so it stays hidden after first dismissal
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("timer_tip_dismissed", false)) {
            binding.cardTip.setVisibility(android.view.View.GONE);
        }
        binding.tvDismissTip.setOnClickListener(v -> {
            binding.cardTip.setVisibility(android.view.View.GONE);
            prefs.edit().putBoolean("timer_tip_dismissed", true).apply();
        });
    }

    private void setupControls() {
        binding.btnStartPause.setOnClickListener(v -> {
            if (!serviceBound || timerService == null) {
                Toast.makeText(requireContext(), getString(R.string.timer_not_ready), Toast.LENGTH_SHORT).show();
                return;
            }

            Boolean running = viewModel.getIsRunning().getValue();
            if (running == null || !running) {
                Integer type = viewModel.getSessionType().getValue();
                boolean focusSession = type == null || type == TimerViewModel.TYPE_FOCUS;
                if (focusSession && !validateModuleSelection()) {
                    return;
                }

                // First start
                String manualSubject = binding.etSubject.getText() != null
                        ? binding.etSubject.getText().toString().trim() : "";
                if (focusSession && (manualSubject == null || manualSubject.trim().isEmpty())) {
                    manualSubject = selectedModuleSubject;
                    if (manualSubject != null && !manualSubject.trim().isEmpty()) {
                        binding.etSubject.setText(manualSubject);
                    }
                }

                viewModel.setSubject(manualSubject);
                if (focusSession) {
                    viewModel.setActiveModule(selectedModuleId, selectedModuleTitle, selectedModuleSubject);
                }
                viewModel.onStart(timerService);
                syncTimerUiFromService();
                startUiSyncLoop();
                updateStartButtonEnabledState();
                pendingTaskAutoStart = false;
                pendingTaskAutoStartConsumed = true;
            } else {
                viewModel.onStartPause(timerService);
                if (Boolean.TRUE.equals(viewModel.getIsRunning().getValue())) {
                    startUiSyncLoop();
                } else {
                    stopUiSyncLoop();
                }
                updateStartButtonEnabledState();
            }
        });

        binding.btnReset.setOnClickListener(v -> {
            if (serviceBound && timerService != null) {
                viewModel.onReset(timerService);
                syncTimerUiFromService();
                stopUiSyncLoop();
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            if (serviceBound && timerService != null) {
                viewModel.onSkip(timerService);
                syncTimerUiFromService();
                stopUiSyncLoop();
                updateSessionLabel();
            }
        });

        binding.chipGroupSession.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            int type;
            if (id == binding.chipFocus.getId()) type = TimerViewModel.TYPE_FOCUS;
            else if (id == binding.chipShortBreak.getId()) type = TimerViewModel.TYPE_SHORT_BREAK;
            else type = TimerViewModel.TYPE_LONG_BREAK;
            viewModel.setSessionType(type, serviceBound ? timerService : null);
            updateSessionLabel();
            updateStartButtonEnabledState();
        });
    }

    private void syncTimerUiFromService() {
        if (!serviceBound || timerService == null || viewModel == null) {
            return;
        }

        long left = timerService.getMillisLeft();
        long total = timerService.getTotalMillis();
        int progress = total > 0 ? (int) ((left * 100) / total) : 0;
        viewModel.onTick(left, progress);

        FocusTimerSessionStore.Snapshot state = FocusTimerSessionStore.getSnapshot(requireContext());
        if (state != null && state.active) {
            if (state.moduleTitle != null && !state.moduleTitle.trim().isEmpty()) {
                binding.tvActiveModule.setText("Module: " + state.moduleTitle.trim());
            } else if (state.subject != null && !state.subject.trim().isEmpty()) {
                binding.tvActiveModule.setText("Module: " + state.subject.trim());
            }
        }
    }

    private void startUiSyncLoop() {
        uiSyncHandler.removeCallbacks(timerUiSyncRunnable);
        uiSyncHandler.post(timerUiSyncRunnable);
    }

    private void stopUiSyncLoop() {
        uiSyncHandler.removeCallbacks(timerUiSyncRunnable);
    }

    private void prefillSubjectFromTaskStarter() {
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences(PREFS_STUDYSYNC, Context.MODE_PRIVATE);
        pendingTaskSubject = safeTrim(prefs.getString(PREF_PENDING_TIMER_SUBJECT, ""));
        pendingTaskId = safeTrim(prefs.getString(PREF_PENDING_TIMER_TASK_ID, ""));
        pendingTaskTitle = safeTrim(prefs.getString(PREF_PENDING_TIMER_TASK_TITLE, ""));
        pendingTaskPriority = safeTrim(prefs.getString(PREF_PENDING_TIMER_TASK_PRIORITY, ""));
        pendingTaskDueMillis = prefs.getLong(PREF_PENDING_TIMER_TASK_DUE, 0L);
        pendingTaskAutoStart = prefs.getBoolean(PREF_PENDING_TIMER_AUTO_START, false);
        pendingTaskAutoStartConsumed = false;

        if (!pendingTaskSubject.isEmpty()) {
            String current = binding.etSubject.getText() != null
                    ? binding.etSubject.getText().toString().trim()
                    : "";
            if (current.isEmpty()) {
                binding.etSubject.setText(pendingTaskSubject);
            }
        }

        prefs.edit()
                .remove(PREF_PENDING_TIMER_SUBJECT)
                .remove(PREF_PENDING_TIMER_TASK_ID)
                .remove(PREF_PENDING_TIMER_TASK_TITLE)
                .remove(PREF_PENDING_TIMER_TASK_PRIORITY)
                .remove(PREF_PENDING_TIMER_TASK_DUE)
                .remove(PREF_PENDING_TIMER_AUTO_START)
                .apply();
    }

    private void loadFocusModules(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            binding.tilFocusModule.setEnabled(false);
            return;
        }

        StudyModuleRepository repository = new StudyModuleRepository(requireContext());
        repository.syncStudyModulesFromFirestore(userId);
        repository.getAllStudyModulesForUser(userId).observe(getViewLifecycleOwner(), modules -> {
            focusModules.clear();
            List<String> labels = new ArrayList<>();

            if (modules != null) {
                for (StudyModule module : modules) {
                    if (module == null || module.isArchived() || !module.isUnlocked()) {
                        continue;
                    }
                    focusModules.add(module);
                    labels.add(formatModuleOption(module));
                }
            }

            binding.tilFocusModule.setEnabled(!focusModules.isEmpty());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    labels
            );
            binding.actvFocusModule.setAdapter(adapter);

            binding.actvFocusModule.setOnItemClickListener((parent, view, position, id) -> {
                if (position < 0 || position >= focusModules.size()) {
                    return;
                }
                applySelectedModule(focusModules.get(position), true);
            });

            restoreModuleSelectionFromState();
            if (selectedModuleId == null || selectedModuleId.trim().isEmpty()) {
                tryAutoSelectModuleFromPendingContext();
            }
            if (focusModules.isEmpty()) {
                binding.actvFocusModule.setText("", false);
                selectedModuleId = "";
                selectedModuleTitle = "";
                selectedModuleSubject = "";
                updateActiveModuleLabel();
            }
            updateStartButtonEnabledState();
            maybeAutoStartFromTaskContext();
        });
    }

    private void tryAutoSelectModuleFromPendingContext() {
        if (focusModules.isEmpty()) {
            return;
        }

        String normalizedSubject = normalizeForMatch(pendingTaskSubject);
        String normalizedTaskTitle = normalizeForMatch(pendingTaskTitle);

        if (normalizedSubject.isEmpty() && normalizedTaskTitle.isEmpty()) {
            return;
        }

        StudyModule bestMatch = null;
        int bestScore = 0;
        for (StudyModule module : focusModules) {
            if (module == null) {
                continue;
            }

            int score = scoreModuleMatch(module, normalizedSubject, normalizedTaskTitle);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = module;
            }
        }

        if (bestMatch != null && bestScore > 0) {
            applySelectedModule(bestMatch, true);
            return;
        }

        if (focusModules.size() == 1) {
            applySelectedModule(focusModules.get(0), true);
        }
    }

    private int scoreModuleMatch(StudyModule module, String normalizedSubject, String normalizedTaskTitle) {
        if (module == null) {
            return 0;
        }

        String moduleSubject = normalizeForMatch(module.getSubject());
        String moduleTitle = normalizeForMatch(module.getTitle());
        String moduleTopic = normalizeForMatch(module.getTopic());

        int score = 0;
        if (!normalizedSubject.isEmpty()) {
            if (!moduleSubject.isEmpty() && moduleSubject.equals(normalizedSubject)) {
                score += 9;
            }
            if (!moduleSubject.isEmpty()
                    && (moduleSubject.contains(normalizedSubject) || normalizedSubject.contains(moduleSubject))) {
                score += 5;
            }
            if (!moduleTitle.isEmpty() && moduleTitle.contains(normalizedSubject)) {
                score += 3;
            }
            if (!moduleTopic.isEmpty() && moduleTopic.contains(normalizedSubject)) {
                score += 2;
            }
        }

        if (!normalizedTaskTitle.isEmpty()) {
            if (!moduleTitle.isEmpty() && moduleTitle.equals(normalizedTaskTitle)) {
                score += 8;
            }
            if (!moduleTitle.isEmpty()
                    && (moduleTitle.contains(normalizedTaskTitle) || normalizedTaskTitle.contains(moduleTitle))) {
                score += 5;
            }
            if (!moduleTopic.isEmpty()
                    && (moduleTopic.contains(normalizedTaskTitle) || normalizedTaskTitle.contains(moduleTopic))) {
                score += 3;
            }
            if (containsLongToken(moduleTitle, normalizedTaskTitle)) {
                score += 2;
            }
            if (containsLongToken(moduleTopic, normalizedTaskTitle)) {
                score += 2;
            }
        }

        return score;
    }

    private boolean containsLongToken(String haystack, String phrase) {
        if (haystack == null || haystack.trim().isEmpty() || phrase == null || phrase.trim().isEmpty()) {
            return false;
        }

        String[] tokens = phrase.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token != null && token.length() >= 4 && haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForMatch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }

    private void updatePendingTaskContextUi() {
        if (binding == null) {
            return;
        }

        boolean hasContext = (pendingTaskTitle != null && !pendingTaskTitle.trim().isEmpty())
                || pendingTaskDueMillis > 0L
                || (pendingTaskPriority != null && !pendingTaskPriority.trim().isEmpty());
        if (!hasContext) {
            binding.cardTaskContext.setVisibility(View.GONE);
            return;
        }

        String title = (pendingTaskTitle != null && !pendingTaskTitle.trim().isEmpty())
                ? pendingTaskTitle.trim()
                : "Planner task";
        String dueLabel = pendingTaskDueMillis > 0L
                ? new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
                .format(new Date(pendingTaskDueMillis))
                : "No deadline set";
        String priorityLabel = (pendingTaskPriority != null && !pendingTaskPriority.trim().isEmpty())
                ? pendingTaskPriority.trim().toUpperCase(Locale.US)
                : "MEDIUM";
        String autoStartLabel = pendingTaskAutoStart ? "\nAuto-start enabled once module is linked" : "";

        String taskLabel = "Linked task: " + title + "\nDue: " + dueLabel + " • Priority: " + priorityLabel + autoStartLabel;
        binding.tvFocusTaskContext.setText(taskLabel);
        binding.cardTaskContext.setVisibility(View.VISIBLE);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void restoreModuleSelectionFromState() {
        FocusTimerSessionStore.Snapshot state = FocusTimerSessionStore.getSnapshot(requireContext());
        if (state == null || state.moduleId == null || state.moduleId.trim().isEmpty()) {
            return;
        }

        for (StudyModule module : focusModules) {
            if (module != null && state.moduleId.equals(module.getModuleId())) {
                applySelectedModule(module, false);
                return;
            }
        }
    }

    private void applySelectedModule(StudyModule module, boolean updateInputText) {
        if (module == null) {
            return;
        }

        selectedModuleId = module.getModuleId() != null ? module.getModuleId().trim() : "";
        selectedModuleTitle = module.getTitle() != null ? module.getTitle().trim() : "";
        selectedModuleSubject = module.getSubject() != null ? module.getSubject().trim() : "";

        if (updateInputText) {
            binding.actvFocusModule.setText(formatModuleOption(module), false);
        }

        String subjectInput = binding.etSubject.getText() != null
                ? binding.etSubject.getText().toString().trim()
                : "";
        if (subjectInput.isEmpty() && !selectedModuleSubject.isEmpty()) {
            binding.etSubject.setText(selectedModuleSubject);
        }

        binding.tilFocusModule.setError(null);
        updateActiveModuleLabel();
        updateStartButtonEnabledState();
        maybeAutoStartFromTaskContext();
    }

    private boolean validateModuleSelection() {
        if (focusModules.isEmpty()) {
            binding.tilFocusModule.setError(null);
            binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_no_modules));
            Toast.makeText(requireContext(), getString(R.string.timer_no_unlocked_modules), Toast.LENGTH_LONG).show();
            return false;
        }

        if (selectedModuleId == null || selectedModuleId.trim().isEmpty()) {
            binding.tilFocusModule.setError(getString(R.string.timer_error_select_module));
            binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_select_module));
            binding.actvFocusModule.requestFocus();
            return false;
        }

        binding.tilFocusModule.setError(null);
        binding.tilFocusModule.setHelperText(null);
        return true;
    }

    private boolean isFocusSessionSelected() {
        Integer type = viewModel != null ? viewModel.getSessionType().getValue() : null;
        return type == null || type == TimerViewModel.TYPE_FOCUS;
    }

    private void updateStartButtonEnabledState() {
        if (binding == null || viewModel == null) {
            return;
        }

        boolean serviceReady = serviceBound && timerService != null;
        boolean running = Boolean.TRUE.equals(viewModel.getIsRunning().getValue());
        boolean focusSession = isFocusSessionSelected();

        updateModuleSelectionHelperText(focusSession, serviceReady);

        boolean hasSelectedModule = selectedModuleId != null && !selectedModuleId.trim().isEmpty();
        boolean focusReady = !focusModules.isEmpty() && hasSelectedModule;

        boolean enabled = serviceReady && (running || !focusSession || focusReady);
        binding.btnStartPause.setEnabled(enabled);
        binding.btnStartPause.setAlpha(enabled ? 1.0f : 0.6f);
    }

    private void updateModuleSelectionHelperText(boolean focusSession, boolean serviceReady) {
        if (binding == null) {
            return;
        }

        if (!focusSession) {
            binding.tilFocusModule.setError(null);
            binding.tilFocusModule.setHelperText(null);
            return;
        }

        if (!serviceReady) {
            binding.tilFocusModule.setError(null);
            binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_wait_service));
            return;
        }

        if (focusModules.isEmpty()) {
            binding.tilFocusModule.setError(null);
            binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_no_modules));
            return;
        }

        if (selectedModuleId == null || selectedModuleId.trim().isEmpty()) {
            binding.tilFocusModule.setError(null);
            if (pendingTaskTitle != null && !pendingTaskTitle.trim().isEmpty()) {
                binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_select_module_linked_task));
            } else {
                binding.tilFocusModule.setHelperText(getString(R.string.timer_helper_select_module));
            }
            return;
        }

        binding.tilFocusModule.setError(null);
        binding.tilFocusModule.setHelperText(null);
    }

    private void maybeAutoStartFromTaskContext() {
        if (!pendingTaskAutoStart || pendingTaskAutoStartConsumed) {
            return;
        }
        if (binding == null || viewModel == null || !serviceBound || timerService == null) {
            return;
        }

        if (!isFocusSessionSelected()) {
            binding.chipGroupSession.check(binding.chipFocus.getId());
        }

        if (focusModules.isEmpty()) {
            return;
        }

        if (selectedModuleId == null || selectedModuleId.trim().isEmpty()) {
            tryAutoSelectModuleFromPendingContext();
        }

        if (selectedModuleId == null || selectedModuleId.trim().isEmpty()) {
            return;
        }

        if (Boolean.TRUE.equals(viewModel.getIsRunning().getValue())) {
            pendingTaskAutoStart = false;
            pendingTaskAutoStartConsumed = true;
            return;
        }

        String subjectInput = binding.etSubject.getText() != null
                ? binding.etSubject.getText().toString().trim()
                : "";
        if (subjectInput.isEmpty()) {
            if (selectedModuleSubject != null && !selectedModuleSubject.trim().isEmpty()) {
                subjectInput = selectedModuleSubject.trim();
            } else {
                subjectInput = pendingTaskSubject != null ? pendingTaskSubject.trim() : "";
            }
            if (!subjectInput.isEmpty()) {
                binding.etSubject.setText(subjectInput);
            }
        }

        viewModel.setSubject(subjectInput);
        viewModel.setActiveModule(selectedModuleId, selectedModuleTitle, selectedModuleSubject);
        viewModel.onStart(timerService);
        syncTimerUiFromService();
        startUiSyncLoop();
        updateStartButtonEnabledState();

        pendingTaskAutoStart = false;
        pendingTaskAutoStartConsumed = true;

        String linkedTaskLabel = pendingTaskTitle != null && !pendingTaskTitle.trim().isEmpty()
                ? pendingTaskTitle.trim()
                : "planner task";
        Toast.makeText(requireContext(), "Focus started for " + linkedTaskLabel, Toast.LENGTH_SHORT).show();
    }

    private void updateActiveModuleLabel() {
        String title = selectedModuleTitle != null ? selectedModuleTitle.trim() : "";
        if (title.isEmpty()) {
            binding.tvActiveModule.setText("Module: Not selected");
            return;
        }
        binding.tvActiveModule.setText("Module: " + title);
    }

    private String formatModuleOption(StudyModule module) {
        if (module == null) {
            return "";
        }
        String title = module.getTitle() != null ? module.getTitle().trim() : "Untitled Module";
        String subject = module.getSubject() != null ? module.getSubject().trim() : "";
        if (subject.isEmpty()) {
            return title;
        }
        return title + " • " + subject;
    }

    private void updateSessionLabel() {
        Integer type = viewModel.getSessionType().getValue();
        if (type == null) return;
        switch (type) {
            case TimerViewModel.TYPE_SHORT_BREAK:
                binding.tvSessionLabel.setText("Short Break");
                binding.tvActiveModule.setText("Module: Break");
                break;
            case TimerViewModel.TYPE_LONG_BREAK:
                binding.tvSessionLabel.setText("Long Break");
                binding.tvActiveModule.setText("Module: Break");
                break;
            default:
                binding.tvSessionLabel.setText("Focus Session");
                updateActiveModuleLabel();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_TICK);
        filter.addAction(TimerService.ACTION_FINISH);
        ContextCompat.registerReceiver(requireContext(), timerReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUiSyncLoop();
        requireContext().unregisterReceiver(timerReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopUiSyncLoop();
        if (serviceBound) {
            requireContext().unbindService(connection);
            serviceBound = false;
        }
        binding = null;
    }
}
