package com.example.studysync_project.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.studysync_project.databinding.FragmentHomeBinding;
import com.example.studysync_project.ui.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        loadUserData();
        setupClickListeners();
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;
        
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                String name = doc.getString("name");
                String email = doc.getString("email");
                
                // Set welcome message
                binding.tvWelcome.setText("Welcome back, " + (name != null ? name : "Student") + "!");
                
                // Set user email
                if (email != null) {
                    binding.tvUserEmail.setText(email);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading user data", Toast.LENGTH_SHORT).show();
            });
    }

    private void setupClickListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmation());
        
        // Quick action buttons
        binding.btnStartQuiz.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Starting quiz...", Toast.LENGTH_SHORT).show()
        );
        
        binding.btnStudyTimer.setOnClickListener(v -> 
            Toast.makeText(getContext(), "Starting timer...", Toast.LENGTH_SHORT).show()
        );
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> performLogout())
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void performLogout() {
        auth.signOut();
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
