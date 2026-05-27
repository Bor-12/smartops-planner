package com.smartops.planner.security;

import com.smartops.planner.dashboard.DashboardController;
import com.smartops.planner.dashboard.DashboardService;
import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.employee.EmployeeController;
import com.smartops.planner.employee.EmployeeService;
import com.smartops.planner.planning.PlanningController;
import com.smartops.planner.planning.PlanningRunStatus;
import com.smartops.planner.planning.PlanningService;
import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DashboardController.class, PlanningController.class, EmployeeController.class})
@Import(SecurityConfig.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private PlanningService planningService;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void dashboard_shouldReturnUnauthorized_whenNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void dashboard_shouldReturnForbidden_whenEmployeeRole() throws Exception {
        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void dashboard_shouldAllowManagerRole() throws Exception {
        when(dashboardService.getPlanningSummary()).thenReturn(planningSummaryResponse());

        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void planningRun_shouldAllowManagerRole() throws Exception {
        when(planningService.runPlanning()).thenReturn(planningRunResponse());

        mockMvc.perform(post("/api/planning/run"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deleteEmployee_shouldReturnForbidden_whenEmployeeRole() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(employeeService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteEmployee_shouldAllowAdminRole() throws Exception {
        doNothing().when(employeeService).deleteById(1L);

        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteById(1L);
    }

    private PlanningSummaryResponse planningSummaryResponse() {
        return new PlanningSummaryResponse(
                1,
                2,
                1,
                3,
                85.5,
                10L,
                PlanningRunStatus.COMPLETED,
                Instant.parse("2026-05-27T10:00:00Z")
        );
    }

    private PlanningRunResponse planningRunResponse() {
        return new PlanningRunResponse(
                1L,
                PlanningRunStatus.COMPLETED,
                Instant.parse("2026-05-27T10:00:00Z"),
                Instant.parse("2026-05-27T10:00:01Z"),
                0,
                0,
                0,
                "Planning run completed.",
                List.<AssignmentResponse>of()
        );
    }
}
