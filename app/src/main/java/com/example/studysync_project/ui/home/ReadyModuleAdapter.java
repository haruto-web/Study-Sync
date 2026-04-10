package com.example.studysync_project.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studysync_project.databinding.ItemReadyModuleBinding;

import java.util.ArrayList;
import java.util.List;

public class ReadyModuleAdapter extends RecyclerView.Adapter<ReadyModuleAdapter.VH> {

    public interface Listener {
        void onModuleClick(@NonNull ReadyModule module);
    }

    private final Listener listener;
    private final List<ReadyModule> items = new ArrayList<>();

    public ReadyModuleAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<ReadyModule> modules) {
        items.clear();
        if (modules != null) items.addAll(modules);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReadyModuleBinding binding = ItemReadyModuleBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VH(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ReadyModule module = items.get(position);
        holder.binding.tvTitle.setText(module.title);
        holder.binding.tvMeta.setText(module.gradeLevel + " \u2022 " + module.subject + " \u2022 " + module.topic);
        holder.binding.tvDescription.setText(module.description);

        if (module.difficulty != null && !module.difficulty.isEmpty()) {
            holder.binding.tvDifficulty.setText(module.difficulty);
            holder.binding.tvDifficulty.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.binding.tvDifficulty.setVisibility(android.view.View.GONE);
        }

        if (module.lessons != null && !module.lessons.isEmpty()) {
            holder.binding.tvLessonsLabel.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvLessons.setVisibility(android.view.View.VISIBLE);
            holder.binding.tvLessons.setText(module.lessons);
        } else {
            holder.binding.tvLessonsLabel.setVisibility(android.view.View.GONE);
            holder.binding.tvLessons.setVisibility(android.view.View.GONE);
        }

        holder.binding.getRoot().setOnClickListener(v -> listener.onModuleClick(module));
        holder.binding.btnGenerate.setOnClickListener(v -> listener.onModuleClick(module));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemReadyModuleBinding binding;

        VH(ItemReadyModuleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
