package com.smartops.planner.task;

import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.skill.dto.SkillResponse;
import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyTasksService {

    private static final String DEMO_EMAIL_DOMAIN = "@smartops.demo";

    private final EmployeeRepository employeeRepository;
    private final TaskRepository taskRepository;

    public MyTasksService(EmployeeRepository employeeRepository, TaskRepository taskRepository) {
        this.employeeRepository = employeeRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findMyTasks(String username) {
        Employee employee = findEmployeeForUsername(username);
        return taskRepository.findByAssignedEmployeeId(employee.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse updateMyTaskStatus(String username, Long taskId, UpdateTaskStatusRequest request) {
        Employee employee = findEmployeeForUsername(username);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + taskId));

        if (task.getAssignedEmployee() == null || !task.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new ResourceNotFoundException("Assigned task not found with id " + taskId);
        }

        task.setStatus(request.status());
        return toResponse(task);
    }

    private Employee findEmployeeForUsername(String username) {
        return employeeRepository.findByEmailIgnoreCase(username + DEMO_EMAIL_DOMAIN)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found for user " + username));
    }

    private TaskResponse toResponse(Task task) {
        List<SkillResponse> requiredSkills = task.getRequiredSkills()
                .stream()
                .map(skill -> new SkillResponse(skill.getId(), skill.getName()))
                .sorted(Comparator.comparing(SkillResponse::id))
                .toList();

        Long assignedEmployeeId = task.getAssignedEmployee() == null
                ? null
                : task.getAssignedEmployee().getId();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getEstimatedHours(),
                task.getDeadline(),
                task.getStatus(),
                requiredSkills,
                assignedEmployeeId,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
