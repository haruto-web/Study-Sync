package com.example.studysync_project.ui.ar;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.example.studysync_project.BuildConfig;
import com.example.studysync_project.MainActivity;
import com.example.studysync_project.R;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.databinding.FragmentArBinding;
import com.example.studysync_project.utils.GeminiApiClient;
import com.example.studysync_project.utils.IdUtil;
import com.example.studysync_project.utils.NetworkUtil;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArFlashcardFragment extends Fragment {

    private enum ArExperienceMode {
        SURFACE,
        FACE
    }

    private static final String TAG = "ArFlashcardFragment";
    private static final int MAX_AR_AVAILABILITY_RETRIES = 12;
    private static final float CARD_PLACEMENT_DISTANCE_METERS = 1.0f;
    private static final float CARD_VERTICAL_OFFSET_METERS = 0.06f;
    private static final float FACE_CARD_VERTICAL_OFFSET_METERS = 0.12f;
    private static final float FACE_CARD_SCREEN_MARGIN_DP = 12f;

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
    private int arAvailabilityRetryCount;
    private boolean faceTrackingModeEnabled;
    private ArExperienceMode requestedMode = ArExperienceMode.SURFACE;
    @Nullable
    private Scene.OnUpdateListener faceTrackingUpdateListener;
    @Nullable
    private AugmentedFace trackedFace;
    @Nullable
    private Integer arModeStatusOverrideResId;
    @Nullable
    private Integer pendingArModeToastResId;

    private ArFlashcardViewModel viewModel;
    private List<Quiz> availableQuizzes = new ArrayList<>();
    private List<Question> selectedQuestions = new ArrayList<>();
    private int selectedQuestionIndex = 0;
    private int answeredQuestionCount = 0;
    private int correctAnswerCount = 0;
    private boolean currentQuestionAnswered;
    private boolean isQuizCompleted;
    private boolean questionSessionInitialized;
    private boolean noQuestionsToastShown;
    private boolean isGeneratingAiInsight;
    private long quizSessionStartedAtMillis;
    private String activeQuizId;
    private String activeQuizSubject = "General";
    private final List<String> userAnswers = new ArrayList<>();
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
                if (modelClass.isAssignableFrom(ArFlashcardViewModel.class)) {
                    return modelClass.cast(new ArFlashcardViewModel(requireContext()));
                }
                throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
            }
        }).get(ArFlashcardViewModel.class);

        setupAnswerControls();
        setupQuizSelector();
        setupArModeControls();
        binding.btnRetryAr.setOnClickListener(v -> retryArStartup());
        binding.btnArFallback.setOnClickListener(v -> navigateToStandardFlashcards());
        binding.btnToggleArControls.setOnClickListener(v -> {
            controlsExpanded = !controlsExpanded;
            applyArControlPanelState();
        });
        applyArControlPanelState();
        binding.btnPlaceArCard.setOnClickListener(v -> handlePlaceCardTap());

        binding.btnClearAr.setOnClickListener(v -> {
            if (arSceneView == null) {
                Toast.makeText(requireContext(), "AR scene is not ready.", Toast.LENGTH_SHORT).show();
                return;
            }
            clearPlacedNodes();
            Toast.makeText(requireContext(), "Cleared", Toast.LENGTH_SHORT).show();
        });

        startArIfPossible();
    }

    private void setupArModeControls() {
        if (binding == null) {
            return;
        }

        syncModeToggleSelection();
        binding.toggleArMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.btn_ar_mode_face) {
                selectArMode(ArExperienceMode.FACE);
            } else {
                selectArMode(ArExperienceMode.SURFACE);
            }
        });
        updateArModeStatusMessage();
    }

    private void selectArMode(ArExperienceMode mode) {
        if (mode == ArExperienceMode.FACE && !isFaceModeAllowedOnDevice()) {
            requestedMode = ArExperienceMode.SURFACE;
            arModeStatusOverrideResId = R.string.ar_mode_face_unavailable_device;
            pendingArModeToastResId = R.string.ar_mode_face_unavailable_device;
            syncModeToggleSelection();
            applyArModeUi();
            return;
        }

        if (requestedMode == mode) {
            return;
        }

        requestedMode = mode;
        arModeStatusOverrideResId = null;
        syncModeToggleSelection();
        restartArSessionForModeChange();
    }

    private void restartArSessionForModeChange() {
        if (binding == null || arSceneView == null || !isAdded()) {
            return;
        }

        if (!hasCameraPermission()) {
            startArIfPossible();
            return;
        }

        clearPlacedNodes();
        trackedFace = null;
        binding.cardArFaceQuiz.setVisibility(View.GONE);

        try {
            arSceneView.pause();
        } catch (Throwable t) {
            Log.w(TAG, "Failed to pause AR scene during mode switch", t);
        }

        try {
            Session previous = arSceneView.getSession();
            if (previous != null) {
                previous.close();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to close previous AR session during mode switch", t);
        }

        try {
            Session session = new Session(requireActivity());
            configureSessionForStability(session);
            arSceneView.setupSession(session);
            if (isResumed()) {
                arSceneView.resume();
            }
            arAvailable = true;
            binding.layoutArUnavailable.setVisibility(View.GONE);
            binding.cardArInstruction.setVisibility(View.VISIBLE);
            binding.cardArControls.setVisibility(View.VISIBLE);
            binding.arSceneView.setVisibility(View.VISIBLE);
            applyArModeUi();
            applyArControlPanelState();
        } catch (Throwable t) {
            Log.e(TAG, "Failed to switch AR mode", t);
            if (handleFaceModeRuntimeFailure(t)) {
                return;
            }
            showArUnavailable(getString(R.string.ar_session_not_ready, safeMessage(t)));
        }
    }

    private boolean isFaceModeAllowedOnDevice() {
        // Let all devices attempt face mode; runtime camera/config checks still fallback safely.
        return true;
    }

    private void syncModeToggleSelection() {
        if (binding == null) {
            return;
        }

        int target = requestedMode == ArExperienceMode.FACE
                ? R.id.btn_ar_mode_face
                : R.id.btn_ar_mode_surface;
        if (binding.toggleArMode.getCheckedButtonId() != target) {
            binding.toggleArMode.check(target);
        }
    }

    private void updateArModeStatusMessage() {
        if (binding == null) {
            return;
        }

        if (arModeStatusOverrideResId != null) {
            binding.tvArModeStatus.setText(arModeStatusOverrideResId);
            return;
        }

        binding.tvArModeStatus.setText(faceTrackingModeEnabled
                ? R.string.ar_mode_face_active
                : R.string.ar_mode_surface_active);
    }

    private void handlePlaceCardTap() {
        if (arSceneView == null) {
            Toast.makeText(requireContext(), "AR scene is not ready.", Toast.LENGTH_SHORT).show();
            return;
        }

        Frame frame = arSceneView.getArFrame();
        if (frame == null) {
            Toast.makeText(requireContext(), getString(R.string.ar_place_card_retry), Toast.LENGTH_SHORT).show();
            return;
        }

        if (shouldUseArTapAnswerPlacement()) {
            if (!placeCurrentQuestionCardsInFrontOfCamera(frame)) {
                Toast.makeText(requireContext(), getString(R.string.ar_place_card_retry), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String text = getCurrentCardText();
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.ar_no_questions_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!placeFlashcardInFrontOfCamera(frame, text)) {
            Toast.makeText(requireContext(), getString(R.string.ar_place_card_retry), Toast.LENGTH_SHORT).show();
        }
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

        ArCoreApk.Availability availability;
        try {
            availability = ArCoreApk.getInstance().checkAvailability(requireContext());
        } catch (Throwable t) {
            Log.e(TAG, "ARCore availability check failed", t);
            showArUnavailable(getString(R.string.ar_install_check_failed, safeMessage(t)));
            return;
        }

        if (!availability.isSupported()) {
            showArUnavailable("This device does not support ARCore.");
            return;
        }

        if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            showArUnavailable("This device does not support ARCore.");
            return;
        }

        if (availability.isTransient()) {
            if (arAvailabilityRetryCount >= MAX_AR_AVAILABILITY_RETRIES) {
                showArUnavailable(getString(R.string.ar_install_check_failed,
                        "Timed out while checking ARCore availability."));
                return;
            }
            arAvailabilityRetryCount++;
            binding.getRoot().postDelayed(this::startArIfPossible, 300L);
            return;
        }
        arAvailabilityRetryCount = 0;

        // requestInstall should run after the Fragment has reached RESUMED.
        if (!isResumed()) {
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
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception during ARCore install flow", e);
            showArUnavailable(getString(R.string.ar_install_check_failed, safeMessage(e)));
            return;
        } catch (Exception e) {
            showArUnavailable(getString(R.string.ar_install_check_failed, safeMessage(e)));
            return;
        }

        if (arSceneView == null) {
            arSceneView = binding.arSceneView;
        }
        try {
            Session session = arSceneView.getSession();
            if (session == null) {
                session = new Session(requireActivity());
                configureSessionForStability(session);
                arSceneView.setupSession(session);
            } else {
                configureSessionForStability(session);
            }
            attachSceneTouchListenerIfNeeded();
            attachFaceTrackingUpdateListenerIfNeeded();
            arAvailable = true;
            binding.layoutArUnavailable.setVisibility(View.GONE);
            binding.cardArInstruction.setVisibility(View.VISIBLE);
            binding.cardArControls.setVisibility(View.VISIBLE);
            binding.arSceneView.setVisibility(View.VISIBLE);
            applyArModeUi();
            applyArControlPanelState();
        } catch (UnavailableArcoreNotInstalledException e) {
            showArUnavailable(getString(R.string.ar_install_required));
        } catch (UnavailableApkTooOldException | UnavailableSdkTooOldException e) {
            showArUnavailable(getString(R.string.ar_install_required));
        } catch (UnavailableDeviceNotCompatibleException e) {
            showArUnavailable("This device does not support ARCore.");
        } catch (Exception e) {
            if (handleFaceModeRuntimeFailure(e)) {
                return;
            }
            showArUnavailable(getString(R.string.ar_session_not_ready, safeMessage(e)));
        } catch (Throwable t) {
            Log.e(TAG, "Unexpected AR session startup failure", t);
            if (handleFaceModeRuntimeFailure(t)) {
                return;
            }
            showArUnavailable(getString(R.string.ar_session_not_ready, safeMessage(t)));
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
            if (faceTrackingModeEnabled) return;

            if (hitTestResult != null && hitTestResult.getNode() != null) {
                return;
            }

            showTapFocusIndicator(motionEvent);

            Frame frame = arSceneView.getArFrame();
            if (frame == null) return;

            if (shouldUseArTapAnswerPlacement()) {
                List<HitResult> hits = frame.hitTest(motionEvent);
                HitResult placementHit = pickPlacementHit(hits);
                if (placementHit != null) {
                    placeCurrentQuestionCards(placementHit.createAnchor());
                    return;
                }

                boolean fallbackPlaced = placeCurrentQuestionCardsInFrontOfCamera(frame);
                if (!fallbackPlaced) {
                    Toast.makeText(requireContext(), getString(R.string.ar_surface_not_ready), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            String text = getCurrentCardText();
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Enter flashcard text first", Toast.LENGTH_SHORT).show();
                return;
            }

            List<HitResult> hits = frame.hitTest(motionEvent);
            HitResult placementHit = pickPlacementHit(hits);
            if (placementHit != null) {
                placeFlashcard(placementHit.createAnchor(), text);
                return;
            }

            boolean fallbackPlaced = placeFlashcardInFrontOfCamera(frame, text);
            if (!fallbackPlaced) {
                Toast.makeText(requireContext(), getString(R.string.ar_surface_not_ready), Toast.LENGTH_SHORT).show();
            }
        });
        sceneTouchListenerAttached = true;
    }

    private void attachFaceTrackingUpdateListenerIfNeeded() {
        if (arSceneView == null || faceTrackingUpdateListener != null) {
            return;
        }

        faceTrackingUpdateListener = frameTime -> {
            if (binding == null || arSceneView == null) {
                return;
            }

            if (!faceTrackingModeEnabled) {
                if (binding.cardArFaceQuiz.getVisibility() == View.VISIBLE) {
                    binding.cardArFaceQuiz.setVisibility(View.GONE);
                }
                trackedFace = null;
                maybeAutoPlaceCurrentQuestionCards();
                return;
            }

            Frame frame = arSceneView.getArFrame();
            if (frame == null) {
                binding.cardArFaceQuiz.setVisibility(View.GONE);
                return;
            }

            for (AugmentedFace face : frame.getUpdatedTrackables(AugmentedFace.class)) {
                if (face.getTrackingState() == TrackingState.TRACKING) {
                    trackedFace = face;
                    break;
                }
                if (trackedFace == face && face.getTrackingState() == TrackingState.STOPPED) {
                    trackedFace = null;
                }
            }

            if (trackedFace == null || trackedFace.getTrackingState() != TrackingState.TRACKING) {
                binding.cardArFaceQuiz.setVisibility(View.GONE);
                return;
            }

            updateFaceQuizCardPosition(trackedFace);
        };

        arSceneView.getScene().addOnUpdateListener(faceTrackingUpdateListener);
    }

    private void updateFaceQuizCardPosition(AugmentedFace face) {
        if (binding == null || arSceneView == null) {
            return;
        }

        float[] facePoint = face.getCenterPose().getTranslation();
        Vector3 worldPoint = new Vector3(
                facePoint[0],
                facePoint[1] + FACE_CARD_VERTICAL_OFFSET_METERS,
                facePoint[2]
        );
        Vector3 screenPoint = arSceneView.getScene().getCamera().worldToScreenPoint(worldPoint);

        View card = binding.cardArFaceQuiz;
        if (card.getWidth() == 0 || card.getHeight() == 0) {
            card.measure(
                    View.MeasureSpec.makeMeasureSpec(binding.getRoot().getWidth(), View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(binding.getRoot().getHeight(), View.MeasureSpec.AT_MOST)
            );
        }

        float cardWidth = card.getWidth() > 0 ? card.getWidth() : card.getMeasuredWidth();
        float cardHeight = card.getHeight() > 0 ? card.getHeight() : card.getMeasuredHeight();

        float margin = dpToPx(FACE_CARD_SCREEN_MARGIN_DP);
        float parentWidth = binding.getRoot().getWidth();
        float parentHeight = binding.getRoot().getHeight();
        if (parentWidth <= 0 || parentHeight <= 0) {
            return;
        }

        float x = clamp(screenPoint.x - (cardWidth / 2f), margin, Math.max(margin, parentWidth - cardWidth - margin));
        float y = clamp(screenPoint.y - cardHeight - dpToPx(16f), margin, Math.max(margin, parentHeight - cardHeight - margin));

        card.setX(x);
        card.setY(y);
        if (card.getVisibility() != View.VISIBLE) {
            card.setVisibility(View.VISIBLE);
        }
    }

    private float clamp(float value, float min, float max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    @Nullable
    private HitResult pickPlacementHit(List<HitResult> hits) {
        for (HitResult hit : hits) {
            if (hit.getTrackable() instanceof Plane) {
                Plane plane = (Plane) hit.getTrackable();
                if (plane.getTrackingState() == TrackingState.TRACKING && plane.isPoseInPolygon(hit.getHitPose())) {
                    return hit;
                }
            }
        }

        return null;
    }

    private boolean placeFlashcardInFrontOfCamera(Frame frame, String text) {
        if (arSceneView == null || arSceneView.getSession() == null) {
            return false;
        }

        try {
            Camera camera = frame.getCamera();
            if (camera.getTrackingState() != TrackingState.TRACKING) {
                return false;
            }

            Pose cameraPose = camera.getPose();
            Pose cardPose = cameraPose.compose(Pose.makeTranslation(
                    0f,
                    -0.08f,
                    -CARD_PLACEMENT_DISTANCE_METERS
            ));

            Anchor anchor = arSceneView.getSession().createAnchor(cardPose);
            placeFlashcard(anchor, text);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to place AR card in front of camera", t);
            return false;
        }
    }

    private boolean shouldUseArTapAnswerPlacement() {
        return !faceTrackingModeEnabled
                && getCurrentQuestion() != null
                && !isQuizCompleted;
    }

    private boolean placeCurrentQuestionCardsInFrontOfCamera(Frame frame) {
        if (arSceneView == null || arSceneView.getSession() == null) {
            return false;
        }

        try {
            Camera camera = frame.getCamera();
            if (camera.getTrackingState() != TrackingState.TRACKING) {
                return false;
            }

            Pose cameraPose = camera.getPose();
            Pose cardPose = cameraPose.compose(Pose.makeTranslation(
                    0f,
                    -0.06f,
                    -CARD_PLACEMENT_DISTANCE_METERS
            ));

            Anchor anchor = arSceneView.getSession().createAnchor(cardPose);
            placeCurrentQuestionCards(anchor);
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to place AR quiz cards in front of camera", t);
            return false;
        }
    }

    private void placeCurrentQuestionCards(Anchor anchor) {
        if (arSceneView == null) {
            try {
                anchor.detach();
            } catch (Throwable detachError) {
                Log.w(TAG, "Failed to detach quiz anchor when AR scene is unavailable", detachError);
            }
            return;
        }

        Question question = getCurrentQuestion();
        if (question == null || isQuizCompleted) {
            try {
                anchor.detach();
            } catch (Throwable detachError) {
                Log.w(TAG, "Failed to detach stale quiz anchor", detachError);
            }
            return;
        }

        clearPlacedNodes();

        String progress = getString(
                R.string.ar_question_progress,
                selectedQuestionIndex + 1,
                selectedQuestions.size()
        );
        String questionText = nonEmptyOrFallback(
                question.getQuestionText(),
                getString(R.string.ar_question_text_fallback)
        );
        placeFlashcard(anchor, progress + "\n" + questionText);
        setAnswerFeedback(getString(R.string.ar_answer_select_prompt));
    }

    private void faceNodeToCamera(Node node) {
        if (arSceneView == null) {
            return;
        }
        Vector3 cardPosition = node.getWorldPosition();
        Vector3 cameraPosition = arSceneView.getScene().getCamera().getWorldPosition();
        Vector3 forward = Vector3.subtract(cameraPosition, cardPosition);
        if (forward.length() < 0.0001f) {
            return;
        }
        node.setWorldRotation(Quaternion.lookRotation(forward, Vector3.up()));
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
        installRequested = false;
        arAvailabilityRetryCount = 0;
        startArIfPossible();
    }

    private boolean handleFaceModeRuntimeFailure(Throwable throwable) {
        if (requestedMode != ArExperienceMode.FACE) {
            return false;
        }

        Log.w(TAG, "Face AR mode failed at runtime, switching to surface mode", throwable);
        requestedMode = ArExperienceMode.SURFACE;
        faceTrackingModeEnabled = false;
        arModeStatusOverrideResId = R.string.ar_mode_face_fallback;
        pendingArModeToastResId = R.string.ar_mode_face_fallback;
        syncModeToggleSelection();

        try {
            if (arSceneView != null) {
                arSceneView.pause();
                Session previous = arSceneView.getSession();
                if (previous != null) {
                    previous.close();
                }
            }
        } catch (Throwable closeError) {
            Log.w(TAG, "Failed to close AR session during face-mode fallback", closeError);
        }

        if (binding != null) {
            binding.getRoot().post(this::startArIfPossible);
        }
        return true;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable instanceof IllegalArgumentException) {
            return "Unsupported AR camera configuration on this device.";
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }

    private String getCurrentCardText() {
        Question currentQuestion = getCurrentQuestion();
        if (currentQuestion != null) {
            String questionText = currentQuestion.getQuestionText();
            if (questionText != null && !questionText.trim().isEmpty()) {
                return questionText.trim();
            }
        }

        if (binding == null || binding.etFlashcardText.getText() == null) {
            return "";
        }
        return binding.etFlashcardText.getText().toString().trim();
    }

    @Nullable
    private Question getCurrentQuestion() {
        if (selectedQuestions == null || selectedQuestions.isEmpty()) {
            return null;
        }
        if (selectedQuestionIndex < 0 || selectedQuestionIndex >= selectedQuestions.size()) {
            selectedQuestionIndex = 0;
        }
        return selectedQuestions.get(selectedQuestionIndex);
    }

    private void configureSessionForStability(Session session) {
        faceTrackingModeEnabled = false;
        arModeStatusOverrideResId = null;

        if (requestedMode == ArExperienceMode.FACE) {
            if (!isFaceModeAllowedOnDevice()) {
                requestedMode = ArExperienceMode.SURFACE;
                arModeStatusOverrideResId = R.string.ar_mode_face_unavailable_device;
                pendingArModeToastResId = R.string.ar_mode_face_unavailable_device;
            } else {
                try {
                    CameraConfigFilter frontFilter = new CameraConfigFilter(session)
                            .setFacingDirection(CameraConfig.FacingDirection.FRONT)
                            .setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
                    List<CameraConfig> frontConfigs = session.getSupportedCameraConfigs(frontFilter);
                    if (frontConfigs.isEmpty()) {
                        frontFilter = new CameraConfigFilter(session)
                                .setFacingDirection(CameraConfig.FacingDirection.FRONT);
                        frontConfigs = session.getSupportedCameraConfigs(frontFilter);
                    }
                    if (!frontConfigs.isEmpty()) {
                        session.setCameraConfig(frontConfigs.get(0));
                        Config faceConfig = session.getConfig();
                        faceConfig.setDepthMode(Config.DepthMode.DISABLED);
                        faceConfig.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
                        faceConfig.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
                        faceConfig.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                        faceConfig.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                        faceConfig.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
                        session.configure(faceConfig);
                        faceTrackingModeEnabled = true;
                        return;
                    }
                    requestedMode = ArExperienceMode.SURFACE;
                    arModeStatusOverrideResId = R.string.ar_mode_face_fallback;
                    pendingArModeToastResId = R.string.ar_mode_face_fallback;
                } catch (Throwable t) {
                    Log.w(TAG, "Face AR mode unavailable, falling back to surface mode", t);
                    requestedMode = ArExperienceMode.SURFACE;
                    arModeStatusOverrideResId = R.string.ar_mode_face_fallback;
                    pendingArModeToastResId = R.string.ar_mode_face_fallback;
                }
            }
        }

        try {
            CameraConfigFilter filter = new CameraConfigFilter(session)
                    .setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30));
            List<CameraConfig> configs = session.getSupportedCameraConfigs(filter);
            if (!configs.isEmpty()) {
                session.setCameraConfig(configs.get(0));
            }
        } catch (Throwable t) {
            Log.w(TAG, "Unable to apply preferred AR camera config", t);
        }

        try {
            Config config = session.getConfig();
            config.setDepthMode(Config.DepthMode.DISABLED);
            config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
            config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
            config.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            config.setAugmentedFaceMode(Config.AugmentedFaceMode.DISABLED);
            session.configure(config);
            faceTrackingModeEnabled = false;
        } catch (Throwable t) {
            Log.w(TAG, "Unable to apply stable AR session settings", t);
        }
    }

    private void applyArModeUi() {
        if (binding == null) {
            return;
        }

        boolean showSurfaceControls = !faceTrackingModeEnabled;
        binding.cardArQuestionPanel.setVisibility(showSurfaceControls ? View.VISIBLE : View.GONE);
        binding.tilFlashcardText.setVisibility(showSurfaceControls ? View.VISIBLE : View.GONE);
        binding.btnClearAr.setVisibility(showSurfaceControls ? View.VISIBLE : View.GONE);
        binding.btnPlaceArCard.setVisibility(showSurfaceControls ? View.VISIBLE : View.GONE);
        binding.cardArFaceQuiz.setVisibility(faceTrackingModeEnabled ? View.INVISIBLE : View.GONE);
        binding.tvArInstruction.setText(faceTrackingModeEnabled
                ? R.string.ar_instruction_face_mode
                : R.string.ar_instruction_compact);
        syncModeToggleSelection();
        updateArModeStatusMessage();
        if (pendingArModeToastResId != null) {
            Toast.makeText(requireContext(), getString(pendingArModeToastResId), Toast.LENGTH_SHORT).show();
            pendingArModeToastResId = null;
        }
        updateFaceQuizCardContent();
        updatePortraitOverflowHint();
        maybeAutoPlaceCurrentQuestionCards();
        applyArControlPanelState();
    }

    private void applyArControlPanelState() {
        if (binding == null) {
            return;
        }

        boolean hasActiveQuestion = getCurrentQuestion() != null;
        boolean showSetupControls = controlsExpanded || faceTrackingModeEnabled || !hasActiveQuestion;

        binding.layoutArControlsBody.setVisibility(View.VISIBLE);
        binding.tvArModeTitle.setVisibility(showSetupControls ? View.VISIBLE : View.GONE);
        binding.toggleArMode.setVisibility(showSetupControls ? View.VISIBLE : View.GONE);
        binding.tvArModeStatus.setVisibility(showSetupControls ? View.VISIBLE : View.GONE);
        binding.tilQuizSelector.setVisibility(showSetupControls ? View.VISIBLE : View.GONE);
        binding.tilFlashcardText.setVisibility(showSetupControls && !faceTrackingModeEnabled ? View.VISIBLE : View.GONE);
        binding.layoutArSurfaceActions.setVisibility(showSetupControls && !faceTrackingModeEnabled ? View.VISIBLE : View.GONE);
        binding.cardArQuestionPanel.setVisibility(!faceTrackingModeEnabled ? View.VISIBLE : View.GONE);

        binding.btnToggleArControls.setText(
                showSetupControls ? R.string.ar_hide_controls : R.string.ar_show_controls
        );
    }

    private void placeFlashcard(Anchor anchor, String text) {
        ViewRenderable.builder()
                .setView(requireContext(), buildCardView(text))
                .build()
                .thenAccept(renderable -> {
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arSceneView.getScene());

                    TransformationSystem ts = new TransformationSystem(
                            requireActivity().getResources().getDisplayMetrics(),
                            new com.google.ar.sceneform.ux.FootprintSelectionVisualizer());
                    TransformableNode node = new TransformableNode(ts);
                    node.setParent(anchorNode);
                    node.setRenderable(renderable);
                    node.setLocalPosition(new Vector3(0f, CARD_VERTICAL_OFFSET_METERS, 0f));
                    node.getTranslationController().setEnabled(false);
                    node.getScaleController().setEnabled(false);
                    node.getRotationController().setEnabled(false);
                    faceNodeToCamera(node);
                    placedNodes.add(anchorNode);
                })
                .exceptionally(t -> {
                    Toast.makeText(requireContext(), "Failed to place card", Toast.LENGTH_SHORT).show();
                    try {
                        anchor.detach();
                    } catch (Throwable detachError) {
                        Log.w(TAG, "Failed to detach anchor after render failure", detachError);
                    }
                    return null;
                });
    }

    private void setupAnswerControls() {
        if (binding == null) {
            return;
        }

        binding.btnSubmitArAnswer.setOnClickListener(v -> submitCurrentAnswer());
        binding.btnNextArQuestion.setOnClickListener(v -> handleNextQuestionTap());
        binding.btnArFaceOptionA.setOnClickListener(v -> submitFaceAnswer("A"));
        binding.btnArFaceOptionB.setOnClickListener(v -> submitFaceAnswer("B"));
        binding.btnArFaceOptionC.setOnClickListener(v -> submitFaceAnswer("C"));
        binding.btnArFaceOptionD.setOnClickListener(v -> submitFaceAnswer("D"));

        binding.rgArOptions.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != View.NO_ID && !currentQuestionAnswered && !isQuizCompleted) {
                binding.tvArAnswerFeedback.setText(getString(R.string.ar_answer_select_prompt));
            }
        });

        updateQuestionPanel();
    }

    private void submitCurrentAnswer() {
        if (binding == null) {
            return;
        }

        String selectedAnswer = getSelectedAnswerLetter();
        if (selectedAnswer.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.ar_answer_pick_required), Toast.LENGTH_SHORT).show();
            return;
        }

        submitSelectedAnswer(selectedAnswer, false);
    }

    private void submitFaceAnswer(String selectedAnswer) {
        if (!faceTrackingModeEnabled) {
            return;
        }
        submitSelectedAnswer(selectedAnswer, true);
    }

    private void submitSelectedAnswer(String selectedAnswer, boolean autoAdvance) {
        if (binding == null) {
            return;
        }

        Question question = getCurrentQuestion();
        if (question == null) {
            Toast.makeText(requireContext(), getString(R.string.ar_no_questions_loaded), Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentQuestionAnswered || isQuizCompleted) {
            return;
        }

        ensureUserAnswersCapacity(selectedQuestions.size());
        userAnswers.set(selectedQuestionIndex, selectedAnswer);

        String correctAnswer = resolveCorrectAnswerLetter(question);
        answeredQuestionCount++;

        boolean isCorrect = selectedAnswer.equals(correctAnswer);
        if (isCorrect) {
            correctAnswerCount++;
            setAnswerFeedback(getString(R.string.ar_answer_correct, selectedAnswer));
        } else {
            String fallbackCorrectAnswer = correctAnswer.isEmpty() ? "?" : correctAnswer;
            setAnswerFeedback(getString(
                    R.string.ar_answer_wrong,
                    selectedAnswer,
                    fallbackCorrectAnswer
            ));
        }

        currentQuestionAnswered = true;
        binding.btnSubmitArAnswer.setEnabled(false);
        setOptionInputsEnabled(false);
        setFaceOptionInputsEnabled(false);
        if (selectedQuestionIndex >= selectedQuestions.size() - 1) {
            completeArQuizSession();
        } else {
            binding.btnNextArQuestion.setEnabled(true);
            if (autoAdvance) {
                binding.getRoot().postDelayed(() -> {
                    if (binding == null || isQuizCompleted) {
                        return;
                    }
                    boolean moved = advanceSelectedQuestionIfNeeded();
                    if (!moved) {
                        completeArQuizSession();
                        return;
                    }
                    updateQuestionPanel();
                    placeCurrentQuestionCardsInFrontOfCameraIfReady();
                }, 450L);
            }
        }
        updateScoreLabel();
    }

    private void handleNextQuestionTap() {
        if (isQuizCompleted) {
            return;
        }

        if (!currentQuestionAnswered) {
            Toast.makeText(requireContext(), getString(R.string.ar_submit_before_next), Toast.LENGTH_SHORT).show();
            return;
        }

        boolean moved = advanceSelectedQuestionIfNeeded();
        if (!moved) {
            completeArQuizSession();
            return;
        }

        updateQuestionPanel();
        placeCurrentQuestionCardsInFrontOfCameraIfReady();
    }

    private void placeCurrentQuestionCardsInFrontOfCameraIfReady() {
        maybeAutoPlaceCurrentQuestionCards();
    }

    private void maybeAutoPlaceCurrentQuestionCards() {
        if (arSceneView == null || !shouldUseArTapAnswerPlacement()) {
            return;
        }

        // Stop retrying once a card anchor is already active in the scene.
        if (!placedNodes.isEmpty()) {
            return;
        }

        Frame frame = arSceneView.getArFrame();
        if (frame == null) {
            return;
        }

        placeCurrentQuestionCardsInFrontOfCamera(frame);
    }

    private void completeArQuizSession() {
        if (isQuizCompleted) {
            return;
        }
        isQuizCompleted = true;
        currentQuestionAnswered = true;

        if (binding != null) {
            binding.btnSubmitArAnswer.setEnabled(false);
            binding.btnNextArQuestion.setEnabled(false);
            binding.btnNextArQuestion.setText(R.string.ar_quiz_completed);
            setOptionInputsEnabled(false);
            setFaceOptionInputsEnabled(false);
            setAnswerFeedback(getString(
                    R.string.ar_quiz_complete_summary,
                    correctAnswerCount,
                    selectedQuestions.size()
            ));
        }

        updateScoreLabel();
        openArQuizResult();
    }

    private void openArQuizResult() {
        if (!isAdded() || selectedQuestions == null || selectedQuestions.isEmpty()) {
            return;
        }

        ArrayList<Bundle> questionBundles = buildQuestionBundlesForResult();
        ArrayList<String> userAnswerList = buildUserAnswersForResult();
        ArrayList<String> wrongQuestions = buildWrongQuestionTextsForResult();

        int elapsedMinutes = 0;
        if (quizSessionStartedAtMillis > 0L) {
            elapsedMinutes = (int) ((System.currentTimeMillis() - quizSessionStartedAtMillis) / 60000L);
        }

        Intent resultIntent = new Intent(requireContext(), com.example.studysync_project.ui.quiz.QuizResultActivity.class);
        resultIntent.putExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_SCORE, correctAnswerCount);
        resultIntent.putExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_TOTAL, selectedQuestions.size());
        resultIntent.putExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_SUBJECT, activeQuizSubject);
        resultIntent.putExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_QUIZ_ID, activeQuizId);
        resultIntent.putStringArrayListExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_WRONG_QUESTIONS, wrongQuestions);
        resultIntent.putParcelableArrayListExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_QUESTIONS, questionBundles);
        resultIntent.putStringArrayListExtra(com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_USER_ANSWERS, userAnswerList);
        resultIntent.putExtra(
                com.example.studysync_project.ui.quiz.QuizResultActivity.EXTRA_TIME_TAKEN_MINUTES,
                Math.max(elapsedMinutes, 0)
        );
        startActivity(resultIntent);
    }

    private ArrayList<Bundle> buildQuestionBundlesForResult() {
        ArrayList<Bundle> questionBundles = new ArrayList<>();
        if (selectedQuestions == null) {
            return questionBundles;
        }

        for (Question question : selectedQuestions) {
            if (question == null) {
                continue;
            }

            Bundle bundle = new Bundle();
            bundle.putString(
                    "question",
                    nonEmptyOrFallback(question.getQuestionText(), getString(R.string.ar_question_text_fallback))
            );
            bundle.putString("optionA", nonEmptyOrFallback(question.getOptionA(), getString(R.string.ar_option_fallback)));
            bundle.putString("optionB", nonEmptyOrFallback(question.getOptionB(), getString(R.string.ar_option_fallback)));
            bundle.putString("optionC", nonEmptyOrFallback(question.getOptionC(), getString(R.string.ar_option_fallback)));
            bundle.putString("optionD", nonEmptyOrFallback(question.getOptionD(), getString(R.string.ar_option_fallback)));
            bundle.putString("correctAnswer", resolveCorrectAnswerLetter(question));
            questionBundles.add(bundle);
        }

        return questionBundles;
    }

    private ArrayList<String> buildUserAnswersForResult() {
        ArrayList<String> answers = new ArrayList<>();
        if (selectedQuestions == null) {
            return answers;
        }

        for (int i = 0; i < selectedQuestions.size(); i++) {
            String answer = i < userAnswers.size() ? userAnswers.get(i) : null;
            answers.add(answer != null ? answer : "");
        }
        return answers;
    }

    private ArrayList<String> buildWrongQuestionTextsForResult() {
        ArrayList<String> wrongQuestions = new ArrayList<>();
        if (selectedQuestions == null) {
            return wrongQuestions;
        }

        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question question = selectedQuestions.get(i);
            if (question == null) {
                continue;
            }

            String userAnswer = i < userAnswers.size() ? userAnswers.get(i) : null;
            String correctAnswer = resolveCorrectAnswerLetter(question);

            if (userAnswer == null || userAnswer.isEmpty() || !userAnswer.equals(correctAnswer)) {
                wrongQuestions.add(
                        nonEmptyOrFallback(question.getQuestionText(), getString(R.string.ar_question_text_fallback))
                );
            }
        }

        return wrongQuestions;
    }

    private void requestAiInsightForCompletedQuiz() {
        if (binding == null || selectedQuestions.isEmpty()) {
            return;
        }

        binding.tvArAiInsight.setVisibility(View.VISIBLE);

        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            showFallbackAiInsight();
            return;
        }

        if (!NetworkUtil.isNetworkAvailable(requireContext())) {
            showFallbackAiInsight();
            return;
        }

        if (isGeneratingAiInsight) {
            return;
        }

        isGeneratingAiInsight = true;
        binding.progressArAiInsight.setVisibility(View.VISIBLE);
        binding.tvArAiInsight.setText(getString(R.string.ar_ai_generating));

        String subject = activeQuizSubject != null && !activeQuizSubject.trim().isEmpty()
                ? activeQuizSubject.trim() : "General";
        String wrongTopics = buildWrongTopicsForAi();

        GeminiApiClient.analyzePerformance(subject, correctAnswerCount, selectedQuestions.size(), wrongTopics)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        isGeneratingAiInsight = false;
                        if (binding == null) {
                            return;
                        }

                        binding.progressArAiInsight.setVisibility(View.GONE);
                        if (!response.isSuccessful() || response.body() == null) {
                            showFallbackAiInsight();
                            return;
                        }

                        String insight = extractGeminiText(response.body());
                        if (insight == null || insight.trim().isEmpty()) {
                            showFallbackAiInsight();
                            return;
                        }

                        binding.tvArAiInsight.setText(insight.trim());
                        binding.tvArFaceHint.setText(insight.trim());
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        isGeneratingAiInsight = false;
                        if (binding == null) {
                            return;
                        }
                        binding.progressArAiInsight.setVisibility(View.GONE);
                        showFallbackAiInsight();
                    }
                });
    }

    private void showFallbackAiInsight() {
        if (binding == null) {
            return;
        }

        int total = selectedQuestions.size();
        int percent = total > 0 ? (correctAnswerCount * 100) / total : 0;
        if (percent >= 80) {
            binding.tvArAiInsight.setText(getString(R.string.ar_ai_fallback_high));
        } else if (percent >= 60) {
            binding.tvArAiInsight.setText(getString(R.string.ar_ai_fallback_mid));
        } else {
            binding.tvArAiInsight.setText(getString(R.string.ar_ai_fallback_low));
        }
        binding.tvArFaceHint.setText(binding.tvArAiInsight.getText());
    }

    private String buildWrongTopicsForAi() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < selectedQuestions.size(); i++) {
            Question question = selectedQuestions.get(i);
            String userAnswer = i < userAnswers.size() ? userAnswers.get(i) : null;
            String correctAnswer = resolveCorrectAnswerLetter(question);
            if (userAnswer == null || userAnswer.isEmpty() || !userAnswer.equals(correctAnswer)) {
                builder.append("- ")
                        .append(nonEmptyOrFallback(question.getQuestionText(), getString(R.string.ar_question_text_fallback)))
                        .append(" (your: ")
                        .append(userAnswer == null || userAnswer.isEmpty() ? "-" : userAnswer)
                        .append(", correct: ")
                        .append(correctAnswer == null || correctAnswer.isEmpty() ? "-" : correctAnswer)
                        .append(")\n");
            }
        }
        return builder.toString().trim();
    }

    private String extractGeminiText(JsonObject body) {
        if (body == null) {
            return null;
        }
        try {
            return body.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private void saveArQuizAttempt() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null || selectedQuestions.isEmpty()) {
            return;
        }

        int total = selectedQuestions.size();
        int percent = (correctAnswerCount * 100) / total;
        String quizRef = activeQuizId != null && !activeQuizId.trim().isEmpty()
                ? activeQuizId : activeQuizSubject;

        QuizAttempt attempt = new QuizAttempt(userId, quizRef, total, correctAnswerCount, percent, 0);
        attempt.setAttemptId(IdUtil.generateId("attempt"));
        attempt.setPassed(percent >= 60);
        attempt.setAnswers(userAnswers.toString());
        new QuizAttemptRepository(requireContext()).saveQuizAttempt(attempt, userId);
    }

    private void ensureUserAnswersCapacity(int size) {
        while (userAnswers.size() < size) {
            userAnswers.add(null);
        }
        while (userAnswers.size() > size) {
            userAnswers.remove(userAnswers.size() - 1);
        }
    }

    private void resetQuizSessionState() {
        selectedQuestions = new ArrayList<>();
        selectedQuestionIndex = 0;
        answeredQuestionCount = 0;
        correctAnswerCount = 0;
        currentQuestionAnswered = false;
        isQuizCompleted = false;
        questionSessionInitialized = false;
        noQuestionsToastShown = false;
        isGeneratingAiInsight = false;
        quizSessionStartedAtMillis = 0L;
        userAnswers.clear();
        clearPlacedNodes();

        if (binding != null) {
            binding.progressArAiInsight.setVisibility(View.GONE);
            binding.tvArAiInsight.setVisibility(View.GONE);
            binding.btnNextArQuestion.setText(R.string.ar_next_question);
            setFaceOptionInputsEnabled(false);
            binding.tvArFaceHint.setText(getString(R.string.ar_face_tracking_wait));
        }
    }

    private void initializeQuizSessionWithQuestions(List<Question> questions) {
        selectedQuestions = new ArrayList<>(questions);
        selectedQuestionIndex = 0;
        answeredQuestionCount = 0;
        correctAnswerCount = 0;
        currentQuestionAnswered = false;
        isQuizCompleted = false;
        isGeneratingAiInsight = false;
        quizSessionStartedAtMillis = System.currentTimeMillis();
        clearPlacedNodes();
        ensureUserAnswersCapacity(selectedQuestions.size());
        updateFlashcardInputFromSelectedQuestion();
        updateQuestionPanel();
        placeCurrentQuestionCardsInFrontOfCameraIfReady();
        questionSessionInitialized = true;
    }

    private void setAnswerFeedback(String feedback) {
        if (binding == null) {
            return;
        }
        binding.tvArAnswerFeedback.setText(feedback);
        binding.tvArFaceHint.setText(feedback);
    }

    private void updateFaceQuizCardContent() {
        if (binding == null) {
            return;
        }

        if (!faceTrackingModeEnabled) {
            binding.cardArFaceQuiz.setVisibility(View.GONE);
            return;
        }

        Question question = getCurrentQuestion();
        if (question == null) {
            binding.tvArFaceQuestion.setText(getString(R.string.ar_question_placeholder));
            binding.tvArFaceProgress.setText(getString(R.string.ar_question_progress_default));
            binding.btnArFaceOptionA.setText(getString(R.string.ar_option_format, "A", getString(R.string.ar_option_fallback)));
            binding.btnArFaceOptionB.setText(getString(R.string.ar_option_format, "B", getString(R.string.ar_option_fallback)));
            binding.btnArFaceOptionC.setText(getString(R.string.ar_option_format, "C", getString(R.string.ar_option_fallback)));
            binding.btnArFaceOptionD.setText(getString(R.string.ar_option_format, "D", getString(R.string.ar_option_fallback)));
            binding.tvArFaceHint.setText(getString(R.string.ar_no_questions_loaded));
            setFaceOptionInputsEnabled(false);
            return;
        }

        binding.tvArFaceProgress.setText(getString(
                R.string.ar_question_progress,
                selectedQuestionIndex + 1,
                selectedQuestions.size()
        ));
        binding.tvArFaceQuestion.setText(nonEmptyOrFallback(
                question.getQuestionText(),
                getString(R.string.ar_question_text_fallback)
        ));
        binding.btnArFaceOptionA.setText(getString(
                R.string.ar_option_format,
                "A",
                nonEmptyOrFallback(question.getOptionA(), getString(R.string.ar_option_fallback))
        ));
        binding.btnArFaceOptionB.setText(getString(
                R.string.ar_option_format,
                "B",
                nonEmptyOrFallback(question.getOptionB(), getString(R.string.ar_option_fallback))
        ));
        binding.btnArFaceOptionC.setText(getString(
                R.string.ar_option_format,
                "C",
                nonEmptyOrFallback(question.getOptionC(), getString(R.string.ar_option_fallback))
        ));
        binding.btnArFaceOptionD.setText(getString(
                R.string.ar_option_format,
                "D",
                nonEmptyOrFallback(question.getOptionD(), getString(R.string.ar_option_fallback))
        ));

        boolean enableFaceAnswers = !isQuizCompleted && !currentQuestionAnswered;
        setFaceOptionInputsEnabled(enableFaceAnswers);
        if (!isQuizCompleted) {
            binding.tvArFaceHint.setText(getString(R.string.ar_face_tap_answer_prompt));
        }
    }

    private String getSelectedAnswerLetter() {
        if (binding == null) {
            return "";
        }

        int checkedId = binding.rgArOptions.getCheckedRadioButtonId();
        if (checkedId == binding.rbArOptionA.getId()) return "A";
        if (checkedId == binding.rbArOptionB.getId()) return "B";
        if (checkedId == binding.rbArOptionC.getId()) return "C";
        if (checkedId == binding.rbArOptionD.getId()) return "D";
        return "";
    }

    private String resolveCorrectAnswerLetter(Question question) {
        String directLetter = extractAnswerLetter(question.getCorrectAnswer());
        if (!directLetter.isEmpty()) {
            return directLetter;
        }

        String normalizedAnswer = normalizeComparableText(question.getCorrectAnswer());
        if (normalizedAnswer.isEmpty()) {
            return "";
        }

        if (normalizedAnswer.equals(normalizeComparableText(question.getOptionA()))) return "A";
        if (normalizedAnswer.equals(normalizeComparableText(question.getOptionB()))) return "B";
        if (normalizedAnswer.equals(normalizeComparableText(question.getOptionC()))) return "C";
        if (normalizedAnswer.equals(normalizeComparableText(question.getOptionD()))) return "D";

        return "";
    }

    private String extractAnswerLetter(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .trim()
                .toUpperCase(Locale.US)
                .replace("(", " ")
                .replace(")", " ")
                .replace(".", " ")
                .replace(":", " ")
                .replace("_", " ")
                .replace("-", " ");

        if (normalized.isEmpty()) {
            return "";
        }

        String[] tokens = normalized.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if ("A".equals(token) || "B".equals(token) || "C".equals(token) || "D".equals(token)) {
                return token;
            }

            if (("OPTION".equals(token) || "ANSWER".equals(token)) && i + 1 < tokens.length) {
                String next = tokens[i + 1];
                if ("A".equals(next) || "B".equals(next) || "C".equals(next) || "D".equals(next)) {
                    return next;
                }
            }
        }

        if (tokens.length > 0) {
            String first = tokens[0];
            if (first.length() == 1 && "ABCD".contains(first)) {
                return first;
            }
        }

        return "";
    }

    private String normalizeComparableText(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.US);
        if (normalized.startsWith("A ") || normalized.startsWith("B ")
                || normalized.startsWith("C ") || normalized.startsWith("D ")) {
            normalized = normalized.substring(2).trim();
        }
        return normalized;
    }

    private void updateQuestionPanel() {
        if (binding == null) {
            return;
        }

        Question question = getCurrentQuestion();
        if (question == null) {
            binding.tvArQuestionProgress.setText(getString(R.string.ar_question_progress_default));
            binding.tvArQuestionText.setText(getString(R.string.ar_question_placeholder));
            binding.rbArOptionA.setText(getString(R.string.ar_option_format, "A", getString(R.string.ar_option_fallback)));
            binding.rbArOptionB.setText(getString(R.string.ar_option_format, "B", getString(R.string.ar_option_fallback)));
            binding.rbArOptionC.setText(getString(R.string.ar_option_format, "C", getString(R.string.ar_option_fallback)));
            binding.rbArOptionD.setText(getString(R.string.ar_option_format, "D", getString(R.string.ar_option_fallback)));
            binding.tvArLandscapeHint.setVisibility(View.GONE);
            binding.rgArOptions.clearCheck();
            binding.btnSubmitArAnswer.setEnabled(false);
            binding.btnNextArQuestion.setEnabled(false);
            binding.btnNextArQuestion.setText(R.string.ar_next_question);
            setOptionInputsEnabled(false);
            binding.tvArAnswerFeedback.setText(getString(R.string.ar_no_questions_loaded));
            updateScoreLabel();
            updateFaceQuizCardContent();
            return;
        }

        binding.tvArQuestionProgress.setText(getString(
                R.string.ar_question_progress,
                selectedQuestionIndex + 1,
                selectedQuestions.size()
        ));
        binding.tvArQuestionText.setText(nonEmptyOrFallback(
                question.getQuestionText(),
                getString(R.string.ar_question_text_fallback)
        ));

        binding.rbArOptionA.setText(getString(R.string.ar_option_format, "A",
                nonEmptyOrFallback(question.getOptionA(), getString(R.string.ar_option_fallback))));
        binding.rbArOptionB.setText(getString(R.string.ar_option_format, "B",
                nonEmptyOrFallback(question.getOptionB(), getString(R.string.ar_option_fallback))));
        binding.rbArOptionC.setText(getString(R.string.ar_option_format, "C",
                nonEmptyOrFallback(question.getOptionC(), getString(R.string.ar_option_fallback))));
        binding.rbArOptionD.setText(getString(R.string.ar_option_format, "D",
                nonEmptyOrFallback(question.getOptionD(), getString(R.string.ar_option_fallback))));

        binding.rgArOptions.clearCheck();
        setOptionInputsEnabled(!isQuizCompleted);
        currentQuestionAnswered = false;
        binding.btnSubmitArAnswer.setEnabled(!isQuizCompleted);
        binding.btnNextArQuestion.setEnabled(false);
        binding.btnNextArQuestion.setText(isQuizCompleted ? R.string.ar_quiz_completed : R.string.ar_next_question);
        binding.tvArAnswerFeedback.setText(isQuizCompleted
            ? getString(R.string.ar_quiz_complete_summary, correctAnswerCount, selectedQuestions.size())
            : getString(R.string.ar_answer_select_prompt));
        updateScoreLabel();
        updateFaceQuizCardContent();
        updatePortraitOverflowHint();
        maybeAutoPlaceCurrentQuestionCards();
        applyArControlPanelState();
    }

    private String nonEmptyOrFallback(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private void updateScoreLabel() {
        if (binding == null) {
            return;
        }
        binding.tvArScore.setText(getString(
                R.string.ar_score_summary,
                correctAnswerCount,
                answeredQuestionCount
        ));
    }

    private void updatePortraitOverflowHint() {
        if (binding == null) {
            return;
        }

        boolean eligibleForLandscapeHint = !faceTrackingModeEnabled
                && !isQuizCompleted
                && getCurrentQuestion() != null
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (!eligibleForLandscapeHint) {
            binding.tvArLandscapeHint.setVisibility(View.GONE);
            return;
        }

        binding.cardArQuestionPanel.post(() -> {
            if (binding == null) {
                return;
            }

            boolean shouldShowHint = !faceTrackingModeEnabled
                    && !isQuizCompleted
                    && getCurrentQuestion() != null
                    && getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                    && shouldRecommendLandscapeForCurrentQuestion();
            binding.tvArLandscapeHint.setVisibility(shouldShowHint ? View.VISIBLE : View.GONE);
        });
    }

    private boolean shouldRecommendLandscapeForCurrentQuestion() {
        if (binding == null) {
            return false;
        }

        int questionLineCount = binding.tvArQuestionText.getLineCount();
        int optionLineCount = Math.max(
                Math.max(binding.rbArOptionA.getLineCount(), binding.rbArOptionB.getLineCount()),
                Math.max(binding.rbArOptionC.getLineCount(), binding.rbArOptionD.getLineCount())
        );

        int panelHeight = binding.cardArQuestionPanel.getHeight();
        int rootHeight = binding.getRoot().getHeight();
        boolean panelTakesTooMuchSpace = rootHeight > 0 && panelHeight > (rootHeight * 0.5f);

        return questionLineCount >= 5 || optionLineCount >= 3 || panelTakesTooMuchSpace;
    }

    private void setOptionInputsEnabled(boolean enabled) {
        if (binding == null) {
            return;
        }
        binding.rbArOptionA.setEnabled(enabled);
        binding.rbArOptionB.setEnabled(enabled);
        binding.rbArOptionC.setEnabled(enabled);
        binding.rbArOptionD.setEnabled(enabled);
    }

    private void setFaceOptionInputsEnabled(boolean enabled) {
        if (binding == null) {
            return;
        }
        binding.btnArFaceOptionA.setEnabled(enabled);
        binding.btnArFaceOptionB.setEnabled(enabled);
        binding.btnArFaceOptionC.setEnabled(enabled);
        binding.btnArFaceOptionD.setEnabled(enabled);
    }

    private void showTapFocusIndicator(MotionEvent motionEvent) {
        if (binding == null) {
            return;
        }

        View focusRing = binding.viewArFocusRing;
        focusRing.animate().cancel();
        focusRing.setVisibility(View.VISIBLE);

        float halfWidth = focusRing.getWidth() > 0 ? focusRing.getWidth() / 2f : dpToPx(32f);
        float halfHeight = focusRing.getHeight() > 0 ? focusRing.getHeight() / 2f : dpToPx(32f);

        focusRing.setX(motionEvent.getX() - halfWidth);
        focusRing.setY(motionEvent.getY() - halfHeight);
        focusRing.setAlpha(0.9f);
        focusRing.setScaleX(1.25f);
        focusRing.setScaleY(1.25f);

        focusRing.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .setDuration(500)
                .withEndAction(() -> focusRing.setVisibility(View.GONE))
                .start();
    }

    private float dpToPx(float dp) {
        return dp * requireContext().getResources().getDisplayMetrics().density;
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

            activeQuizId = quizId;
            String selectedSubject = selected.getSubject() != null && !selected.getSubject().trim().isEmpty()
                    ? selected.getSubject().trim()
                    : (selected.getTitle() != null && !selected.getTitle().trim().isEmpty()
                    ? selected.getTitle().trim() : "General");
            activeQuizSubject = selectedSubject;

            resetQuizSessionState();
            updateQuestionPanel();

            viewModel.syncQuestions(quizId);
            if (questionsLiveData != null) {
                questionsLiveData.removeObservers(getViewLifecycleOwner());
            }
            questionsLiveData = viewModel.getQuestionsForQuiz(quizId);
            questionsLiveData.observe(getViewLifecycleOwner(), questions -> {
                List<Question> loadedQuestions = questions != null ? questions : new ArrayList<>();

                if (loadedQuestions.isEmpty()) {
                    selectedQuestions = new ArrayList<>();
                    updateFlashcardInputFromSelectedQuestion();
                    updateQuestionPanel();
                    if (!questionSessionInitialized && !noQuestionsToastShown) {
                        noQuestionsToastShown = true;
                        Toast.makeText(requireContext(), getString(R.string.ar_no_questions_loaded), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (!questionSessionInitialized) {
                    initializeQuizSessionWithQuestions(loadedQuestions);
                    return;
                }

                // Keep in-progress AR quiz stable across Room re-emissions from sync updates.
                if (loadedQuestions.size() != selectedQuestions.size()) {
                    int previousIndex = selectedQuestionIndex;
                    selectedQuestions = new ArrayList<>(loadedQuestions);
                    ensureUserAnswersCapacity(selectedQuestions.size());
                    if (selectedQuestions.isEmpty()) {
                        selectedQuestionIndex = 0;
                    } else {
                        selectedQuestionIndex = Math.min(previousIndex, selectedQuestions.size() - 1);
                    }
                    updateFlashcardInputFromSelectedQuestion();
                    if (!isQuizCompleted) {
                        updateQuestionPanel();
                    }
                }
            });
        });
    }

    private void updateFlashcardInputFromSelectedQuestion() {
        if (binding == null) return;
        if (selectedQuestions == null || selectedQuestions.isEmpty()) {
            binding.etFlashcardText.setText("");
            return;
        }
        if (selectedQuestionIndex < 0 || selectedQuestionIndex >= selectedQuestions.size()) {
            selectedQuestionIndex = 0;
        }
        String text = selectedQuestions.get(selectedQuestionIndex).getQuestionText();
        if (text != null) binding.etFlashcardText.setText(text);
    }

    private boolean advanceSelectedQuestionIfNeeded() {
        if (selectedQuestions == null || selectedQuestions.isEmpty()) return false;
        if (selectedQuestionIndex >= selectedQuestions.size() - 1) return false;
        selectedQuestionIndex++;
        clearPlacedNodes();
        updateFlashcardInputFromSelectedQuestion();
        currentQuestionAnswered = false;
        return true;
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

    private void clearPlacedNodes() {
        if (arSceneView == null) {
            placedNodes.clear();
            return;
        }
        for (AnchorNode node : placedNodes) {
            try {
                arSceneView.getScene().removeChild(node);
                if (node.getAnchor() != null) {
                    node.getAnchor().detach();
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to clean up AR anchor", t);
            }
        }
        placedNodes.clear();
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
            } catch (Throwable t) {
                Log.e(TAG, "Failed to resume AR scene", t);
                if (handleFaceModeRuntimeFailure(t)) {
                    return;
                }
                showArUnavailable(getString(R.string.ar_session_not_ready, safeMessage(t)));
            }
        }
        maybeAutoPlaceCurrentQuestionCards();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (arSceneView != null) {
            try {
                arSceneView.pause();
            } catch (Throwable t) {
                Log.w(TAG, "Failed to pause AR scene cleanly", t);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (arSceneView != null && faceTrackingUpdateListener != null) {
            try {
                arSceneView.getScene().removeOnUpdateListener(faceTrackingUpdateListener);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to remove face tracking listener", t);
            }
        }
        faceTrackingUpdateListener = null;
        trackedFace = null;
        clearPlacedNodes();
        if (arSceneView != null) {
            try {
                arSceneView.destroy();
            } catch (Throwable t) {
                Log.w(TAG, "Failed to destroy AR scene cleanly", t);
            }
        }
        arAvailable = false;
        sceneTouchListenerAttached = false;
        arAvailabilityRetryCount = 0;
        faceTrackingModeEnabled = false;
        arSceneView = null;
        binding = null;
    }
}
