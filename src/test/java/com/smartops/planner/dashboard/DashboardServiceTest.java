package com.smartops.planner.dashboard;

import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.dashboard.dto.TaskStatusSummaryResponse;
import com.smartops.planner.dashboard.dto.WorkloadResponse;
import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.planning.Assignment;
import com.smartops.planner.planning.AssignmentRepository;
import com.smartops.planner.planning.PlanningRun;
import com.smartops.planner.planning.PlanningRunRepository;
import com.smartops.planner.planning.PlanningRunStatus;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private PlanningRunRepository planningRunRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getWorkload_shouldCalculateEmployeeWorkloadPercentages() {
        Employee ada = employee(1L, "Ada", 10, 40);
        Employee grace = employee(2L, "Grace", 30, 40);

        when(employeeRepository.findAll()).thenReturn(List.of(ada, grace));

        List<WorkloadResponse> workload = dashboardService.getWorkload();

        assertEquals(2, workload.size());
        assertEquals(1L, workload.get(0).employeeId());
        assertEquals("Ada", workload.get(0).employeeName());
        assertEquals(10, workload.get(0).currentWeeklyHours());
        assertEquals(40, workload.get(0).maxWeeklyHours());
        assertEquals(30, workload.get(0).remainingWeeklyHours());
        assertEquals(25.0, workload.get(0).workloadPercentage(), 0.001);

        assertEquals(75.0, workload.get(1).workloadPercentage(), 0.001);
        assertEquals(10, workload.get(1).remainingWeeklyHours());

        verify(employeeRepository).findAll();
        verify(taskRepository, never()).findAll();
        verify(assignmentRepository, never()).findAll();
        verify(planningRunRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getWorkload_shouldReturnEmptyList_whenThereAreNoEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        List<WorkloadResponse> workload = dashboardService.getWorkload();

        assertTrue(workload.isEmpty());
        verify(employeeRepository).findAll();
        verify(taskRepository, never()).findAll();
        verify(assignmentRepository, never()).findAll();
        verify(planningRunRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getWorkload_shouldHandleZeroMaxWeeklyHours() {
        Employee employee = employee(1L, "Ada", 0, 0);

        when(employeeRepository.findAll()).thenReturn(List.of(employee));

        List<WorkloadResponse> workload = dashboardService.getWorkload();

        assertEquals(1, workload.size());
        assertEquals(0, workload.get(0).remainingWeeklyHours());
        assertEquals(0.0, workload.get(0).workloadPercentage(), 0.001);

        verify(employeeRepository).findAll();
        verify(taskRepository, never()).findAll();
        verify(assignmentRepository, never()).findAll();
        verify(planningRunRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getTaskStatusSummary_shouldCalculatePercentagesForEveryStatus() {
        when(taskRepository.findAll()).thenReturn(List.of(
                task(1L, "Pending task", TaskStatus.PENDING, TaskPriority.HIGH),
                task(2L, "Assigned task", TaskStatus.ASSIGNED, TaskPriority.MEDIUM),
                task(3L, "Done task", TaskStatus.DONE, TaskPriority.LOW),
                task(4L, "Another pending task", TaskStatus.PENDING, TaskPriority.URGENT)
        ));

        List<TaskStatusSummaryResponse> summary = dashboardService.getTaskStatusSummary();

        assertEquals(5, summary.size());
        assertStatus(summary, TaskStatus.PENDING, 2, 50.0);
        assertStatus(summary, TaskStatus.ASSIGNED, 1, 25.0);
        assertStatus(summary, TaskStatus.IN_PROGRESS, 0, 0.0);
        assertStatus(summary, TaskStatus.DONE, 1, 25.0);
        assertStatus(summary, TaskStatus.CANCELLED, 0, 0.0);

        verify(taskRepository).findAll();
        verify(employeeRepository, never()).findAll();
        verify(assignmentRepository, never()).findAll();
        verify(planningRunRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getTaskStatusSummary_shouldReturnZeroPercentagesWhenThereAreNoTasks() {
        when(taskRepository.findAll()).thenReturn(List.of());

        List<TaskStatusSummaryResponse> summary = dashboardService.getTaskStatusSummary();

        assertEquals(5, summary.size());
        summary.forEach(statusSummary -> {
            assertEquals(0, statusSummary.count());
            assertEquals(0.0, statusSummary.percentage(), 0.001);
        });

        verify(taskRepository).findAll();
        verify(employeeRepository, never()).findAll();
        verify(assignmentRepository, never()).findAll();
        verify(planningRunRepository, never()).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getPlanningSummary_shouldCalculateBusinessMetrics() {
        Task pendingUrgent = task(1L, "Critical pending", TaskStatus.PENDING, TaskPriority.URGENT);
        Task assigned = task(2L, "Assigned", TaskStatus.ASSIGNED, TaskPriority.HIGH);
        Task pending = task(3L, "Pending", TaskStatus.PENDING, TaskPriority.LOW);
        PlanningRun latestRun = planningRun(1L);

        when(taskRepository.findAll()).thenReturn(List.of(pendingUrgent, assigned, pending));
        when(assignmentRepository.findAll()).thenReturn(List.of(
                assignment(1L, latestRun, assigned, employee(1L, "Ada", 10, 40), 80, true),
                assignment(2L, latestRun, pending, null, 0, false),
                assignment(3L, latestRun, assigned, employee(2L, "Grace", 15, 40), 100, true)
        ));
        when(planningRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(latestRun));

        PlanningSummaryResponse summary = dashboardService.getPlanningSummary();

        assertEquals(1, summary.assignedTasks());
        assertEquals(2, summary.pendingTasks());
        assertEquals(1, summary.criticalPendingTasks());
        assertEquals(3, summary.totalAssignments());
        assertEquals(90.0, summary.averageAssignmentScore(), 0.001);
        assertEquals(1L, summary.latestPlanningRunId());
        assertEquals(PlanningRunStatus.COMPLETED, summary.latestPlanningRunStatus());
        assertEquals(Instant.parse("2026-05-27T10:00:01Z"), summary.latestPlanningRunFinishedAt());

        verify(taskRepository).findAll();
        verify(assignmentRepository).findAll();
        verify(planningRunRepository).findTopByOrderByStartedAtDesc();
        verify(employeeRepository, never()).findAll();
    }

    @Test
    void getPlanningSummary_shouldHandleMissingLatestPlanningRun() {
        when(taskRepository.findAll()).thenReturn(List.of());
        when(assignmentRepository.findAll()).thenReturn(List.of());
        when(planningRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());

        PlanningSummaryResponse summary = dashboardService.getPlanningSummary();

        assertEquals(0, summary.assignedTasks());
        assertEquals(0, summary.pendingTasks());
        assertEquals(0, summary.criticalPendingTasks());
        assertEquals(0, summary.totalAssignments());
        assertEquals(0.0, summary.averageAssignmentScore(), 0.001);
        assertNull(summary.latestPlanningRunId());
        assertNull(summary.latestPlanningRunStatus());
        assertNull(summary.latestPlanningRunFinishedAt());

        verify(taskRepository).findAll();
        verify(assignmentRepository).findAll();
        verify(planningRunRepository).findTopByOrderByStartedAtDesc();
        verify(employeeRepository, never()).findAll();
    }

    private void assertStatus(
            List<TaskStatusSummaryResponse> summary,
            TaskStatus status,
            long expectedCount,
            double expectedPercentage
    ) {
        TaskStatusSummaryResponse response = summary.stream()
                .filter(item -> item.status() == status)
                .findFirst()
                .orElseThrow();

        assertEquals(expectedCount, response.count());
        assertEquals(expectedPercentage, response.percentage(), 0.001);
    }

    private Employee employee(Long id, String name, int currentWeeklyHours, int maxWeeklyHours) {
        Employee employee = new Employee(name, name.toLowerCase() + "@smartops.test", maxWeeklyHours, SeniorityLevel.SENIOR);
        employee.setCurrentWeeklyHours(currentWeeklyHours);
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private Task task(Long id, String title, TaskStatus status, TaskPriority priority) {
        Task task = new Task(title, priority, 4);
        task.setStatus(status);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private PlanningRun planningRun(Long id) {
        PlanningRun planningRun = new PlanningRun(Instant.parse("2026-05-27T10:00:00Z"));
        planningRun.complete(3, 1, 2, "Planning summary", Instant.parse("2026-05-27T10:00:01Z"));
        ReflectionTestUtils.setField(planningRun, "id", id);
        return planningRun;
    }

    private Assignment assignment(
            Long id,
            PlanningRun planningRun,
            Task task,
            Employee employee,
            int score,
            boolean assigned
    ) {
        Assignment assignment = new Assignment(
                planningRun,
                task,
                employee,
                score,
                assigned,
                "Assignment explanation",
                Instant.parse("2026-05-27T10:00:00Z")
        );
        ReflectionTestUtils.setField(assignment, "id", id);
        return assignment;
    }
}
