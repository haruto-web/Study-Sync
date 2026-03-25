package com.example.studysync_project.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

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
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Consent + onboarding fields
            database.execSQL("ALTER TABLE users ADD COLUMN termsAccepted INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN termsAcceptedAt INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN termsVersion INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN personalizationEnabled INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE users ADD COLUMN gradeLevel TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN goal TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN subjectsCsv TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN topicsOfInterestCsv TEXT");
            database.execSQL("ALTER TABLE users ADD COLUMN weeklyStudyTargetMinutes INTEGER NOT NULL DEFAULT 0");
        }
    };

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
                        .addMigrations(MIGRATION_1_2)
                        .build();
                }
            }
        }
        return instance;
    }
}
