package com.example.studysync_project.utils;

import android.media.Image;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceDetectionUtil {
    
    public interface FaceDetectionCallback {
        void onFaceDetected(Face face, boolean isFacingCamera);
        void onNoFaceDetected();
        void onHeadTiltDetected(HeadTiltDirection direction);
        void onError(Exception e);
    }
    
    public enum HeadTiltDirection {
        LEFT, RIGHT, UP, DOWN, CENTER
    }
    
    private final FaceDetector detector;
    private FaceDetectionCallback callback;
    private boolean isProcessing = false;
    
    // Thresholds for head tilt detection
    private static final float TILT_THRESHOLD = 15.0f; // degrees
    private static final float YAW_THRESHOLD = 20.0f; // degrees for left/right
    private static final float PITCH_THRESHOLD = 15.0f; // degrees for up/down
    
    public FaceDetectionUtil() {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .enableTracking()
                .build();
        
        detector = FaceDetection.getClient(options);
    }
    
    public void setCallback(FaceDetectionCallback callback) {
        this.callback = callback;
    }
    
    public void processImage(ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }
        
        isProcessing = true;
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        processFaces(faces);
                        isProcessing = false;
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError(e);
                        }
                        isProcessing = false;
                        imageProxy.close();
                    });
        } else {
            isProcessing = false;
            imageProxy.close();
        }
    }
    
    private void processFaces(List<Face> faces) {
        if (callback == null) return;
        
        if (faces.isEmpty()) {
            callback.onNoFaceDetected();
            return;
        }
        
        // Process the first (largest) face
        Face face = faces.get(0);
        
        // Check if face is facing the camera (based on head pose)
        boolean isFacingCamera = isFacingCamera(face);
        callback.onFaceDetected(face, isFacingCamera);
        
        // Detect head tilt direction
        HeadTiltDirection tiltDirection = getHeadTiltDirection(face);
        if (tiltDirection != HeadTiltDirection.CENTER) {
            callback.onHeadTiltDetected(tiltDirection);
        }
    }
    
    private boolean isFacingCamera(Face face) {
        // Check if the face is roughly facing the camera
        float headEulerAngleY = face.getHeadEulerAngleY(); // Yaw
        float headEulerAngleZ = face.getHeadEulerAngleZ(); // Roll
        
        // Face is considered facing camera if yaw and roll are within reasonable bounds
        return Math.abs(headEulerAngleY) < 30.0f && Math.abs(headEulerAngleZ) < 30.0f;
    }
    
    private HeadTiltDirection getHeadTiltDirection(Face face) {
        float headEulerAngleY = face.getHeadEulerAngleY(); // Yaw (left/right)
        float headEulerAngleX = face.getHeadEulerAngleX(); // Pitch (up/down)
        float headEulerAngleZ = face.getHeadEulerAngleZ(); // Roll (tilt)
        
        // Check for significant head movements
        if (Math.abs(headEulerAngleX) > PITCH_THRESHOLD) {
            return headEulerAngleX > 0 ? HeadTiltDirection.DOWN : HeadTiltDirection.UP;
        }
        
        if (Math.abs(headEulerAngleY) > YAW_THRESHOLD) {
            return headEulerAngleY > 0 ? HeadTiltDirection.LEFT : HeadTiltDirection.RIGHT;
        }
        
        if (Math.abs(headEulerAngleZ) > TILT_THRESHOLD) {
            return headEulerAngleZ > 0 ? HeadTiltDirection.RIGHT : HeadTiltDirection.LEFT;
        }
        
        return HeadTiltDirection.CENTER;
    }
    
    public void release() {
        if (detector != null) {
            detector.close();
        }
    }
}