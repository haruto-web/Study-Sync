package com.example.studysync_project.ui.quiz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.databinding.ItemQuizBinding;

/**
 * RecyclerView Adapter for displaying quizzes
 */
public class QuizAdapter extends ListAdapter<Quiz, QuizAdapter.QuizViewHolder> {

    private OnQuizClickListener listener;

    public interface OnQuizClickListener {
        void onQuizClick(Quiz quiz);
        void onQuizDelete(Quiz quiz);
    }

    public QuizAdapter(OnQuizClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Quiz> DIFF_CALLBACK = new DiffUtil.ItemCallback<Quiz>() {
        @Override
        public boolean areItemsTheSame(@NonNull Quiz oldItem, @NonNull Quiz newItem) {
            return oldItem.getQuizId().equals(newItem.getQuizId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Quiz oldItem, @NonNull Quiz newItem) {
            return oldItem.getTitle().equals(newItem.getTitle()) &&
                   oldItem.getTotalQuestions() == newItem.getTotalQuestions() &&
                   oldItem.isUnlocked() == newItem.isUnlocked() &&
                   oldItem.getAttemptCount() == newItem.getAttemptCount() &&
                   oldItem.getMasteredAt() == newItem.getMasteredAt() &&
                   oldItem.getBestScore() == newItem.getBestScore();
        }
    };

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemQuizBinding binding = ItemQuizBinding.inflate(
            LayoutInflater.from(parent.getContext()),
            parent,
            false
        );
        return new QuizViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull QuizViewHolder holder, int position) {
        Quiz quiz = getItem(position);
        holder.bind(quiz);
    }

    class QuizViewHolder extends RecyclerView.ViewHolder {
        private final ItemQuizBinding binding;

        QuizViewHolder(ItemQuizBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Quiz quiz) {
            binding.tvQuizTitle.setText(quiz.getTitle());
            binding.tvQuizSubject.setText(quiz.getSubject());
            binding.tvQuestionCount.setText(quiz.getTotalQuestions() + " questions");

            boolean linkedToModule = quiz.getModuleId() != null && !quiz.getModuleId().trim().isEmpty();
            binding.tvQuizModule.setVisibility(linkedToModule ? View.VISIBLE : View.GONE);
            binding.tvQuizProgression.setText(formatProgressionLabel(quiz));
            binding.getRoot().setAlpha(quiz.isUnlocked() ? 1.0f : 0.65f);
            
            // Set difficulty stars (1-5)
            StringBuilder difficulty = new StringBuilder();
            for (int i = 0; i < quiz.getDifficulty(); i++) {
                difficulty.append("★");
            }
            for (int i = quiz.getDifficulty(); i < 5; i++) {
                difficulty.append("☆");
            }
            binding.tvDifficulty.setText(difficulty.toString());

            binding.tvPassingScore.setText("Pass: " + (int) quiz.getPassingScore() + "%");

            // Click listeners
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuizClick(quiz);
                }
            });

            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuizDelete(quiz);
                }
            });
        }

        private String formatProgressionLabel(Quiz quiz) {
            if (quiz == null || !quiz.isUnlocked()) {
                return "Locked";
            }
            if (quiz.getMasteredAt() > 0L) {
                return "Mastered";
            }
            if (quiz.getAttemptCount() > 0) {
                return "In Progress";
            }
            return "New";
        }
    }
}
