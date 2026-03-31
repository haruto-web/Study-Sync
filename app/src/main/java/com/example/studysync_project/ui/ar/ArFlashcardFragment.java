package com.example.studysync_project.ui.ar;

import android.Manifest;
import android.graphics.Color;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.databinding.FragmentArBinding;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ArFlashcardFragment extends Fragment {

    private final List<AnchorNode> placedNodes = new ArrayList<>();
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startArIfPossible();
                } else {
                    showArUnavailable(getString(R.string.ar_camera_permission_required));
                }
            });

    private FragmentArBinding binding;
    private ArSceneView arSceneView;
    private boolean arAvailable = false;
    private boolean installRequested;
    private boolean cameraPermissionRequested;
    private boolean sceneTouchListenerAttached;
    private boolean controlsExpanded = true;

    private ArFlashcardViewModel viewModel;
    private List<Quiz> availableQuizzes = new ArrayList<>();
    private List<Question> selectedQuestions = new ArrayList<>();
    private int selectedQuestionIndex = 0;
    private LiveData<List<Question>> questionsLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentArBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new androidx.lifecycle.ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ArFlashcardViewModel(requireContext());
            }
        }).get(ArFlashcardViewModel.class);

        setupQuizSelector();
        binding.btnRetryAr.setOnClickListener(v -> retryArStartup());
        binding.btnArFallback.setOnClickListener(v -> navigateToStandardFlashcards());
        binding.btnToggleArControls.setOnClickListener(v -> {
            controlsExpanded = !controlsExpanded;
            applyArControlPanelState();
        });
        applyArControlPanelState();

        binding.btnClearAr.setOnClickListener(v -> {
            if (arSceneView == null) {
                Toast.makeText(requireContext(), "AR scene is not ready.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (AnchorNode node : placedNodes) {
                arSceneView.getScene().removeChild(node);
                if (node.getAnchor() != null) node.getAnchor().detach();
            }
            placedNodes.clear();
            Toast.makeText(requireContext(), "Cleared", Toast.LENGTH_SHORT).show();
        });

        startArIfPossible();
    }

    private void startArIfPossible() {
        if (binding == null || !isAdded()) {
            return;
        }

        if (!hasCameraPermission()) {
            if (!cameraPermissionRequested) {
                cameraPermissionRequested = true;
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                showArUnavailable(getString(R.string.ar_camera_permission_required));
            }
            return;
        }

        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(requireContext());
        if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            showArUnavailable("This device does not support ARCore.");
            return;
        }

        if (availability.isTransient()) {
            binding.getRoot().postDelayed(this::startArIfPossible, 300L);
            return;
        }

        try {
            ArCoreApk.InstallStatus installStatus =
                    ArCoreApk.getInstance().requestInstall(requireActivity(), !installRequested);
            if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                installRequested = true;
                showArUnavailable(getString(R.string.ar_install_required));
                return;
            }
        } catch (UnavailableUserDeclinedInstallationException e) {
            showArUnavailable(getString(R.string.ar_install_declined));
            return;
        } catch (Exception e) {
            showArUnavailable(getString(R.string.ar_install_check_failed, safeMessage(e)));
            return;
        }

        arSceneView = binding.arSceneView;
        try {
            if (arSceneView.getSession() == null) {
                Session session = new Session(requireContext());
                arSceneView.setupSession(session);
            }
            attachSceneTouchListenerIfNeeded();
            arAvailable = true;
            binding.layoutArUnavailable.setVisibility(View.GONE);
            binding.cardArInstruction.setVisibility(View.VISIBLE);
            binding.cardArControls.setVisibility(View.VISIBLE);
            binding.arSceneView.setVisibility(View.VISIBLE);
            applyArControlPanelState();
        } catch (UnavailableArcoreNotInstalledException e) {
            showArUnavailable(getString(R.string.ar_install_required));
        } catch (UnavailableApkTooOldException | UnavailableSdkTooOldException e) {
            showArUnavailable(getString(R.string.ar_install_required));
        } catch (UnavailableDeviceNotCompatibleException e) {
            showArUnavailable("This device does not support ARCore.");
        } catch (Exception e) {
            showArUnavailable(getString(R.string.ar_session_not_ready, safeMessage(e)));
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void attachSceneTouchListenerIfNeeded() {
        if (sceneTouchListenerAttached || arSceneView == null) {
            return;
        }

        arSceneView.getScene().addOnPeekTouchListener((hitTestResult, motionEvent) -> {
            if (motionEvent.getAction() != MotionEvent.ACTION_UP) return;
            String text = binding.etFlashcardText.getText() != null
                    ? binding.etFlashcardText.getText().toString().trim() : "";
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Enter flashcard text first", Toast.LENGTH_SHORT).show();
                return;
            }
            Frame frame = arSceneView.getArFrame();
            if (frame == null) return;
            List<HitResult> hits = frame.hitTest(motionEvent);
            for (HitResult hit : hits) {
                if (hit.getTrackable() instanceof Plane &&
                        ((Plane) hit.getTrackable()).getType() == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                        hit.getTrackable().getTrackingState() == TrackingState.TRACKING) {
                    placeFlashcard(hit, text);
                    break;
                }
            }
        });
        sceneTouchListenerAttached = true;
    }

    private void navigateToStandardFlashcards() {
        if (!isAdded()) {
            return;
        }
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).navigateTo(R.id.quizFragment);
            return;
        }
        try {
            NavHostFragment.findNavController(this).navigate(R.id.quizFragment);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.ar_open_standard_flashcards_hint), Toast.LENGTH_SHORT).show();
        }
    }

    private void retryArStartup() {
        cameraPermissionRequested = false;
        startArIfPossible();
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    private void applyArControlPanelState() {
        if (binding == null) {
            return;
        }
        binding.layoutArControlsBody.setVisibility(controlsExpanded ? View.VISIBLE : View.GONE);
        binding.btnToggleArControls.setText(
                controlsExpanded ? R.string.ar_hide_controls : R.string.ar_show_controls
        );
    }

    private void placeFlashcard(HitResult hitResult, String text) {
        ViewRenderable.builder()
                .setView(requireContext(), buildCardView(text))
                .build()
                .thenAccept(renderable -> {
                    AnchorNode anchorNode = new AnchorNode(hitResult.createAnchor());
                    anchorNode.setParent(arSceneView.getScene());

                    TransformationSystem ts = new TransformationSystem(
                            requireActivity().getResources().getDisplayMetrics(),
                            new com.google.ar.sceneform.ux.FootprintSelectionVisualizer());
                    TransformableNode node = new TransformableNode(ts);
                    node.setParent(anchorNode);
                    node.setRenderable(renderable);
                    placedNodes.add(anchorNode);

                    advanceSelectedQuestionIfNeeded();
                })
                .exceptionally(t -> {
                    Toast.makeText(requireContext(), "Failed to place card", Toast.LENGTH_SHORT).show();
                    return null;
                });
    }

    private void setupQuizSelector() {
        if (binding == null) return;

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            binding.tilQuizSelector.setVisibility(View.GONE);
            return;
        }

        viewModel.syncQuizzes(userId);
        viewModel.getAllQuizzesForUser(userId).observe(getViewLifecycleOwner(), quizzes -> {
            availableQuizzes = quizzes != null ? quizzes : new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (Quiz q : availableQuizzes) {
                String title = q.getTitle() != null && !q.getTitle().trim().isEmpty()
                        ? q.getTitle().trim() : (q.getSubject() != null ? q.getSubject() : "Quiz");
                String subject = q.getSubject() != null && !q.getSubject().trim().isEmpty()
                        ? q.getSubject().trim() : null;
                labels.add(subject != null && !title.contains(subject) ? (title + " (" + subject + ")") : title);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    labels
            );
            binding.actvQuizSelector.setAdapter(adapter);
        });

        binding.actvQuizSelector.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= availableQuizzes.size()) return;
            Quiz selected = availableQuizzes.get(position);
            String quizId = selected.getQuizId();
            if (quizId == null || quizId.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Selected quiz is missing an ID", Toast.LENGTH_SHORT).show();
                return;
            }

            selectedQuestionIndex = 0;
            selectedQuestions = new ArrayList<>();

            viewModel.syncQuestions(quizId);
            if (questionsLiveData != null) {
                questionsLiveData.removeObservers(getViewLifecycleOwner());
            }
            questionsLiveData = viewModel.getQuestionsForQuiz(quizId);
            questionsLiveData.observe(getViewLifecycleOwner(), questions -> {
                selectedQuestions = questions != null ? questions : new ArrayList<>();
                selectedQuestionIndex = 0;
                updateFlashcardInputFromSelectedQuestion();
                if (selectedQuestions.isEmpty()) {
                    Toast.makeText(requireContext(), "No questions found for this quiz", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void updateFlashcardInputFromSelectedQuestion() {
        if (binding == null) return;
        if (selectedQuestions == null || selectedQuestions.isEmpty()) return;
        if (selectedQuestionIndex < 0 || selectedQuestionIndex >= selectedQuestions.size()) {
            selectedQuestionIndex = 0;
        }
        String text = selectedQuestions.get(selectedQuestionIndex).getQuestionText();
        if (text != null) binding.etFlashcardText.setText(text);
    }

    private void advanceSelectedQuestionIfNeeded() {
        if (selectedQuestions == null || selectedQuestions.isEmpty()) return;
        selectedQuestionIndex++;
        if (selectedQuestionIndex >= selectedQuestions.size()) selectedQuestionIndex = 0;
        updateFlashcardInputFromSelectedQuestion();
    }

    private TextView buildCardView(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setTextColor(Color.parseColor("#1C1B1F"));
        tv.setBackgroundColor(Color.WHITE);
        tv.setPadding(32, 20, 32, 20);
        return tv;
    }

    private void showArUnavailable(String reason) {
        if (binding == null) return;
        arAvailable = false;
        binding.layoutArUnavailable.setVisibility(View.VISIBLE);
        binding.tvArUnavailableReason.setText(reason);
        binding.cardArInstruction.setVisibility(View.GONE);
        binding.cardArControls.setVisibility(View.GONE);
        binding.arSceneView.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!arAvailable) {
            startArIfPossible();
        }
        if (arAvailable && arSceneView != null) {
            try {
                arSceneView.resume();
            } catch (Exception e) {
                showArUnavailable(e.getMessage());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (arAvailable && arSceneView != null) arSceneView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (arAvailable && arSceneView != null) arSceneView.destroy();
        arAvailable = false;
        sceneTouchListenerAttached = false;
        arSceneView = null;
        binding = null;
    }
}
