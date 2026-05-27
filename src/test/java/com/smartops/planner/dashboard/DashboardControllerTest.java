package com.smartops.planner.dashboard;

import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.dashboard.dto.TaskStatusSummaryResponse;
import com.smartops.planner.dashboard.dto.WorkloadResponse;
import com.smartops.planner.planning.PlanningRunStatus;
import com.smartops.planner.task.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void getWorkload_shouldReturnEmployeeWorkload() throws Exception {
        when(dashboardService.getWorkload()).thenReturn(List.of(
                new WorkloadResponse(1L, "Ada", 10, 40, 30, 25.0)
        ));

        mockMvc.perform(get("/api/dashboard/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(1))
                .andExpect(jsonPath("$[0].employeeName").value("Ada"))
                .andExpect(jsonPath("$[0].workloadPercentage").value(25.0));
    }

    @Test
    void getTaskStatusSummary_shouldReturnStatusSummary() throws Exception {
        when(dashboardService.getTaskStatusSummary()).thenReturn(List.of(
                new TaskStatusSummaryResponse(TaskStatus.PENDING, 2, 50.0)
        ));

        mockMvc.perform(get("/api/dashboard/task-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].percentage").value(50.0));
    }

    @Test
    void getPlanningSummary_shouldReturnPlanningMetrics() throws Exception {
        when(dashboardService.getPlanningSummary()).thenReturn(new PlanningSummaryResponse(
                1,
                2,
                1,
                3,
                90.0,
                1L,
                PlanningRunStatus.COMPLETED,
                Instant.parse("2026-05-27T10:00:01Z")
        ));

        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTasks").value(1))
                .andExpect(jsonPath("$.pendingTasks").value(2))
                .andExpect(jsonPath("$.criticalPendingTasks").value(1))
                .andExpect(jsonPath("$.averageAssignmentScore").value(90.0))
                .andExpect(jsonPath("$.latestPlanningRunStatus").value("COMPLETED"));
    }
}
