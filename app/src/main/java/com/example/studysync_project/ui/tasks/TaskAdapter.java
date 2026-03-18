package com.example.studysync_project.ui.tasks;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.databinding.ItemTaskBinding;

/**
 * RecyclerView Adapter for displaying tasks
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task);
        void onTaskDelete(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTaskId().equals(newItem.getTaskId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.isCompleted() == newItem.isCompleted();
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTaskBinding binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new TaskViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ItemTaskBinding binding;

        TaskViewHolder(ItemTaskBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Task task) {
            binding.tvTaskTitle.setText(task.getTitle());
            binding.tvTaskDescription.setText(task.getDescription());
            binding.tvCategory.setText(task.getCategory());

            // Format due date
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy");
            binding.tvDueDate.setText("Due: " + sdf.format(new java.util.Date(task.getDueDate())));

            // Set priority color and text
            String priority = task.getPriority();
            binding.tvPriority.setText(priority);
            switch (priority) {
                case "HIGH":
                    binding.tvPriority.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                    break;
                case "MEDIUM":
                    binding.tvPriority.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
                    break;
                case "LOW":
                    binding.tvPriority.setTextColor(itemView.getContext().getColor(android.R.color.holo_green_dark));
                    break;
            }

            // Set completion checkbox
            binding.cbTaskComplete.setChecked(task.isCompleted());
            binding.cbTaskComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskComplete(task);
                }
            });

            // Click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            // Delete button
            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskDelete(task);
                }
            });
        }
    }
}
