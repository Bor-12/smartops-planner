package com.smartops.planner.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.planning.AssignmentRepository;
import com.smartops.planner.planning.PlanningRunRepository;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PlanningRunRepository planningRunRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @BeforeEach
    void cleanDatabase() {
        assignmentRepository.deleteAll();
        planningRunRepository.deleteAll();
        taskRepository.deleteAll();
        employeeRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void dashboard_shouldReturnUnauthorized_withoutToken() throws Exception {
        mockMvc.perform(get("/api/dashboard/planning-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_shouldCreateUserAndReturnToken() throws Exception {
        String token = register("manager.integration", "password123", "MANAGER");

        assertThat(token).isNotBlank();
        assertThat(userRepository.existsByUsernameIgnoreCase("manager.integration")).isTrue();
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        register("manager.integration", "password123", "MANAGER");

        String token = login("manager.integration", "password123");

        assertThat(token).isNotBlank();
    }

    @Test
    void dashboard_shouldAllowManagerToken() throws Exception {
        register("manager.integration", "password123", "MANAGER");
        String managerToken = login("manager.integration", "password123");

        mockMvc.perform(get("/api/dashboard/planning-summary")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void dashboard_shouldReturnForbidden_withEmployeeToken() throws Exception {
        String employeeToken = register("employee.integration", "password123", "EMPLOYEE");

        mockMvc.perform(get("/api/dashboard/planning-summary")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void planningRun_shouldAllowManagerToken() throws Exception {
        register("manager.integration", "password123", "MANAGER");
        String managerToken = login("manager.integration", "password123");

        mockMvc.perform(post("/api/planning/run")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());
    }

    @Test
    void planningRun_shouldReturnForbidden_withEmployeeToken() throws Exception {
        String employeeToken = register("employee.integration", "password123", "EMPLOYEE");

        mockMvc.perform(post("/api/planning/run")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    private String register(String username, String password, String role) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "role": "%s"
                                }
                                """.formatted(username, password, role)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return tokenFrom(response);
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return tokenFrom(response);
    }

    private String tokenFrom(String response) throws Exception {
        JsonNode json = objectMapper.readTree(response);
        return json.get("token").asText();
    }
}
