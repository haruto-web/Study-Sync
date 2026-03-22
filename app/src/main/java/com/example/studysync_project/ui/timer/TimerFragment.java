package com.example.studysync_project.ui.timer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.studysync_project.databinding.FragmentTimerBinding;
import com.google.firebase.auth.FirebaseAuth;

public class TimerFragment extends Fragment {

    private FragmentTimerBinding binding;
    private TimerViewModel viewModel;
    private final BroadcastReceiver timerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TimerService.ACTION_TICK.equals(intent.getAction())) {
                long millisLeft = intent.getLongExtra(TimerService.EXTRA_MILLIS_LEFT, 0);
                int prog = intent.getIntExtra(TimerService.EXTRA_PROGRESS, 100);
                viewModel.onTick(millisLeft, prog);
            } else if (TimerService.ACTION_FINISH.equals(intent.getAction())) {
                viewModel.onSessionFinished();
                binding.btnStartPause.setText("Start");
                updateSessionLabel();
            }
        }
    };
    private TimerService timerService;
    private boolean serviceBound = false;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            timerService = ((TimerService.LocalBinder) binder).getService();
            serviceBound = true;
            // Restore UI if timer was already running
            if (timerService.isRunning()) {
                binding.btnStartPause.setText("Pause");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        viewModel = new TimerViewModel(requireContext(), userId);

        // Start & bind service
        Intent serviceIntent = new Intent(requireContext(), TimerService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        requireContext().bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        observeViewModel();
        setupControls();
        setupTipCard();
    }

    private void observeViewModel() {
        viewModel.getTimerText().observe(getViewLifecycleOwner(), t -> binding.tvTimer.setText(t));
        viewModel.getProgress().observe(getViewLifecycleOwner(), p -> binding.progressTimer.setProgress(p));
        viewModel.getIsRunning().observe(getViewLifecycleOwner(), running ->
                binding.btnStartPause.setText(running ? "Pause" : "Start"));
        viewModel.getSessionCount().observe(getViewLifecycleOwner(), count ->
                binding.tvSessionCount.setText("Session " + count + " of 4"));
        viewModel.getTotalMinutesToday().observe(getViewLifecycleOwner(), mins ->
                binding.tvMinutesToday.setText(String.valueOf(mins != null ? mins : 0)));
        viewModel.getSessionsCompletedToday().observe(getViewLifecycleOwner(), count ->
                binding.tvSessionsCompleted.setText(String.valueOf(count != null ? count : 0)));
    }

    private void setupTipCard() {
        // Dismiss tip card using SharedPreferences so it stays hidden after first dismissal
        android.content.SharedPreferences prefs = requireContext()
                .getSharedPreferences("studysync_prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("timer_tip_dismissed", false)) {
            binding.cardTip.setVisibility(android.view.View.GONE);
        }
        binding.tvDismissTip.setOnClickListener(v -> {
            binding.cardTip.setVisibility(android.view.View.GONE);
            prefs.edit().putBoolean("timer_tip_dismissed", true).apply();
        });
    }

    private void setupControls() {
        binding.btnStartPause.setOnClickListener(v -> {
            if (!serviceBound) return;
            Boolean running = viewModel.getIsRunning().getValue();
            if (running == null || !running) {
                // First start
                viewModel.setSubject(binding.etSubject.getText() != null
                        ? binding.etSubject.getText().toString().trim() : "");
                viewModel.onStart(timerService);
            } else {
                viewModel.onStartPause(timerService);
            }
        });

        binding.btnReset.setOnClickListener(v -> {
            if (serviceBound) viewModel.onReset(timerService);
        });

        binding.btnSkip.setOnClickListener(v -> {
            if (serviceBound) {
                viewModel.onSkip(timerService);
                updateSessionLabel();
            }
        });

        binding.chipGroupSession.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            int type;
            if (id == binding.chipFocus.getId()) type = TimerViewModel.TYPE_FOCUS;
            else if (id == binding.chipShortBreak.getId()) type = TimerViewModel.TYPE_SHORT_BREAK;
            else type = TimerViewModel.TYPE_LONG_BREAK;
            viewModel.setSessionType(type, serviceBound ? timerService : null);
            updateSessionLabel();
        });
    }

    private void updateSessionLabel() {
        Integer type = viewModel.getSessionType().getValue();
        if (type == null) return;
        switch (type) {
            case TimerViewModel.TYPE_SHORT_BREAK:
                binding.tvSessionLabel.setText("Short Break");
                break;
            case TimerViewModel.TYPE_LONG_BREAK:
                binding.tvSessionLabel.setText("Long Break");
                break;
            default:
                binding.tvSessionLabel.setText("Focus Session");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(TimerService.ACTION_TICK);
        filter.addAction(TimerService.ACTION_FINISH);
        requireContext().registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(timerReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            requireContext().unbindService(connection);
            serviceBound = false;
        }
        binding = null;
    }
}
