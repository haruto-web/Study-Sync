package com.example.studysync_project.utils;

import android.content.Context;

import com.example.studysync_project.data.repository.QuestionRepository;
import com.example.studysync_project.data.repository.QuizAttemptRepository;
import com.example.studysync_project.data.repository.QuizRepository;
import com.example.studysync_project.data.repository.StudyModuleRepository;
import com.example.studysync_project.data.repository.TaskRepository;
import com.example.studysync_project.data.repository.TimerRepository;
import com.example.studysync_project.data.repository.UserRepository;
import com.example.studysync_project.data.progression.ProgressionRepository;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Utility class for syncing data between Firestore and Room database
 * Manages the data synchronization for all features
 */
public class FirestoreSyncUtil {
    private final Context context;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final StudyModuleRepository studyModuleRepository;
    private final TaskRepository taskRepository;
    private final TimerRepository timerRepository;
    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ProgressionRepository progressionRepository;

    public FirestoreSyncUtil(Context context) {
        this.context = context;
        this.quizRepository = new QuizRepository(context);
        this.questionRepository = new QuestionRepository(context);
        this.studyModuleRepository = new StudyModuleRepository(context);
        this.taskRepository = new TaskRepository(context);
        this.timerRepository = new TimerRepository(context);
        this.userRepository = new UserRepository(context);
        this.quizAttemptRepository = new QuizAttemptRepository(context);
        this.progressionRepository = new ProgressionRepository(context);
    }

    /**
     * Sync all data from Firestore to local Room database
     * Called on app startup or when explicitly requested
     */
    public void syncAllData(String userId) {
        syncUserProfile(userId);
        syncStudyModules(userId);
        syncQuizzes(userId);
        syncTasks(userId);
        syncTimerSessions(userId);
        syncQuizAttempts(userId);
        progressionRepository.recomputeProgressionAsync(userId);
    }

    /**
     * Sync study modules from Firestore to Room
     */
    private void syncStudyModules(String userId) {
        studyModuleRepository.syncStudyModulesFromFirestore(userId);
    }

    /**
     * Sync user profile from Firestore to Room
     */
    private void syncUserProfile(String userId) {
        userRepository.syncUserProfileFromFirestore(userId);
    }

    /**
     * Sync quizzes from Firestore to Room
     */
    private void syncQuizzes(String userId) {
        quizRepository.syncQuizzesFromFirestore(userId);
    }

    /**
     * Sync tasks from Firestore to Room
     */
    private void syncTasks(String userId) {
        taskRepository.syncTasksFromFirestore(userId);
    }

    /**
     * Sync timer sessions from Firestore to Room
     */
    private void syncTimerSessions(String userId) {
        timerRepository.syncTimerSessionsFromFirestore(userId);
    }

    /**
     * Sync quiz attempts from Firestore to Room
     */
    private void syncQuizAttempts(String userId) {
        quizAttemptRepository.syncQuizAttemptsFromFirestore(userId);
    }

    /**
     * Sync questions for a specific quiz
     */
    public void syncQuestionsForQuiz(String quizId) {
        questionRepository.syncQuestionsFromFirestore(quizId);
    }

    /**
     * Perform periodic sync (can be called by WorkManager or timer)
     */
    public void performPeriodicSync() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (userId != null) {
            syncAllData(userId);
        }
    }

    /**
     * Clear all local cached data
     */
    public void clearAllLocalData() {
        studyModuleRepository.clearLocalData();
        quizRepository.clearLocalData();
        questionRepository.clearLocalData();
        taskRepository.clearLocalData();
        timerRepository.clearLocalData();
        userRepository.clearLocalData();
        quizAttemptRepository.clearLocalData();
    }
}
