package com.example.studysync_project;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.studysync_project.databinding.ActivityMainBinding;
import com.example.studysync_project.ui.auth.LoginActivity;
import com.example.studysync_project.utils.FirestoreSyncUtil;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_DEFERRED_SETUP_MESSAGE = "extra_deferred_setup_message";
    public static final String EXTRA_OPEN_TAB_ID = "extra_open_tab_id";

    private ActivityMainBinding binding;
    private FirestoreSyncUtil syncUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // Initialize sync utility and trigger full data sync
        syncUtil = new FirestoreSyncUtil(this);
        syncUtil.syncAllData(currentUser.getUid());

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        handleRequestedNavigation(getIntent());

        String deferredSetupMessage = getIntent().getStringExtra(EXTRA_DEFERRED_SETUP_MESSAGE);
        if (deferredSetupMessage != null && !deferredSetupMessage.trim().isEmpty()) {
            showDeferredSetupBanner(deferredSetupMessage);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRequestedNavigation(intent);
    }

    public void navigateTo(int navItemId) {
        if (binding != null) binding.bottomNavigation.setSelectedItemId(navItemId);
    }

    private void showDeferredSetupBanner(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.bottomNavigation)
                .show();
    }

    private void handleRequestedNavigation(Intent intent) {
        if (intent == null || binding == null) {
            return;
        }

        int tabId = intent.getIntExtra(EXTRA_OPEN_TAB_ID, 0);
        if (tabId != 0) {
            binding.bottomNavigation.setSelectedItemId(tabId);
        }
    }
}
