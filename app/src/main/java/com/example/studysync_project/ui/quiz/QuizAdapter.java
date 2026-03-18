package com.example.studysync_project.ui.quiz;

import android.view.LayoutInflater;
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
                   oldItem.getTotalQuestions() == newItem.getTotalQuestions();
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
            
            // Set difficulty stars (1-5)
            String difficulty = "";
            for (int i = 0; i < quiz.getDifficulty(); i++) {
                difficulty += "★";
            }
            for (int i = quiz.getDifficulty(); i < 5; i++) {
                difficulty += "☆";
            }
            binding.tvDifficulty.setText(difficulty);

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
    }
}
