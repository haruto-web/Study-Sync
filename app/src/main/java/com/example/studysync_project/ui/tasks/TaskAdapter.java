package com.example.studysync_project.ui.tasks;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.R;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.databinding.ItemTaskBinding;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying tasks
 */
public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private OnTaskClickListener listener;
    private long referenceDayMillis = System.currentTimeMillis();

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskComplete(Task task);
        void onTaskStartFocus(Task task);
        void onTaskDelete(Task task);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setReferenceDayMillis(long referenceDayMillis) {
        long normalizedCurrent = startOfDay(this.referenceDayMillis);
        long normalizedIncoming = startOfDay(referenceDayMillis);
        this.referenceDayMillis = referenceDayMillis;
        if (normalizedCurrent != normalizedIncoming) {
            notifyDataSetChanged();
        }
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<Task>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTaskId().equals(newItem.getTaskId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.isCompleted() == newItem.isCompleted() &&
                   oldItem.getStartDate() == newItem.getStartDate() &&
                   oldItem.getDueDate() == newItem.getDueDate() &&
                   safe(oldItem.getDescription()).equals(safe(newItem.getDescription())) &&
                   safe(oldItem.getCategory()).equals(safe(newItem.getCategory())) &&
                   safe(oldItem.getPriority()).equals(safe(newItem.getPriority()));
        }

        private String safe(String text) {
            return text == null ? "" : text;
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
            String title = task.getTitle() != null ? task.getTitle().trim() : "";
            if (title.isEmpty()) {
                title = "Untitled task";
            }
            binding.tvTaskTitle.setText(title);
            binding.tvTaskTitle.setContentDescription(title);

            String description = task.getDescription() != null ? task.getDescription().trim() : "";
            binding.tvTaskDescription.setText(description);
            binding.tvTaskDescription.setVisibility(description.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

            String category = task.getCategory() != null ? task.getCategory().trim() : "";
            String categoryLabel = category.isEmpty() ? "General" : category;
            boolean showCategory = !"GENERAL".equalsIgnoreCase(categoryLabel);
            binding.tvCategory.setText(categoryLabel);
            binding.tvCategory.setVisibility(showCategory ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.tvCategory.setContentDescription("Category " + categoryLabel);

            // Format timeline (start -> deadline)
            SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            long startDate = task.getStartDate() > 0L
                    ? task.getStartDate()
                    : (task.getCreatedAt() > 0L ? task.getCreatedAt() : task.getDueDate());
            long deadline = task.getDueDate() > 0L ? task.getDueDate() : startDate;
            if (startDate > deadline) {
                long temp = startDate;
                startDate = deadline;
                deadline = temp;
            }

            boolean deadlineDay = !task.isCompleted() && isSameDay(referenceDayMillis, deadline);
            String effectivePriority = deadlineDay ? "HIGH" : normalizePriority(task.getPriority());

            long contextDayStart = startOfDay(referenceDayMillis);
            boolean overdue = !task.isCompleted() && deadline < contextDayStart;
            boolean withinWindow = contextDayStart >= startOfDay(startDate)
                    && contextDayStart <= startOfDay(deadline);

            if (task.isCompleted()) {
                binding.tvDueDate.setText(
                        "Completed • Deadline was " + dateTimeFormatter.format(new Date(deadline))
                );
            } else if (overdue) {
                binding.tvDueDate.setText(
                        "Overdue • Due " + dateTimeFormatter.format(new Date(deadline))
                );
            } else if (deadlineDay) {
                binding.tvDueDate.setText(
                        "Due today • " + dateTimeFormatter.format(new Date(deadline))
                );
            } else if (withinWindow) {
                binding.tvDueDate.setText(
                        "In progress • Due " + dateTimeFormatter.format(new Date(deadline))
                );
            } else {
                binding.tvDueDate.setText(
                        "Upcoming • " + dateFormatter.format(new Date(startDate))
                                + " to " + dateTimeFormatter.format(new Date(deadline))
                );
            }
            binding.tvDueDate.setContentDescription(binding.tvDueDate.getText());

            applyPriorityStyling(task, effectivePriority);
            binding.cardTask.setAlpha(task.isCompleted() ? 0.88f : 1.0f);

            if (overdue) {
                binding.tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error));
            } else {
                binding.tvDueDate.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }

            // Set completion checkbox
            binding.cbTaskComplete.setOnCheckedChangeListener(null);
            binding.cbTaskComplete.setChecked(task.isCompleted());
            binding.cbTaskComplete.setContentDescription(
                    task.isCompleted()
                            ? "Mark " + title + " as active"
                            : "Mark " + title + " as completed"
            );
            binding.cbTaskComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onTaskComplete(task);
                }
            });

            binding.btnStartFocus.setEnabled(!task.isCompleted());
            binding.btnStartFocus.setAlpha(task.isCompleted() ? 0.55f : 1.0f);
            binding.btnStartFocus.setContentDescription("Start focus for " + title);
            binding.btnStartFocus.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskStartFocus(task);
                }
            });

            // Click listener
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });

            // Delete button
            binding.btnDelete.setContentDescription("Delete task " + title);
            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskDelete(task);
                }
            });
        }

        private void applyPriorityStyling(Task task, String priorityText) {
            String priority = priorityText != null ? priorityText.trim().toUpperCase(Locale.US) : "MEDIUM";

            int strokeColor;
            int chipBgColor;
            int chipTextColor;
            String priorityLabel;

            switch (priority) {
                case "HIGH":
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.error);
                    chipBgColor = ContextCompat.getColor(itemView.getContext(), R.color.error);
                    chipTextColor = ContextCompat.getColor(itemView.getContext(), R.color.on_error);
                    priorityLabel = "P1 High";
                    break;
                case "LOW":
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.success);
                    chipBgColor = ContextCompat.getColor(itemView.getContext(), R.color.success);
                    chipTextColor = ContextCompat.getColor(itemView.getContext(), R.color.on_primary);
                    priorityLabel = "P3 Low";
                    break;
                default:
                    strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.warning);
                    chipBgColor = ContextCompat.getColor(itemView.getContext(), R.color.warning);
                    chipTextColor = ContextCompat.getColor(itemView.getContext(), R.color.on_primary);
                    priorityLabel = "P2 Medium";
                    break;
            }

            MaterialCardView card = binding.cardTask;
            int cardBgColor = ContextCompat.getColor(itemView.getContext(), R.color.surface);
            if (task.isCompleted()) {
                strokeColor = ContextCompat.getColor(itemView.getContext(), R.color.outline);
                cardBgColor = ContextCompat.getColor(itemView.getContext(), R.color.surface_variant);
                chipBgColor = ContextCompat.getColor(itemView.getContext(), R.color.outline);
                chipTextColor = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
                priorityLabel = "Done";
            }

            card.setStrokeColor(strokeColor);
            card.setCardBackgroundColor(cardBgColor);

            binding.tvPriority.setText(priorityLabel);
            binding.tvPriority.setContentDescription("Priority " + priorityLabel);
            binding.tvPriority.setBackgroundTintList(ColorStateList.valueOf(chipBgColor));
            binding.tvPriority.setTextColor(chipTextColor);
            binding.tvCategory.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), R.color.primary_container)
            ));
        }

        private String normalizePriority(String priorityText) {
            if ("HIGH".equalsIgnoreCase(priorityText)) {
                return "HIGH";
            }
            if ("LOW".equalsIgnoreCase(priorityText)) {
                return "LOW";
            }
            return "MEDIUM";
        }

        private boolean isSameDay(long firstMillis, long secondMillis) {
            Calendar first = Calendar.getInstance();
            first.setTime(new Date(firstMillis));
            Calendar second = Calendar.getInstance();
            second.setTime(new Date(secondMillis));
            return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                    && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
        }

        private long startOfDay(long millis) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(millis);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        }
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
