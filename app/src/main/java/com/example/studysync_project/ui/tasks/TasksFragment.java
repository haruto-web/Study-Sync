package com.example.studysync_project.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.databinding.DialogCreateTaskBinding;
import com.example.studysync_project.databinding.FragmentTasksBinding;
import com.example.studysync_project.utils.IdUtil;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private static final String PREFS_PLANNER_NOTES = "planner_daily_notes";
    private static final String PREFS_STUDYSYNC = "studysync_prefs";
    private static final String PREF_PENDING_TIMER_SUBJECT = "pending_timer_subject";
    private static final String PREF_PENDING_TIMER_TASK_ID = "pending_timer_task_id";
    private static final String PREF_PENDING_TIMER_TASK_TITLE = "pending_timer_task_title";
    private static final String PREF_PENDING_TIMER_TASK_PRIORITY = "pending_timer_task_priority";
    private static final String PREF_PENDING_TIMER_TASK_DUE = "pending_timer_task_due";
    private static final String PREF_PENDING_TIMER_AUTO_START = "pending_timer_auto_start";
    private static final String PREF_TASKS_FOCUS_HELPER_EXPANDED = "tasks_focus_helper_expanded";
    private static final String PREF_TASKS_CONCENTRATION_EXPANDED = "tasks_concentration_expanded";
    private static final String PREF_TASKS_DAILY_NOTES_EXPANDED = "tasks_daily_notes_expanded";
    private static final int MAX_DAILY_NOTE_LENGTH = 1200;
    private static final int DEFAULT_DUE_HOUR = 19;
    private static final int DEFAULT_DUE_MINUTE = 0;
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private FragmentTasksBinding binding;
    private TasksViewModel viewModel;
    private TaskAdapter adapter;
    private String userId;
    private final Calendar selectedStartDate = Calendar.getInstance();
    private final Calendar selectedDueDate = Calendar.getInstance();
    private final Calendar plannerDate = Calendar.getInstance();
    private final List<Task> cachedTasks = new ArrayList<>();
    private int selectedFilterTab = 0;
    private String selectedPriorityFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel = new ViewModelProvider(this, new androidx.lifecycle.ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TasksViewModel(requireContext());
            }
        }).get(TasksViewModel.class);

        adapter = new TaskAdapter(this);
        binding.rvTasks.setAdapter(adapter);
        setupPlannerUi();

        viewModel.getAllTasksForUser(userId).observe(getViewLifecycleOwner(), tasks -> {
            cachedTasks.clear();
            if (tasks != null) {
                cachedTasks.addAll(tasks);
            }
            renderTasksForActiveFilter();
        });

        binding.tabsFilter.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                selectedFilterTab = tab.getPosition();
                renderTasksForActiveFilter();
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
            }
        });

        binding.fabAddTask.setOnClickListener(v -> showCreateTaskDialog(null));
    }

    private void setupPlannerUi() {
        plannerDate.setTimeInMillis(System.currentTimeMillis());
        normalizeToDayStart(plannerDate);

        binding.calendarPlanner.setDate(plannerDate.getTimeInMillis(), false, true);
        binding.calendarPlanner.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            plannerDate.set(year, month, dayOfMonth, 0, 0, 0);
            plannerDate.set(Calendar.MILLISECOND, 0);
            onPlannerDateChanged();
        });

        binding.switchDayFilter.setOnCheckedChangeListener((buttonView, isChecked) -> renderTasksForActiveFilter());

        binding.btnJumpToday.setOnClickListener(v -> {
            plannerDate.setTimeInMillis(System.currentTimeMillis());
            normalizeToDayStart(plannerDate);
            binding.calendarPlanner.setDate(plannerDate.getTimeInMillis(), true, true);
            onPlannerDateChanged();
        });

        binding.btnSaveDailyNote.setOnClickListener(v -> saveDailyNote());
        binding.btnSuggestFocusTask.setOnClickListener(v -> suggestFocusTaskForSelectedDay());

        binding.chipGroupPriorityFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                selectedPriorityFilter = "ALL";
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_priority_high) {
                    selectedPriorityFilter = "HIGH";
                } else if (checkedId == R.id.chip_priority_medium) {
                    selectedPriorityFilter = "MEDIUM";
                } else if (checkedId == R.id.chip_priority_low) {
                    selectedPriorityFilter = "LOW";
                } else {
                    selectedPriorityFilter = "ALL";
                }
            }
            renderTasksForActiveFilter();
        });

        setupCollapsibleSections();

        onPlannerDateChanged();
    }

    private void setupCollapsibleSections() {
        if (binding == null) {
            return;
        }

        SharedPreferences prefs = uiPrefs();
        boolean focusHelperExpanded = prefs.getBoolean(PREF_TASKS_FOCUS_HELPER_EXPANDED, false);
        boolean concentrationExpanded = prefs.getBoolean(PREF_TASKS_CONCENTRATION_EXPANDED, false);
        boolean dailyNotesExpanded = prefs.getBoolean(PREF_TASKS_DAILY_NOTES_EXPANDED, false);

        binding.btnToggleFocusHelper.setOnClickListener(v ->
                setFocusHelperExpanded(binding.layoutFocusHelperContent.getVisibility() != View.VISIBLE, true)
        );

        binding.btnToggleConcentrationDetails.setOnClickListener(v ->
                setConcentrationDetailsExpanded(
                        binding.layoutCalendarConcentrationDetails.getVisibility() != View.VISIBLE,
                        true
                )
        );

        binding.btnToggleDailyNotes.setOnClickListener(v ->
                setDailyNotesExpanded(binding.layoutDailyNotesContent.getVisibility() != View.VISIBLE, true)
        );

        setFocusHelperExpanded(focusHelperExpanded, false);
        setConcentrationDetailsExpanded(concentrationExpanded, false);
        setDailyNotesExpanded(dailyNotesExpanded, false);
    }

    private void setFocusHelperExpanded(boolean expanded, boolean persist) {
        if (binding == null) {
            return;
        }

        binding.layoutFocusHelperContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.btnToggleFocusHelper.setText(expanded ? "Hide focus helper" : "Show focus helper");

        if (persist) {
            uiPrefs().edit().putBoolean(PREF_TASKS_FOCUS_HELPER_EXPANDED, expanded).apply();
        }
    }

    private void setConcentrationDetailsExpanded(boolean expanded, boolean persist) {
        if (binding == null) {
            return;
        }

        binding.layoutCalendarConcentrationDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.btnToggleConcentrationDetails.setText(
                expanded ? "Hide concentration details" : "Show concentration details"
        );

        if (persist) {
            uiPrefs().edit().putBoolean(PREF_TASKS_CONCENTRATION_EXPANDED, expanded).apply();
        }
    }

    private void setDailyNotesExpanded(boolean expanded, boolean persist) {
        if (binding == null) {
            return;
        }

        binding.layoutDailyNotesContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.btnToggleDailyNotes.setText(expanded ? "Hide notes" : "Show notes");

        if (persist) {
            uiPrefs().edit().putBoolean(PREF_TASKS_DAILY_NOTES_EXPANDED, expanded).apply();
        }
    }

    private void onPlannerDateChanged() {
        loadDailyNoteForSelectedDate();
        renderTasksForActiveFilter();
    }

    private void showCreateTaskDialog(@Nullable Task existingTask) {
        DialogCreateTaskBinding dialogBinding = DialogCreateTaskBinding.inflate(LayoutInflater.from(requireContext()));
        selectedStartDate.setTimeInMillis(plannerDate.getTimeInMillis());
        normalizeToDayStart(selectedStartDate);
        selectedDueDate.setTimeInMillis(selectedStartDate.getTimeInMillis());
        selectedDueDate.add(Calendar.DAY_OF_YEAR, 3);
        applyDefaultDueTime(selectedDueDate);
        updateTimelineButtons(dialogBinding);

        if (existingTask != null) {
            dialogBinding.etTitle.setText(existingTask.getTitle());
            dialogBinding.etDescription.setText(existingTask.getDescription());
            dialogBinding.etCategory.setText(existingTask.getCategory());

            long existingStart = existingTask.getStartDate() > 0L
                    ? existingTask.getStartDate()
                    : (existingTask.getCreatedAt() > 0L ? existingTask.getCreatedAt() : existingTask.getDueDate());
            selectedStartDate.setTimeInMillis(existingStart);
            normalizeToDayStart(selectedStartDate);

            long existingDue = existingTask.getDueDate() > 0L
                    ? existingTask.getDueDate()
                    : selectedStartDate.getTimeInMillis();
            selectedDueDate.setTimeInMillis(existingDue);
            if (isMidnight(selectedDueDate)) {
                applyDefaultDueTime(selectedDueDate);
            }
            if (selectedDueDate.before(selectedStartDate)) {
                selectedDueDate.setTimeInMillis(selectedStartDate.getTimeInMillis());
                applyDefaultDueTime(selectedDueDate);
            }

            updateTimelineButtons(dialogBinding);
            String existingPriority = existingTask.getPriority() != null
                    ? existingTask.getPriority().trim().toUpperCase(Locale.US)
                    : "MEDIUM";
            switch (existingPriority) {
                case "LOW":
                    dialogBinding.chipLow.setChecked(true);
                    break;
                case "HIGH":
                    dialogBinding.chipHigh.setChecked(true);
                    break;
                default:
                    dialogBinding.chipMedium.setChecked(true);
            }
        }

        dialogBinding.btnPickStartDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(),
                    (dp, y, m, d) -> {
                        selectedStartDate.set(y, m, d, 0, 0, 0);
                        selectedStartDate.set(Calendar.MILLISECOND, 0);
                        if (selectedStartDate.after(selectedDueDate)) {
                            selectedDueDate.setTimeInMillis(selectedStartDate.getTimeInMillis());
                            applyDefaultDueTime(selectedDueDate);
                        }
                        updateTimelineButtons(dialogBinding);
                    },
                    selectedStartDate.get(Calendar.YEAR),
                    selectedStartDate.get(Calendar.MONTH),
                    selectedStartDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.btnPickDeadline.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(),
                    (dp, y, m, d) -> {
                        int hour = selectedDueDate.get(Calendar.HOUR_OF_DAY);
                        int minute = selectedDueDate.get(Calendar.MINUTE);
                        selectedDueDate.set(y, m, d, hour, minute, 0);
                        selectedDueDate.set(Calendar.MILLISECOND, 0);
                        if (selectedDueDate.before(selectedStartDate)) {
                            selectedDueDate.setTimeInMillis(selectedStartDate.getTimeInMillis());
                            applyDefaultDueTime(selectedDueDate);
                        }
                        updateTimelineButtons(dialogBinding);
                    },
                    selectedDueDate.get(Calendar.YEAR),
                    selectedDueDate.get(Calendar.MONTH),
                    selectedDueDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        dialogBinding.btnPickDeadlineTime.setOnClickListener(v -> {
            boolean use24Hour = DateFormat.is24HourFormat(requireContext());
            new TimePickerDialog(requireContext(),
                    (view, hourOfDay, minute) -> {
                        selectedDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDueDate.set(Calendar.MINUTE, minute);
                        selectedDueDate.set(Calendar.SECOND, 0);
                        selectedDueDate.set(Calendar.MILLISECOND, 0);
                        if (normalizeDayMillis(selectedDueDate.getTimeInMillis())
                                < normalizeDayMillis(selectedStartDate.getTimeInMillis())) {
                            selectedDueDate.setTimeInMillis(selectedStartDate.getTimeInMillis());
                            selectedDueDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            selectedDueDate.set(Calendar.MINUTE, minute);
                        }
                        updateTimelineButtons(dialogBinding);
                    },
                    selectedDueDate.get(Calendar.HOUR_OF_DAY),
                    selectedDueDate.get(Calendar.MINUTE),
                    use24Hour
            ).show();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingTask == null
                        ? getString(R.string.task_dialog_new_title)
                        : getString(R.string.task_dialog_edit_title))
                .setView(dialogBinding.getRoot())
                .setPositiveButton(R.string.action_save, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!validateTaskDialogInput(dialogBinding)) {
                    return;
                }

                String title = dialogBinding.etTitle.getText() != null
                        ? dialogBinding.etTitle.getText().toString().trim() : "";
                String description = dialogBinding.etDescription.getText() != null
                        ? dialogBinding.etDescription.getText().toString().trim() : "";
                String categoryInput = dialogBinding.etCategory.getText() != null
                        ? dialogBinding.etCategory.getText().toString().trim() : "";
                String category = categoryInput.isEmpty() ? "General" : categoryInput;
                String priority = getPriority(dialogBinding);
                long startMillis = selectedStartDate.getTimeInMillis();
                long dueMillis = selectedDueDate.getTimeInMillis();

                if (existingTask == null) {
                    Task task = new Task(userId, title, description,
                            startMillis, dueMillis, priority, category);
                    task.setTaskId(IdUtil.generateId("task"));
                    viewModel.createTask(task, userId);
                    showPlannerMessage(R.string.task_created);
                } else {
                    existingTask.setTitle(title);
                    existingTask.setDescription(description);
                    existingTask.setCategory(category);
                    existingTask.setPriority(priority);
                    existingTask.setStartDate(startMillis);
                    existingTask.setDueDate(dueMillis);
                    viewModel.updateTask(existingTask);
                    showPlannerMessage(R.string.task_updated);
                }

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private boolean validateTaskDialogInput(DialogCreateTaskBinding dialogBinding) {
        clearTaskDialogErrors(dialogBinding);

        String title = dialogBinding.etTitle.getText() != null
                ? dialogBinding.etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            dialogBinding.tilTitle.setError(getString(R.string.task_validation_title_required));
            dialogBinding.etTitle.requestFocus();
            return false;
        }
        if (title.length() > 80) {
            dialogBinding.tilTitle.setError(getString(R.string.task_validation_title_max));
            dialogBinding.etTitle.requestFocus();
            return false;
        }

        Calendar startDay = (Calendar) selectedStartDate.clone();
        normalizeToDayStart(startDay);

        Calendar dueDay = (Calendar) selectedDueDate.clone();
        normalizeToDayStart(dueDay);

        if (dueDay.before(startDay)) {
            showPlannerMessage(R.string.task_validation_deadline_before_start);
            return false;
        }

        String description = dialogBinding.etDescription.getText() != null
                ? dialogBinding.etDescription.getText().toString().trim() : "";
        if (description.length() > 280) {
            dialogBinding.tilDescription.setError(getString(R.string.task_validation_description_max));
            dialogBinding.etDescription.requestFocus();
            return false;
        }

        return true;
    }

    private static void clearTaskDialogErrors(DialogCreateTaskBinding dialogBinding) {
        dialogBinding.tilTitle.setError(null);
        dialogBinding.tilDescription.setError(null);
    }

    private void showPlannerMessage(@StringRes int messageResId) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), messageResId, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(requireContext(), getString(messageResId), Toast.LENGTH_SHORT).show();
    }

    private void renderTasksForActiveFilter() {
        if (binding == null) {
            return;
        }

        boolean selectedDayOnly = binding.switchDayFilter.isChecked();
        long selectedDayMillis = plannerDate.getTimeInMillis();
        long priorityContextDayMillis = selectedDayOnly ? selectedDayMillis : System.currentTimeMillis();
        String activePriorityFilter = selectedPriorityFilter != null ? selectedPriorityFilter : "ALL";

        List<Task> filtered = new ArrayList<>();
        for (Task task : cachedTasks) {
            if (task == null) {
                continue;
            }

            if (selectedDayOnly && !isDayWithinTaskWindow(task, selectedDayMillis)) {
                continue;
            }
            if (selectedFilterTab == 1 && task.isCompleted()) {
                continue;
            }
            if (selectedFilterTab == 2 && !task.isCompleted()) {
                continue;
            }
            String effectivePriority = effectivePriorityForDay(task, priorityContextDayMillis);
            if (!"ALL".equalsIgnoreCase(activePriorityFilter)
                    && !activePriorityFilter.equalsIgnoreCase(effectivePriority)) {
                continue;
            }
            filtered.add(task);
        }

        final long sortingDay = priorityContextDayMillis;
        Collections.sort(filtered, (left, right) -> {
            if (left.isCompleted() != right.isCompleted()) {
                return left.isCompleted() ? 1 : -1;
            }

            int priorityCompare = Integer.compare(
                    priorityRank(effectivePriorityForDay(left, sortingDay)),
                    priorityRank(effectivePriorityForDay(right, sortingDay))
            );
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            int dueCompare = Long.compare(left.getDueDate(), right.getDueDate());
            if (dueCompare != 0) {
                return dueCompare;
            }

            return Long.compare(right.getCreatedAt(), left.getCreatedAt());
        });

        adapter.setReferenceDayMillis(priorityContextDayMillis);
        adapter.submitList(filtered);
        boolean isEmpty = filtered.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        updatePlannerDayStats();
    }

    private void updatePlannerDayStats() {
        if (binding == null) {
            return;
        }

        long dayMillis = plannerDate.getTimeInMillis();
        int trackedCount = 0;
        int completedCount = 0;
        int dueTodayCount = 0;
        int highestPriorityRank = 3;
        for (Task task : cachedTasks) {
            if (task == null || !isDayWithinTaskWindow(task, dayMillis)) {
                continue;
            }
            trackedCount++;
            if (task.isCompleted()) {
                completedCount++;
                continue;
            }

            String effectivePriority = effectivePriorityForDay(task, dayMillis);
            highestPriorityRank = Math.min(highestPriorityRank, priorityRank(effectivePriority));
            if (isSameDay(dayMillis, resolveTaskDueMillis(task))) {
                dueTodayCount++;
            }
        }

        int activeCount = Math.max(0, trackedCount - completedCount);
        int completionPercent = trackedCount == 0
                ? 0
                : Math.round((completedCount * 100f) / trackedCount);

        binding.tvPlannerSelectedDate.setText(formatPlannerDate(dayMillis));
        binding.tvPlannerDayStats.setText(
            trackedCount + " tracked • " + activeCount + " active • " + dueTodayCount + " due today"
        );
        binding.progressDayCompletion.setProgress(completionPercent);
        String highestPriorityLabel = highestPriorityRank == 0
            ? "High"
            : (highestPriorityRank == 1 ? "Medium" : "Low");
        binding.tvDayCompletionLabel.setText(
            "Completion " + completionPercent + "% • Highest " + highestPriorityLabel
        );
        updateCalendarIndicators();
    }

    private String formatPlannerDate(long millis) {
        String formatted = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
                .format(millis);
        if (isSameDay(millis, System.currentTimeMillis())) {
            return "Today • " + formatted;
        }
        return formatted;
    }

    private void saveDailyNote() {
        if (binding == null) {
            return;
        }

        String note = binding.etDailyNotes.getText() != null
                ? binding.etDailyNotes.getText().toString()
                : "";
        if (note.length() > MAX_DAILY_NOTE_LENGTH) {
            Toast.makeText(
                    requireContext(),
                    "Keep daily notes under " + MAX_DAILY_NOTE_LENGTH + " characters",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        plannerNotesPrefs()
                .edit()
                .putString(noteKeyForDate(plannerDate.getTimeInMillis()), note)
                .apply();
        Toast.makeText(requireContext(), "Daily note saved", Toast.LENGTH_SHORT).show();
    }

    private void loadDailyNoteForSelectedDate() {
        if (binding == null) {
            return;
        }

        String note = plannerNotesPrefs().getString(noteKeyForDate(plannerDate.getTimeInMillis()), "");
        String current = binding.etDailyNotes.getText() != null
                ? binding.etDailyNotes.getText().toString()
                : "";
        if (current.equals(note)) {
            return;
        }

        binding.etDailyNotes.setText(note);
        if (binding.etDailyNotes.getText() != null) {
            binding.etDailyNotes.setSelection(binding.etDailyNotes.getText().length());
        }
    }

    private SharedPreferences plannerNotesPrefs() {
        return requireContext().getSharedPreferences(PREFS_PLANNER_NOTES, Context.MODE_PRIVATE);
    }

    private SharedPreferences uiPrefs() {
        return requireContext().getSharedPreferences(PREFS_STUDYSYNC, Context.MODE_PRIVATE);
    }

    private String noteKeyForDate(long millis) {
        String uid = userId != null && !userId.trim().isEmpty() ? userId.trim() : "guest";
        String dayKey = new SimpleDateFormat("yyyyMMdd", Locale.US).format(millis);
        return uid + "_" + dayKey;
    }

    private void suggestFocusTaskForSelectedDay() {
        if (binding == null) {
            return;
        }

        long selectedDayMillis = plannerDate.getTimeInMillis();
        Task best = null;
        for (Task task : cachedTasks) {
            if (task == null || task.isCompleted()) {
                continue;
            }
            if (!isDayWithinTaskWindow(task, selectedDayMillis)) {
                continue;
            }
            if (!"ALL".equalsIgnoreCase(selectedPriorityFilter)
                    && !selectedPriorityFilter.equalsIgnoreCase(task.getPriority())) {
                continue;
            }

            if (best == null) {
                best = task;
                continue;
            }

            int rankBest = priorityRank(effectivePriorityForDay(best, selectedDayMillis));
            int rankCandidate = priorityRank(effectivePriorityForDay(task, selectedDayMillis));
            if (rankCandidate < rankBest) {
                best = task;
                continue;
            }

            if (rankCandidate == rankBest && task.getDueDate() < best.getDueDate()) {
                best = task;
            }
        }

        if (best == null) {
            Toast.makeText(requireContext(), "No active task to suggest for this day", Toast.LENGTH_SHORT).show();
            return;
        }

        Task suggested = best;
        String title = suggested.getTitle() != null ? suggested.getTitle() : "Focus Task";
        String priority = effectivePriorityForDay(suggested, selectedDayMillis);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Suggested focus task")
            .setMessage(title
                + "\nPriority: " + priority
                + "\nDeadline: " + formatShortDateTime(resolveTaskDueMillis(suggested)))
                .setPositiveButton("Start Focus", (dialog, which) -> onTaskStartFocus(suggested))
                .setNeutralButton("Open Task", (dialog, which) -> showCreateTaskDialog(suggested))
                .setNegativeButton("Later", null)
                .show();
    }

    private boolean isDayWithinTaskWindow(Task task, long dayMillis) {
        if (task == null) {
            return false;
        }

        long normalizedDay = normalizeDayMillis(dayMillis);
        long startMillis = resolveTaskStartMillis(task);
        long dueMillis = resolveTaskDueMillis(task);

        startMillis = normalizeDayMillis(startMillis);
        dueMillis = normalizeDayMillis(dueMillis);

        if (startMillis > dueMillis) {
            long temp = startMillis;
            startMillis = dueMillis;
            dueMillis = temp;
        }

        return normalizedDay >= startMillis && normalizedDay <= dueMillis;
    }

    private long resolveTaskStartMillis(Task task) {
        if (task == null) {
            return 0L;
        }
        if (task.getStartDate() > 0L) {
            return task.getStartDate();
        }
        if (task.getCreatedAt() > 0L) {
            return task.getCreatedAt();
        }
        return task.getDueDate();
    }

    private long resolveTaskDueMillis(Task task) {
        if (task == null) {
            return 0L;
        }
        long start = resolveTaskStartMillis(task);
        long due = task.getDueDate() > 0L ? task.getDueDate() : start;
        return Math.max(due, start);
    }

    private String effectivePriorityForDay(Task task, long dayMillis) {
        if (task == null) {
            return "LOW";
        }

        String basePriority = normalizePriority(task.getPriority());
        if (!task.isCompleted() && isSameDay(dayMillis, resolveTaskDueMillis(task))) {
            return "HIGH";
        }
        return basePriority;
    }

    private static String normalizePriority(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) {
            return "HIGH";
        }
        if ("MEDIUM".equalsIgnoreCase(priority)) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private void updateCalendarIndicators() {
        if (binding == null) {
            return;
        }

        Calendar monthStart = (Calendar) plannerDate.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        normalizeToDayStart(monthStart);

        Calendar monthEnd = (Calendar) monthStart.clone();
        monthEnd.add(Calendar.MONTH, 1);
        monthEnd.add(Calendar.MILLISECOND, -1);

        long monthStartMillis = monthStart.getTimeInMillis();
        long monthEndMillis = normalizeDayMillis(monthEnd.getTimeInMillis());
        long selectedDayMillis = normalizeDayMillis(plannerDate.getTimeInMillis());

        Set<Long> coveredDays = new HashSet<>();
        Set<Long> highDays = new HashSet<>();
        Set<Long> mediumDays = new HashSet<>();
        Set<Long> lowDays = new HashSet<>();
        Set<Long> deadlineDays = new HashSet<>();
        Map<Long, Integer> dayTaskCounts = new TreeMap<>();

        int selectedTracked = 0;
        int selectedDeadline = 0;
        int selectedEscalated = 0;

        for (Task task : cachedTasks) {
            if (task == null || task.isCompleted()) {
                continue;
            }

            long taskStart = normalizeDayMillis(resolveTaskStartMillis(task));
            long taskDue = normalizeDayMillis(resolveTaskDueMillis(task));
            if (taskStart > taskDue) {
                long temp = taskStart;
                taskStart = taskDue;
                taskDue = temp;
            }

            if (selectedDayMillis >= taskStart && selectedDayMillis <= taskDue) {
                selectedTracked++;
                if (isSameDay(selectedDayMillis, taskDue)) {
                    selectedDeadline++;
                    selectedEscalated++;
                }
            }

            long clampedStart = Math.max(taskStart, monthStartMillis);
            long clampedEnd = Math.min(taskDue, monthEndMillis);
            if (clampedStart > clampedEnd) {
                continue;
            }

            Calendar dayCursor = Calendar.getInstance();
            dayCursor.setTimeInMillis(clampedStart);
            normalizeToDayStart(dayCursor);
            while (dayCursor.getTimeInMillis() <= clampedEnd) {
                long day = dayCursor.getTimeInMillis();
                coveredDays.add(day);
                dayTaskCounts.put(day, dayTaskCounts.getOrDefault(day, 0) + 1);

                String effective = effectivePriorityForDay(task, day);
                if ("HIGH".equalsIgnoreCase(effective)) {
                    highDays.add(day);
                } else if ("MEDIUM".equalsIgnoreCase(effective)) {
                    mediumDays.add(day);
                } else {
                    lowDays.add(day);
                }
                if (isSameDay(day, taskDue)) {
                    deadlineDays.add(day);
                }
                dayCursor.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        String monthLabel = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthStartMillis);
        int maxConcentration = 0;
        for (Integer count : dayTaskCounts.values()) {
            if (count != null) {
                maxConcentration = Math.max(maxConcentration, count);
            }
        }

        binding.tvCalendarIndicatorSummary.setText(
                monthLabel + " • " + coveredDays.size() + " active planner days"
                        + " • peak " + maxConcentration + " tasks/day"
        );

        if (selectedTracked == 0) {
            binding.tvCalendarIndicatorSelected.setText("Selected day has no active tasks");
            binding.tvCalendarIndicatorSelected.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
            );
        } else {
            binding.tvCalendarIndicatorSelected.setText(
                    "Selected day: " + selectedTracked
                            + " active • " + selectedDeadline
                            + " deadline markers • " + selectedEscalated
                            + " auto-high"
            );
            binding.tvCalendarIndicatorSelected.setTextColor(
                    ContextCompat.getColor(requireContext(), concentrationColorRes(selectedTracked))
            );
        }

        binding.tvCalendarIndicatorHigh.setText("High " + highDays.size());
        binding.tvCalendarIndicatorMedium.setText("Medium " + mediumDays.size());
        binding.tvCalendarIndicatorLow.setText("Low " + lowDays.size());
        binding.tvCalendarIndicatorDeadline.setText("Deadlines " + deadlineDays.size());
        renderConcentrationDayChips(dayTaskCounts, selectedDayMillis);
    }

    private void renderConcentrationDayChips(Map<Long, Integer> dayTaskCounts, long selectedDayMillis) {
        if (binding == null) {
            return;
        }

        binding.chipGroupCalendarConcentration.removeAllViews();
        if (dayTaskCounts == null || dayTaskCounts.isEmpty()) {
            binding.tvCalendarConcentrationEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvCalendarConcentrationEmpty.setVisibility(View.GONE);
        SimpleDateFormat dayFormatter = new SimpleDateFormat("MMM dd", Locale.getDefault());

        for (Map.Entry<Long, Integer> entry : dayTaskCounts.entrySet()) {
            if (entry == null || entry.getValue() == null) {
                continue;
            }

            long dayMillis = entry.getKey();
            int count = entry.getValue();

            Chip chip = new Chip(requireContext());
            chip.setClickable(true);
            chip.setCheckable(false);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setChipBackgroundColorResource(concentrationColorRes(count));
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            String prefix = isSameDay(dayMillis, selectedDayMillis) ? "Selected " : "";
            chip.setText(prefix + dayFormatter.format(dayMillis) + " • " + count);

            chip.setOnClickListener(v -> {
                plannerDate.setTimeInMillis(dayMillis);
                normalizeToDayStart(plannerDate);
                binding.calendarPlanner.setDate(dayMillis, true, true);
                onPlannerDateChanged();
            });
            binding.chipGroupCalendarConcentration.addView(chip);
        }
    }

    private int concentrationColorRes(int taskCount) {
        if (taskCount >= 5) {
            return R.color.concentration_red;
        }
        if (taskCount >= 3) {
            return R.color.concentration_green;
        }
        return R.color.concentration_blue;
    }

    private static long normalizeDayMillis(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        normalizeToDayStart(calendar);
        return calendar.getTimeInMillis();
    }

    private static int priorityRank(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) {
            return 0;
        }
        if ("MEDIUM".equalsIgnoreCase(priority)) {
            return 1;
        }
        return 2;
    }

    private String formatShortDate(long millis) {
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(millis);
    }

    private String formatShortDateTime(long millis) {
        return new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(millis);
    }

    private static boolean isMidnight(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY) == 0
                && calendar.get(Calendar.MINUTE) == 0
                && calendar.get(Calendar.SECOND) == 0;
    }

    private static void applyDefaultDueTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, DEFAULT_DUE_HOUR);
        calendar.set(Calendar.MINUTE, DEFAULT_DUE_MINUTE);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private static boolean isSameDay(long firstMillis, long secondMillis) {
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(firstMillis);
        Calendar second = Calendar.getInstance();
        second.setTimeInMillis(secondMillis);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private static void normalizeToDayStart(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private String getPriority(DialogCreateTaskBinding b) {
        if (b.chipHigh.isChecked()) return "HIGH";
        if (b.chipLow.isChecked()) return "LOW";
        return "MEDIUM";
    }

    private void updateTimelineButtons(DialogCreateTaskBinding b) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        b.btnPickStartDate.setText("Start: " + dateFormatter.format(selectedStartDate.getTime()));
        b.btnPickDeadline.setText("Deadline: " + dateFormatter.format(selectedDueDate.getTime()));
        b.btnPickDeadlineTime.setText("Due Time: " + timeFormatter.format(selectedDueDate.getTime()));
        b.tvDeadlineDetails.setText(
                "Notifications and deadline checks follow "
                        + timeFormatter.format(selectedDueDate.getTime())
        );
    }

    @Override
    public void onTaskClick(Task task) {
        showCreateTaskDialog(task);
    }

    @Override
    public void onTaskComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        viewModel.updateTask(task);
        Toast.makeText(requireContext(),
                task.isCompleted() ? "Task completed!" : "Task reactivated!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskDelete(Task task) {
        if (task == null) {
            return;
        }

        Task deletedSnapshot = cloneTaskForUndo(task);
        String title = task.getTitle() != null ? task.getTitle().trim() : "";
        String message = title.isEmpty()
                ? getString(R.string.task_delete_confirm_message_fallback)
                : getString(R.string.task_delete_confirm_message, title);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.task_delete_confirm_title)
                .setMessage(message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    viewModel.deleteTask(deletedSnapshot.getTaskId());
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(), R.string.task_deleted, Snackbar.LENGTH_LONG)
                                .setAction(R.string.action_undo, v -> restoreDeletedTask(deletedSnapshot))
                                .show();
                    } else {
                        showPlannerMessage(R.string.task_deleted);
                    }
                })
                .show();
    }

    private Task cloneTaskForUndo(Task source) {
        Task clone = new Task();
        clone.setTaskId(source.getTaskId());
        clone.setUserId(source.getUserId());
        clone.setTitle(source.getTitle());
        clone.setDescription(source.getDescription());
        clone.setStartDate(source.getStartDate());
        clone.setDueDate(source.getDueDate());
        clone.setPriority(source.getPriority());
        clone.setCategory(source.getCategory());

        clone.isCompleted = source.isCompleted();
        clone.createdAt = source.getCreatedAt();
        clone.updatedAt = source.getUpdatedAt();
        clone.completedAt = source.getCompletedAt();
        return clone;
    }

    private void restoreDeletedTask(Task deletedSnapshot) {
        if (deletedSnapshot == null || userId == null || userId.trim().isEmpty()) {
            return;
        }

        viewModel.createTask(deletedSnapshot, userId);
        showPlannerMessage(R.string.task_restored);
    }

    @Override
    public void onTaskStartFocus(Task task) {
        if (task == null || task.isCompleted()) {
            Toast.makeText(requireContext(), "Only active tasks can start focus mode", Toast.LENGTH_SHORT).show();
            return;
        }

        String subject = task.getCategory() != null && !task.getCategory().trim().isEmpty()
                ? task.getCategory().trim()
                : "General";

        String taskId = task.getTaskId() != null ? task.getTaskId().trim() : "";
        String title = task.getTitle() != null ? task.getTitle().trim() : "";
        String effectivePriority = effectivePriorityForDay(task, System.currentTimeMillis());
        long dueAt = resolveTaskDueMillis(task);

        uiPrefs().edit()
            .putString(PREF_PENDING_TIMER_SUBJECT, subject)
            .putString(PREF_PENDING_TIMER_TASK_ID, taskId)
            .putString(PREF_PENDING_TIMER_TASK_TITLE, title)
            .putString(PREF_PENDING_TIMER_TASK_PRIORITY, effectivePriority)
            .putLong(PREF_PENDING_TIMER_TASK_DUE, dueAt)
            .putBoolean(PREF_PENDING_TIMER_AUTO_START, true)
            .apply();

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateTo(R.id.timerFragment);
        }

        Toast.makeText(
            requireContext(),
            "Focus timer ready for " + subject + " • due " + formatShortDateTime(dueAt),
            Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
