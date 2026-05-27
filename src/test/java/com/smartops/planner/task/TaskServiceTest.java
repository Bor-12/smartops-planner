package com.smartops.planner.task;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.task.dto.CreateTaskRequest;
import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskRequest;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void create_shouldCreateTask_whenDataIsValid() {
        Skill java = skill(10L, "Java");
        CreateTaskRequest request = validCreateRequest(Set.of(10L));

        when(skillRepository.findAllById(Set.of(10L))).thenReturn(List.of(java));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedTask, "id", 1L);
            return savedTask;
        });

        TaskResponse response = taskService.create(request);

        assertEquals(1L, response.id());
        assertEquals("Build planning API", response.title());
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(6, response.estimatedHours());
        assertEquals(LocalDate.of(2026, 6, 15), response.deadline());
        assertEquals(TaskStatus.PENDING, response.status());
        assertEquals(1, response.requiredSkills().size());
        assertEquals("Java", response.requiredSkills().get(0).name());

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();
        assertEquals("Build planning API", savedTask.getTitle());
        assertEquals(TaskStatus.PENDING, savedTask.getStatus());
        assertEquals(1, savedTask.getRequiredSkills().size());
    }

    @Test
    void create_shouldThrowBadRequest_whenSkillDoesNotExist() {
        CreateTaskRequest request = validCreateRequest(Set.of(10L, 999999L));

        when(skillRepository.findAllById(Set.of(10L, 999999L)))
                .thenReturn(List.of(skill(10L, "Java")));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> taskService.create(request)
        );

        assertEquals("Skill not found with id 999999", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateStatus_shouldChangeStatus() {
        Task task = task(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.updateStatus(
                1L,
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)
        );

        assertEquals(TaskStatus.IN_PROGRESS, response.status());
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
    }

    @Test
    void updateStatus_shouldThrowResourceNotFoundException_whenTaskDoesNotExist() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.updateStatus(99L, new UpdateTaskStatusRequest(TaskStatus.DONE))
        );

        assertEquals("Task not found with id 99", exception.getMessage());
    }

    @Test
    void update_shouldUpdateTask_whenDataIsValid() {
        Task task = task(1L);
        Skill spring = skill(20L, "Spring Boot");
        UpdateTaskRequest request = new UpdateTaskRequest(
                " Update planning API ",
                "Updated description",
                TaskPriority.URGENT,
                8,
                LocalDate.of(2026, 6, 20),
                Set.of(20L)
        );

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(skillRepository.findAllById(Set.of(20L))).thenReturn(List.of(spring));

        TaskResponse response = taskService.update(1L, request);

        assertEquals("Update planning API", response.title());
        assertEquals("Updated description", response.description());
        assertEquals(TaskPriority.URGENT, response.priority());
        assertEquals(8, response.estimatedHours());
        assertEquals(LocalDate.of(2026, 6, 20), response.deadline());
        assertEquals(1, response.requiredSkills().size());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void deleteById_shouldDeleteTask_whenTaskExists() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        taskService.deleteById(1L);

        verify(taskRepository).existsById(1L);
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteById_shouldThrowResourceNotFoundException_whenTaskDoesNotExist() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> taskService.deleteById(99L)
        );

        assertEquals("Task not found with id 99", exception.getMessage());
        verify(taskRepository, never()).deleteById(any());
    }

    private CreateTaskRequest validCreateRequest(Set<Long> requiredSkillIds) {
        return new CreateTaskRequest(
                " Build planning API ",
                "Create task endpoints",
                TaskPriority.HIGH,
                6,
                LocalDate.of(2026, 6, 15),
                requiredSkillIds
        );
    }

    private Task task(Long id) {
        Task task = new Task("Build planning API", TaskPriority.HIGH, 6);
        task.setDeadline(LocalDate.of(2026, 6, 15));
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private Skill skill(Long id, String name) {
        Skill skill = new Skill(name);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
