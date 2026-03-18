package com.example.studysync_project.ui.tasks;

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
import com.example.studysync_project.databinding.FragmentTasksBinding;
import com.example.studysync_project.utils.IdUtil;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private FragmentTasksBinding binding;
    private TasksViewModel viewModel;
    private TaskAdapter adapter;
    private FirebaseAuth auth;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this, new androidx.lifecycle.ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new TasksViewModel(requireContext());
            }
        }).get(TasksViewModel.class);

        // Setup RecyclerView
        adapter = new TaskAdapter(this);
        binding.rvTasks.setAdapter(adapter);

        // Observe tasks
        viewModel.getAllTasksForUser(userId).observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                adapter.submitList(tasks);
                binding.emptyState.setVisibility(View.GONE);
                binding.rvTasks.setVisibility(View.VISIBLE);
            } else {
                binding.rvTasks.setVisibility(View.GONE);
                binding.emptyState.setVisibility(View.VISIBLE);
            }
        });

        // Tab selection listener
        binding.tabsFilter.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0: // All
                        viewModel.getAllTasksForUser(userId).observe(getViewLifecycleOwner(), tasks -> adapter.submitList(tasks));
                        break;
                    case 1: // Active
                        viewModel.getAllTasksForUser(userId).observe(getViewLifecycleOwner(), tasks -> {
                            if (tasks != null) {
                                adapter.submitList(tasks.stream().filter(t -> !t.isCompleted()).toList());
                            }
                        });
                        break;
                    case 2: // Completed
                        viewModel.getCompletedTasks(userId).observe(getViewLifecycleOwner(), tasks -> adapter.submitList(tasks));
                        break;
                }
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        // FAB click listener
        binding.fabAddTask.setOnClickListener(v -> createSampleTask());
    }

    /**
     * Create a sample task for demo purposes
     * In production, this would open a dialog for creating tasks
     */
    private void createSampleTask() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 3);

        Task task = new Task(
            userId,
            "Study Chapter 5",
            "Review Chapter 5 of Mathematics textbook",
            calendar.getTimeInMillis(),
            "HIGH",
            "Mathematics"
        );
        task.setTaskId(IdUtil.generateId("task"));

        viewModel.createTask(task, userId);
        Toast.makeText(requireContext(), "Task created!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTaskClick(Task task) {
        Toast.makeText(requireContext(), "Edit: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: Open edit task dialog
    }

    @Override
    public void onTaskComplete(Task task) {
        task.setCompleted(!task.isCompleted());
        viewModel.updateTask(task);
        Toast.makeText(requireContext(), 
            task.isCompleted() ? "Task completed!" : "Task reactivated!", 
            Toast.LENGTH_SHORT).show();
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
