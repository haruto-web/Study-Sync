package com.example.studysync_project.data.db;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.migration.Migration;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.studysync_project.data.db.dao.QuestionDao;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.StudyModuleDao;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.db.dao.TimerSessionDao;
import com.example.studysync_project.data.db.dao.UserProfileDao;
import com.example.studysync_project.data.model.Question;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.StudyModule;
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
        StudyModule.class,
        Quiz.class,
        Question.class,
        Task.class,
        TimerSession.class,
        QuizAttempt.class
    },
    version = 6,
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

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `study_modules` (`moduleId` TEXT NOT NULL, `userId` TEXT, `title` TEXT, `subject` TEXT, `topic` TEXT, `description` TEXT, `contentText` TEXT, `sourceType` TEXT, `sourceRef` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`moduleId`))");
            database.execSQL("ALTER TABLE quizzes ADD COLUMN moduleId TEXT");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            if (!hasColumn(database, "users", "strand")) {
                database.execSQL("ALTER TABLE users ADD COLUMN strand TEXT");
            }
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            if (!hasColumn(database, "users", "progressionIndex")) {
                database.execSQL("ALTER TABLE users ADD COLUMN progressionIndex REAL NOT NULL DEFAULT 0.0");
            }
            if (!hasColumn(database, "users", "progressionDelta")) {
                database.execSQL("ALTER TABLE users ADD COLUMN progressionDelta REAL NOT NULL DEFAULT 0.0");
            }
            if (!hasColumn(database, "users", "progressionState")) {
                database.execSQL("ALTER TABLE users ADD COLUMN progressionState TEXT");
                database.execSQL("UPDATE users SET progressionState = 'STARTING' WHERE progressionState IS NULL");
            }
            if (!hasColumn(database, "users", "currentStreakDays")) {
                database.execSQL("ALTER TABLE users ADD COLUMN currentStreakDays INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(database, "users", "longestStreakDays")) {
                database.execSQL("ALTER TABLE users ADD COLUMN longestStreakDays INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(database, "users", "studyMinutesLast7Days")) {
                database.execSQL("ALTER TABLE users ADD COLUMN studyMinutesLast7Days INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(database, "users", "averageQuizScoreLast7Days")) {
                database.execSQL("ALTER TABLE users ADD COLUMN averageQuizScoreLast7Days REAL NOT NULL DEFAULT 0.0");
            }
            if (!hasColumn(database, "users", "strongestSubject")) {
                database.execSQL("ALTER TABLE users ADD COLUMN strongestSubject TEXT");
            }
            if (!hasColumn(database, "users", "focusSubject")) {
                database.execSQL("ALTER TABLE users ADD COLUMN focusSubject TEXT");
            }
            if (!hasColumn(database, "users", "unlockedBadgesCsv")) {
                database.execSQL("ALTER TABLE users ADD COLUMN unlockedBadgesCsv TEXT");
            }
            if (!hasColumn(database, "users", "lastUnlockedBadge")) {
                database.execSQL("ALTER TABLE users ADD COLUMN lastUnlockedBadge TEXT");
            }
            if (!hasColumn(database, "users", "lastBadgeUnlockedAt")) {
                database.execSQL("ALTER TABLE users ADD COLUMN lastBadgeUnlockedAt INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(database, "users", "lastProgressComputedAt")) {
                database.execSQL("ALTER TABLE users ADD COLUMN lastProgressComputedAt INTEGER NOT NULL DEFAULT 0");
            }
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            if (!hasColumn(database, "users", "username")) {
                database.execSQL("ALTER TABLE users ADD COLUMN username TEXT");
            }
            if (!hasColumn(database, "users", "age")) {
                database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0");
            }
        }
    };

    private static boolean hasColumn(SupportSQLiteDatabase database, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = database.query("PRAGMA table_info(`" + tableName + "`)");
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && columnName.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    // Abstract methods to get DAOs
    public abstract UserProfileDao userProfileDao();
    public abstract StudyModuleDao studyModuleDao();
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
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .build();
                }
            }
        }
        return instance;
    }
}
