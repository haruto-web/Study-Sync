package com.example.studysync_project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.studysync_project.data.model.Quiz;
import com.example.studysync_project.data.model.QuizAttempt;
import com.example.studysync_project.data.model.Task;
import com.example.studysync_project.data.model.TimerSession;
import com.example.studysync_project.utils.IdUtil;

import org.junit.Test;

public class ModelUnitTest {

    @Test
    public void task_defaultIsNotCompleted() {
        Task task = new Task("user1", "Study Math", "Chapter 5", System.currentTimeMillis(), "HIGH", "Math");
        assertFalse(task.isCompleted());
    }

    @Test
    public void task_setCompleted_setsCompletedAt() {
        Task task = new Task("user1", "Study Math", "", System.currentTimeMillis(), "LOW", "Math");
        task.setCompleted(true);
        assertTrue(task.isCompleted());
        assertTrue(task.getCompletedAt() > 0);
    }

    @Test
    public void quiz_defaultNotArchived() {
        Quiz quiz = new Quiz("user1", "Math Quiz", "desc", 10, 60.0, "Math", 2);
        assertFalse(quiz.isArchived());
    }

    @Test
    public void quizAttempt_passedWhenScoreAbove60() {
        QuizAttempt attempt = new QuizAttempt("user1", "quiz1", 10, 7, 70.0, 5);
        attempt.setPassed(attempt.getScorePercentage() >= 60);
        assertTrue(attempt.isPassed());
    }

    @Test
    public void quizAttempt_failedWhenScoreBelow60() {
        QuizAttempt attempt = new QuizAttempt("user1", "quiz1", 10, 5, 50.0, 5);
        attempt.setPassed(attempt.getScorePercentage() >= 60);
        assertFalse(attempt.isPassed());
    }

    @Test
    public void idUtil_generateId_notEmpty() {
        String id = IdUtil.generateId("task");
        assertNotNull(id);
        assertTrue(id.startsWith("task_"));
    }

    @Test
    public void idUtil_generateId_unique() {
        String id1 = IdUtil.generateId("quiz");
        String id2 = IdUtil.generateId("quiz");
        assertNotEquals(id1, id2);
    }

    @Test
    public void timerSession_defaultNotCompleted() {
        TimerSession session = new TimerSession("user1", 25, "Math", "");
        assertFalse(session.isCompleted());
        assertFalse(session.isPaused());
    }

    @Test
    public void timerSession_addPausedDuration_accumulates() {
        TimerSession session = new TimerSession("user1", 25, "Math", "");
        session.addPausedDuration(5000);
        session.addPausedDuration(3000);
        assertEquals(8000, session.getPausedDuration());
    }
}
