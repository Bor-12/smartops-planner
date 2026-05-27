package com.smartops.planner.task;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.skill.dto.SkillResponse;
import com.smartops.planner.task.dto.CreateTaskRequest;
import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskRequest;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final SkillRepository skillRepository;

    public TaskService(TaskRepository taskRepository, SkillRepository skillRepository) {
        this.taskRepository = taskRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return taskRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Task task = new Task(
                request.title().trim(),
                request.priority(),
                request.estimatedHours()
        );
        task.setDescription(request.description());
        task.setDeadline(request.deadline());
        task.setRequiredSkills(loadSkills(request.requiredSkillIds()));

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setEstimatedHours(request.estimatedHours());
        task.setDeadline(request.deadline());
        task.setRequiredSkills(loadSkills(request.requiredSkillIds()));

        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));

        task.setStatus(request.status());
        return toResponse(task);
    }

    @Transactional
    public void deleteById(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task not found with id " + id);
        }

        taskRepository.deleteById(id);
    }

    private Set<Skill> loadSkills(Set<Long> requiredSkillIds) {
        if (requiredSkillIds == null || requiredSkillIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueSkillIds = new HashSet<>(requiredSkillIds);
        List<Skill> skills = skillRepository.findAllById(uniqueSkillIds);
        if (skills.size() != uniqueSkillIds.size()) {
            Set<Long> foundIds = skills.stream()
                    .map(Skill::getId)
                    .collect(java.util.stream.Collectors.toSet());

            Long missingId = uniqueSkillIds.stream()
                    .filter(skillId -> !foundIds.contains(skillId))
                    .findFirst()
                    .orElseThrow();

            throw new BadRequestException("Skill not found with id " + missingId);
        }

        return new HashSet<>(skills);
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
