package com.smartops.planner.planning;

import com.smartops.planner.common.exception.GlobalExceptionHandler;
import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlanningController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class PlanningControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanningService planningService;

    @Test
    void runPlanning_shouldReturnPlanningRun() throws Exception {
        when(planningService.runPlanning()).thenReturn(planningRunResponse());

        mockMvc.perform(post("/api/planning/run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.assignedTasks").value(1))
                .andExpect(jsonPath("$.assignments[0].assigned").value(true));
    }

    @Test
    void findAllRuns_shouldReturnRuns() throws Exception {
        when(planningService.findAllRuns()).thenReturn(List.of(planningRunResponse()));

        mockMvc.perform(get("/api/planning/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void findRunById_shouldReturnNotFound_whenRunDoesNotExist() throws Exception {
        when(planningService.findRunById(99L))
                .thenThrow(new ResourceNotFoundException("Planning run not found with id 99"));

        mockMvc.perform(get("/api/planning/runs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Planning run not found with id 99"));
    }

    @Test
    void findAllAssignments_shouldReturnAssignments() throws Exception {
        when(planningService.findAllAssignments()).thenReturn(List.of(assignmentResponse()));

        mockMvc.perform(get("/api/planning/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].assigned").value(true));
    }

    @Test
    void findAssignmentById_shouldReturnAssignment() throws Exception {
        when(planningService.findAssignmentById(1L)).thenReturn(assignmentResponse());

        mockMvc.perform(get("/api/planning/assignments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.employeeName").value("Ada"));
    }

    private PlanningRunResponse planningRunResponse() {
        return new PlanningRunResponse(
                1L,
                PlanningRunStatus.COMPLETED,
                Instant.parse("2026-05-27T10:00:00Z"),
                Instant.parse("2026-05-27T10:00:01Z"),
                1,
                1,
                0,
                "Planning run completed: 1 assigned, 0 unassigned out of 1 pending tasks.",
                List.of(assignmentResponse())
        );
    }

    private AssignmentResponse assignmentResponse() {
        return new AssignmentResponse(
                1L,
                1L,
                1L,
                "Build planning API",
                1L,
                "Ada",
                90,
                true,
                "Ada is the best match.",
                Instant.parse("2026-05-27T10:00:00Z")
        );
    }
}
