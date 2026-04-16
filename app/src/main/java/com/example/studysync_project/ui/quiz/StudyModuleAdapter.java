package com.example.studysync_project.ui.quiz;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.data.model.StudyModule;
import com.example.studysync_project.databinding.ItemStudyModuleBinding;

import java.util.Locale;

/**
 * RecyclerView adapter for reusable study modules.
 */
public class StudyModuleAdapter extends ListAdapter<StudyModule, StudyModuleAdapter.StudyModuleViewHolder> {

    public interface OnStudyModuleClickListener {
        void onStudyModuleClick(StudyModule module);
        void onGenerateQuizFromModule(StudyModule module);
    }

    private final OnStudyModuleClickListener listener;

    public StudyModuleAdapter(OnStudyModuleClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<StudyModule> DIFF_CALLBACK = new DiffUtil.ItemCallback<StudyModule>() {
        @Override
        public boolean areItemsTheSame(@NonNull StudyModule oldItem, @NonNull StudyModule newItem) {
            return oldItem.getModuleId().equals(newItem.getModuleId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull StudyModule oldItem, @NonNull StudyModule newItem) {
            return TextUtils.equals(oldItem.getTitle(), newItem.getTitle())
                    && TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                    && TextUtils.equals(oldItem.getSubject(), newItem.getSubject())
                    && TextUtils.equals(oldItem.getTopic(), newItem.getTopic())
                    && TextUtils.equals(oldItem.getContentText(), newItem.getContentText())
                    && TextUtils.equals(oldItem.getSourceType(), newItem.getSourceType())
                    && TextUtils.equals(oldItem.getProgressionState(), newItem.getProgressionState())
                    && oldItem.isUnlocked() == newItem.isUnlocked()
                    && oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    };

    @NonNull
    @Override
    public StudyModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudyModuleBinding binding = ItemStudyModuleBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new StudyModuleViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudyModuleViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class StudyModuleViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudyModuleBinding binding;

        StudyModuleViewHolder(ItemStudyModuleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(StudyModule module) {
            binding.tvModuleTitle.setText(module.getTitle() != null ? module.getTitle() : "Untitled Module");

            String subject = module.getSubject() != null && !module.getSubject().trim().isEmpty()
                    ? module.getSubject().trim() : "General";
            String topic = module.getTopic() != null && !module.getTopic().trim().isEmpty()
                    ? module.getTopic().trim() : "Mixed Topics";
            binding.tvModuleMeta.setText(subject + " • " + topic);

            String description = module.getDescription();
            if (description == null || description.trim().isEmpty()) {
                description = buildFallbackSummary(module.getContentText());
            }
            binding.tvModuleDescription.setText(description);

            String sourceType = module.getSourceType() != null ? module.getSourceType() : "MODULE";
            binding.tvModuleSource.setText(formatSourceLabel(sourceType));
            binding.tvModuleProgression.setText(formatProgressionLabel(module));

            boolean unlocked = module.isUnlocked();
            boolean isNewState = StudyModule.PROGRESSION_NEW.equalsIgnoreCase(module.getProgressionState());

            binding.getRoot().setAlpha(unlocked ? 1.0f : 0.65f);
            binding.btnReviewModule.setEnabled(unlocked);

            if (!unlocked) {
                binding.btnGenerateQuizFromModule.setEnabled(false);
                binding.btnGenerateQuizFromModule.setText("Locked");
            } else if (isNewState) {
                binding.btnGenerateQuizFromModule.setEnabled(false);
                binding.btnGenerateQuizFromModule.setText("Read First");
            } else {
                binding.btnGenerateQuizFromModule.setEnabled(true);
                binding.btnGenerateQuizFromModule.setText("Generate Quiz");
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onStudyModuleClick(module);
            });

            binding.btnReviewModule.setOnClickListener(v -> {
                if (listener != null) listener.onStudyModuleClick(module);
            });

            binding.btnGenerateQuizFromModule.setOnClickListener(v -> {
                if (listener != null) listener.onGenerateQuizFromModule(module);
            });
        }

        private String buildFallbackSummary(String contentText) {
            if (contentText == null || contentText.trim().isEmpty()) {
                return "No module preview available.";
            }
            String compact = contentText.trim().replaceAll("\\s+", " ");
            if (compact.length() > 120) {
                return compact.substring(0, 120) + "...";
            }
            return compact;
        }

        private String formatSourceLabel(String sourceType) {
            String normalized = sourceType.trim().toUpperCase();
            switch (normalized) {
                case "READY_MADE":
                    return "Ready-made";
                case "AI_PERSONALIZED":
                    return "AI personalized";
                case "AI_TOPIC_REQUEST":
                    return "AI topic";
                case "UPLOADED_FILE":
                    return "Uploaded";
                default:
                    return "Saved";
            }
        }

        private String formatProgressionLabel(StudyModule module) {
            if (module == null || !module.isUnlocked()) {
                return "Locked";
            }

            String state = module.getProgressionState() != null
                    ? module.getProgressionState().trim().toUpperCase(Locale.US)
                    : StudyModule.PROGRESSION_NEW;

            switch (state) {
                case StudyModule.PROGRESSION_MASTERED:
                    return "Mastered";
                case StudyModule.PROGRESSION_IN_PROGRESS:
                    return "In Progress";
                default:
                    return "New";
            }
        }
    }
}
