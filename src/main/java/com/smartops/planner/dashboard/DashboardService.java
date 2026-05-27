package com.smartops.planner.dashboard;

import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.dashboard.dto.TaskStatusSummaryResponse;
import com.smartops.planner.dashboard.dto.WorkloadResponse;
import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.planning.Assignment;
import com.smartops.planner.planning.AssignmentRepository;
import com.smartops.planner.planning.PlanningRun;
import com.smartops.planner.planning.PlanningRunRepository;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.task.TaskStatus;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;
    private final AssignmentRepository assignmentRepository;
    private final PlanningRunRepository planningRunRepository;

    public DashboardService(
            EmployeeRepository employeeRepository,
            TaskRepository taskRepository,
            AssignmentRepository assignmentRepository,
            PlanningRunRepository planningRunRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.planningRunRepository = planningRunRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkloadResponse> getWorkload() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toWorkloadResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskStatusSummaryResponse> getTaskStatusSummary() {
        List<Task> tasks = taskRepository.findAll();
        long totalTasks = tasks.size();
        Map<TaskStatus, Long> countByStatus = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));

        return Arrays.stream(TaskStatus.values())
                .map(status -> new TaskStatusSummaryResponse(
                        status,
                        countByStatus.getOrDefault(status, 0L),
                        percentage(countByStatus.getOrDefault(status, 0L), totalTasks)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanningSummaryResponse getPlanningSummary() {
        List<Task> tasks = taskRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        Optional<PlanningRun> latestRun = planningRunRepository.findTopByOrderByStartedAtDesc();

        long assignedTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.ASSIGNED)
                .count();
        long pendingTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .count();
        long criticalPendingTasks = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .filter(task -> task.getPriority() == TaskPriority.URGENT)
                .count();
        double averageScore = assignments.stream()
                .filter(assignment -> Boolean.TRUE.equals(assignment.getAssigned()))
                .mapToInt(Assignment::getScore)
                .average()
                .orElse(0.0);

        return new PlanningSummaryResponse(
                assignedTasks,
                pendingTasks,
                criticalPendingTasks,
                assignments.size(),
                roundTwoDecimals(averageScore),
                latestRun.map(PlanningRun::getId).orElse(null),
                latestRun.map(PlanningRun::getStatus).orElse(null),
                latestRun.map(PlanningRun::getFinishedAt).orElse(null)
        );
    }

    private WorkloadResponse toWorkloadResponse(Employee employee) {
        int remainingHours = employee.getMaxWeeklyHours() - employee.getCurrentWeeklyHours();
        return new WorkloadResponse(
                employee.getId(),
                employee.getName(),
                employee.getCurrentWeeklyHours(),
                employee.getMaxWeeklyHours(),
                remainingHours,
                percentage(employee.getCurrentWeeklyHours(), employee.getMaxWeeklyHours())
        );
    }

    private double percentage(long value, long total) {
        if (total <= 0) {
            return 0.0;
        }

        return roundTwoDecimals((value * 100.0) / total);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
