package com.smartops.planner.task;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskModelTest {

    @Test
    void constructor_shouldCreateTaskWithPendingStatus() {
        Task task = new Task("Build planning API", TaskPriority.HIGH, 6);
        task.setDeadline(LocalDate.of(2026, 6, 15));

        assertEquals("Build planning API", task.getTitle());
        assertEquals(TaskPriority.HIGH, task.getPriority());
        assertEquals(6, task.getEstimatedHours());
        assertEquals(LocalDate.of(2026, 6, 15), task.getDeadline());
        assertEquals(TaskStatus.PENDING, task.getStatus());
    }

    @Test
    void onCreate_shouldSetCreatedAtAndUpdatedAt() {
        Task task = new Task("Build planning API", TaskPriority.HIGH, 6);

        task.onCreate();

        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
        assertEquals(task.getCreatedAt(), task.getUpdatedAt());
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() throws InterruptedException {
        Task task = new Task("Build planning API", TaskPriority.HIGH, 6);
        task.onCreate();
        Thread.sleep(1);

        task.onUpdate();

        assertTrue(task.getUpdatedAt().isAfter(task.getCreatedAt()));
    }
}
