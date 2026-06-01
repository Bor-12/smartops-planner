package com.smartops.planner.security;

import com.smartops.planner.dashboard.DashboardController;
import com.smartops.planner.dashboard.DashboardService;
import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.auth.AuthController;
import com.smartops.planner.auth.AuthService;
import com.smartops.planner.auth.dto.AuthResponse;
import com.smartops.planner.auth.dto.LoginRequest;
import com.smartops.planner.auth.dto.RegisterRequest;
import com.smartops.planner.employee.EmployeeController;
import com.smartops.planner.employee.EmployeeService;
import com.smartops.planner.planning.PlanningController;
import com.smartops.planner.planning.PlanningRunStatus;
import com.smartops.planner.planning.PlanningService;
import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import com.smartops.planner.skill.SkillController;
import com.smartops.planner.skill.SkillService;
import com.smartops.planner.task.MyTasksController;
import com.smartops.planner.task.MyTasksService;
import com.smartops.planner.task.TaskController;
import com.smartops.planner.task.TaskService;
import com.smartops.planner.user.Role;
import com.smartops.planner.user.UserController;
import com.smartops.planner.user.UserService;
import com.smartops.planner.user.dto.UserResponse;
import com.smartops.planner.web.HomeController;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AuthController.class,
        DashboardController.class,
        EmployeeController.class,
        HomeController.class,
        MyTasksController.class,
        PlanningController.class,
        SkillController.class,
        TaskController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private MyTasksService myTasksService;

    @MockitoBean
    private PlanningService planningService;

    @MockitoBean
    private SkillService skillService;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserService userService;

    @Test
    void protectedEndpoints_shouldReturnUnauthorized_withoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/my-tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicEndpoints_shouldAllowAnonymousAccess() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse(1L, "admin", Role.ADMIN, "jwt-token"));

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void register_shouldRequireAdminRole() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse(2L, "new.user", Role.EMPLOYEE, "jwt-token"));

        String payload = """
                {
                  "username": "new.user",
                  "password": "password123",
                  "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/register")
                        .with(user("manager").roles("MANAGER"))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/register")
                        .with(user("admin").roles("ADMIN"))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void createUser_shouldRequireAdminRole() throws Exception {
        when(userService.create(any(RegisterRequest.class)))
                .thenReturn(new UserResponse(2L, "new.user", Role.EMPLOYEE));

        String payload = """
                {
                  "username": "new.user",
                  "password": "password123",
                  "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/users")
                        .with(user("manager").roles("MANAGER"))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users")
                        .with(user("employee").roles("EMPLOYEE"))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated());
    }

    @Test
    void admin_shouldAccessAdministrativeAndOperationalEndpoints() throws Exception {
        when(userService.findAll()).thenReturn(List.of());
        when(employeeService.findAll()).thenReturn(List.of());
        when(skillService.findAll()).thenReturn(List.of());
        when(taskService.findAll()).thenReturn(List.of());
        when(dashboardService.getPlanningSummary()).thenReturn(planningSummaryResponse());
        when(planningService.runPlanning()).thenReturn(planningRunResponse());

        mockMvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/employees").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/skills").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/planning-summary").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/planning/run").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void admin_shouldNotAccessEmployeeOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/my-tasks").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(myTasksService);
    }

    @Test
    void manager_shouldAccessOperationalEndpoints() throws Exception {
        when(employeeService.findAll()).thenReturn(List.of());
        when(skillService.findAll()).thenReturn(List.of());
        when(taskService.findAll()).thenReturn(List.of());
        when(dashboardService.getPlanningSummary()).thenReturn(planningSummaryResponse());
        when(planningService.runPlanning()).thenReturn(planningRunResponse());

        mockMvc.perform(get("/api/employees").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/skills").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard/planning-summary").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/planning/run").with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void manager_shouldNotAccessUserAdministrationOrEmployeeOnlyEndpoint() throws Exception {
        mockMvc.perform(get("/api/users").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/my-tasks").with(user("manager").roles("MANAGER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, myTasksService);
    }

    @Test
    void employee_shouldAccessOnlyMyTasksEndpoint() throws Exception {
        when(myTasksService.findMyTasks("employee")).thenReturn(List.of());

        mockMvc.perform(get("/api/my-tasks").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void employee_shouldNotAccessAdminOrManagerEndpoints() throws Exception {
        mockMvc.perform(get("/api/users").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/employees").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/skills").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tasks").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard/planning-summary").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/planning/run").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService, employeeService, skillService, taskService, dashboardService, planningService);
    }

    @Test
    void deleteEmployee_shouldAllowAdminAndManagerButDenyEmployee() throws Exception {
        doNothing().when(employeeService).deleteById(1L);

        mockMvc.perform(delete("/api/employees/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/employees/1").with(user("manager").roles("MANAGER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/employees/1").with(user("employee").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());

        verify(employeeService, org.mockito.Mockito.times(2)).deleteById(1L);
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
