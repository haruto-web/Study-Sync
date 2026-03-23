package com.example.studysync_project.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.studysync_project.data.db.dao.QuestionDao;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.db.dao.UserProfileDao;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.data.model.UserProfile;

/**
 * Room Database for StudySync app
 * Handles local data persistence for offline support and caching
 */
@Database(
    entities = {
        UserProfile.class,
        Quiz.class,
        Question.class,
        Task.class,
        TimerSession.class,
        QuizAttempt.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    // Abstract methods to get DAOs
    public abstract UserProfileDao userProfileDao();
    public abstract QuizDao quizDao();
    public abstract QuestionDao questionDao();
    public abstract TaskDao taskDao();
    public abstract TimerSessionDao timerSessionDao();
    public abstract QuizAttemptDao quizAttemptDao();

    /**
     * Singleton pattern to ensure only one database instance exists
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "studysync_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build();
                }
            }
        }
        return instance;
    }
}
