package com.example.studysync_project;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.studysync_project.data.db.AppDatabase;
import com.example.studysync_project.data.db.dao.TaskDao;
import com.example.studysync_project.data.db.dao.QuizDao;
import com.example.studysync_project.data.db.dao.QuizAttemptDao;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.utils.IdUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseInstrumentedTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase db;
    private TaskDao taskDao;
    private QuizDao quizDao;
    private QuizAttemptDao quizAttemptDao;

    @Before
    public void createDb() {
        Context ctx = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        taskDao = db.taskDao();
        quizDao = db.quizDao();
        quizAttemptDao = db.quizAttemptDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void insertAndReadTask() throws InterruptedException {
        Task task = new Task("user1", "Test Task", "desc", System.currentTimeMillis(), "HIGH", "Math");
        task.setTaskId(IdUtil.generateId("task"));
        taskDao.insertTask(task);

        List<Task> tasks = getOrAwait(taskDao.getAllTasksForUser("user1"));
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("Test Task", tasks.get(0).getTitle());
    }

    @Test
    public void completeTask_updatesFlag() throws InterruptedException {
        Task task = new Task("user1", "Task A", "", System.currentTimeMillis(), "LOW", "Science");
        task.setTaskId(IdUtil.generateId("task"));
        taskDao.insertTask(task);

        task.setCompleted(true);
        taskDao.updateTask(task);

        List<Task> completed = getOrAwait(taskDao.getCompletedTasksForUser("user1"));
        assertEquals(1, completed.size());
        assertTrue(completed.get(0).isCompleted());
    }

    @Test
    public void deleteTask_removesFromDb() throws InterruptedException {
        Task task = new Task("user1", "Delete Me", "", System.currentTimeMillis(), "MEDIUM", "History");
        task.setTaskId(IdUtil.generateId("task"));
        taskDao.insertTask(task);
        taskDao.deleteTaskById(task.getTaskId());

        List<Task> tasks = getOrAwait(taskDao.getAllTasksForUser("user1"));
        assertTrue(tasks == null || tasks.isEmpty());
    }

    @Test
    public void insertAndReadQuiz() throws InterruptedException {
        Quiz quiz = new Quiz("user1", "Math Quiz", "desc", 10, 60.0, "Math", 2);
        quiz.setQuizId(IdUtil.generateId("quiz"));
        quizDao.insertQuiz(quiz);

        List<Quiz> quizzes = getOrAwait(quizDao.getAllQuizzesForUser("user1"));
        assertNotNull(quizzes);
        assertEquals(1, quizzes.size());
        assertEquals("Math Quiz", quizzes.get(0).getTitle());
    }

    @Test
    public void quizAttempt_averageScore() throws InterruptedException {
        QuizAttempt a1 = new QuizAttempt("user1", "quiz1", 10, 8, 80.0, 5);
        a1.setAttemptId(IdUtil.generateId("attempt"));
        QuizAttempt a2 = new QuizAttempt("user1", "quiz1", 10, 6, 60.0, 5);
        a2.setAttemptId(IdUtil.generateId("attempt"));
        quizAttemptDao.insertQuizAttempt(a1);
        quizAttemptDao.insertQuizAttempt(a2);

        Double avg = getOrAwait(quizAttemptDao.getAverageScoreForUser("user1"));
        assertNotNull(avg);
        assertEquals(70.0, avg, 0.01);
    }

    // Helper to get LiveData value synchronously in tests
    private <T> T getOrAwait(LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);
        liveData.observeForever(value -> {
            data[0] = value;
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        //noinspection unchecked
        return (T) data[0];
    }
}
