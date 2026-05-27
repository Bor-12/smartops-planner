package com.smartops.planner.task;

import com.smartops.planner.common.exception.BadRequestException;
import com.smartops.planner.common.exception.GlobalExceptionHandler;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.task.dto.CreateTaskRequest;
import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void findAll_shouldReturnTasks() throws Exception {
        when(taskService.findAll()).thenReturn(List.of(
                taskResponse(1L, "Build planning API", TaskStatus.PENDING),
                taskResponse(2L, "Write task tests", TaskStatus.IN_PROGRESS)
        ));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Build planning API"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Write task tests"))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));

        verify(taskService).findAll();
    }

    @Test
    void findById_shouldReturnTask_whenTaskExists() throws Exception {
        when(taskService.findById(1L))
                .thenReturn(taskResponse(1L, "Build planning API", TaskStatus.PENDING));

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Build planning API"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).findById(1L);
    }

    @Test
    void create_shouldCreateTask() throws Exception {
        when(taskService.create(any(CreateTaskRequest.class)))
                .thenReturn(taskResponse(1L, "Build planning API", TaskStatus.PENDING));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build planning API",
                                  "description": "Create task endpoints",
                                  "priority": "HIGH",
                                  "estimatedHours": 6,
                                  "deadline": "2026-06-15",
                                  "requiredSkillIds": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Build planning API"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void create_shouldReturnBadRequest_whenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Create task endpoints",
                                  "priority": "HIGH",
                                  "estimatedHours": 6,
                                  "deadline": "2026-06-15",
                                  "requiredSkillIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void create_shouldReturnBadRequest_whenEstimatedHoursIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build planning API",
                                  "description": "Create task endpoints",
                                  "priority": "HIGH",
                                  "estimatedHours": 0,
                                  "deadline": "2026-06-15",
                                  "requiredSkillIds": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void updateStatus_shouldChangeStatus() throws Exception {
        when(taskService.updateStatus(any(Long.class), any(UpdateTaskStatusRequest.class)))
                .thenReturn(taskResponse(1L, "Build planning API", TaskStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void update_shouldUpdateTask() throws Exception {
        when(taskService.update(any(Long.class), any()))
                .thenReturn(taskResponse(1L, "Updated planning API", TaskStatus.PENDING));

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated planning API",
                                  "description": "Updated description",
                                  "priority": "URGENT",
                                  "estimatedHours": 8,
                                  "deadline": "2026-06-20",
                                  "requiredSkillIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated planning API"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void updateStatus_shouldReturnBadRequest_whenStatusIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "NOT_A_STATUS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request body"));
    }

    @Test
    void create_shouldReturnBadRequest_whenRequiredSkillDoesNotExist() throws Exception {
        when(taskService.create(any(CreateTaskRequest.class)))
                .thenThrow(new BadRequestException("Skill not found with id 999999"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build planning API",
                                  "description": "Create task endpoints",
                                  "priority": "HIGH",
                                  "estimatedHours": 6,
                                  "deadline": "2026-06-15",
                                  "requiredSkillIds": [999999]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Skill not found with id 999999"));
    }

    @Test
    void findById_shouldReturnNotFound_whenTaskDoesNotExist() throws Exception {
        when(taskService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Task not found with id 99"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id 99"));
    }

    @Test
    void deleteById_shouldReturnNoContent_whenTaskExists() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).deleteById(1L);
    }

    @Test
    void deleteById_shouldReturnNotFound_whenTaskDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Task not found with id 99"))
                .when(taskService)
                .deleteById(99L);

        mockMvc.perform(delete("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id 99"));
    }

    private TaskResponse taskResponse(Long id, String title, TaskStatus status) {
        return new TaskResponse(
                id,
                title,
                "Create task endpoints",
                TaskPriority.HIGH,
                6,
                LocalDate.of(2026, 6, 15),
                status,
                List.of(),
                null,
                null,
                null
        );
    }
}
