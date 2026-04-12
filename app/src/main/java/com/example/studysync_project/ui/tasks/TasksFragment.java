package com.example.studysync_project.ui.tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.databinding.DialogCreateTaskBinding;
import com.example.studysync_project.databinding.FragmentTasksBinding;
import com.example.studysync_project.utils.IdUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private FragmentTasksBinding binding;
    private TasksViewModel viewModel;
    private TaskAdapter adapter;
    private String userId;
    private final Calendar selectedDueDate = Calendar.getInstance();
    private final List<Task> cachedTasks = new ArrayList<>();
    private int selectedFilterTab = 0;

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

    private void showCreateTaskDialog(@Nullable Task existingTask) {
        DialogCreateTaskBinding dialogBinding = DialogCreateTaskBinding.inflate(LayoutInflater.from(requireContext()));
        selectedDueDate.setTimeInMillis(System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L);
        updateDateButton(dialogBinding);

        if (existingTask != null) {
            dialogBinding.etTitle.setText(existingTask.getTitle());
            dialogBinding.etDescription.setText(existingTask.getDescription());
            dialogBinding.etCategory.setText(existingTask.getCategory());
            selectedDueDate.setTimeInMillis(existingTask.getDueDate());
            updateDateButton(dialogBinding);
            switch (existingTask.getPriority()) {
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

        dialogBinding.btnPickDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(),
                    (dp, y, m, d) -> {
                        selectedDueDate.set(y, m, d);
                        updateDateButton(dialogBinding);
                    },
                    selectedDueDate.get(Calendar.YEAR),
                    selectedDueDate.get(Calendar.MONTH),
                    selectedDueDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(existingTask == null ? "New Task" : "Edit Task")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = dialogBinding.etTitle.getText() != null
                            ? dialogBinding.etTitle.getText().toString().trim() : "";
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (title.length() > 80) {
                        Toast.makeText(requireContext(), "Keep the title under 80 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Calendar dueDay = (Calendar) selectedDueDate.clone();
                    dueDay.set(Calendar.HOUR_OF_DAY, 0);
                    dueDay.set(Calendar.MINUTE, 0);
                    dueDay.set(Calendar.SECOND, 0);
                    dueDay.set(Calendar.MILLISECOND, 0);

                    Calendar today = Calendar.getInstance();
                    today.set(Calendar.HOUR_OF_DAY, 0);
                    today.set(Calendar.MINUTE, 0);
                    today.set(Calendar.SECOND, 0);
                    today.set(Calendar.MILLISECOND, 0);

                    if (dueDay.before(today)) {
                        Toast.makeText(requireContext(), "Due date cannot be in the past", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String description = dialogBinding.etDescription.getText() != null
                            ? dialogBinding.etDescription.getText().toString().trim() : "";
                    if (description.length() > 280) {
                        Toast.makeText(requireContext(), "Keep the description under 280 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String categoryInput = dialogBinding.etCategory.getText() != null
                            ? dialogBinding.etCategory.getText().toString().trim() : "";
                    String category = categoryInput.isEmpty() ? "General" : categoryInput;
                    String priority = getPriority(dialogBinding);

                    if (existingTask == null) {
                        Task task = new Task(userId, title, description,
                                selectedDueDate.getTimeInMillis(), priority, category);
                        task.setTaskId(IdUtil.generateId("task"));
                        viewModel.createTask(task, userId);
                        Toast.makeText(requireContext(), "Task created!", Toast.LENGTH_SHORT).show();
                    } else {
                        existingTask.setTitle(title);
                        existingTask.setDescription(description);
                        existingTask.setCategory(category);
                        existingTask.setPriority(priority);
                        existingTask.setDueDate(selectedDueDate.getTimeInMillis());
                        viewModel.updateTask(existingTask);
                        Toast.makeText(requireContext(), "Task updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void renderTasksForActiveFilter() {
        if (binding == null) {
            return;
        }

        List<Task> filtered = new ArrayList<>();
        for (Task task : cachedTasks) {
            if (task == null) {
                continue;
            }
            if (selectedFilterTab == 1 && task.isCompleted()) {
                continue;
            }
            if (selectedFilterTab == 2 && !task.isCompleted()) {
                continue;
            }
            filtered.add(task);
        }

        adapter.submitList(filtered);
        boolean isEmpty = filtered.isEmpty();
        binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private String getPriority(DialogCreateTaskBinding b) {
        if (b.chipHigh.isChecked()) return "HIGH";
        if (b.chipLow.isChecked()) return "LOW";
        return "MEDIUM";
    }

    private void updateDateButton(DialogCreateTaskBinding b) {
        b.btnPickDate.setText("Due: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(selectedDueDate.getTime()));
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
        viewModel.deleteTask(task.getTaskId());
        Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
