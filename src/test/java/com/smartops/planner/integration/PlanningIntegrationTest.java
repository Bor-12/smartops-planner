package com.smartops.planner.integration;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.planning.Assignment;
import com.smartops.planner.planning.AssignmentRepository;
import com.smartops.planner.planning.PlanningRun;
import com.smartops.planner.planning.PlanningRunRepository;
import com.smartops.planner.planning.PlanningService;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.skill.SkillRepository;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.task.TaskStatus;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class PlanningIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PlanningService planningService;

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
    }

    @Test
    void planningRun_shouldAssignTaskAndPersistChanges() {
        Skill java = skillRepository.save(new Skill("Java"));

        Employee ada = new Employee("Ada", "ada.integration@smartops.test", 40, SeniorityLevel.SENIOR);
        ada.setCurrentWeeklyHours(10);
        ada.setSkills(new HashSet<>(Set.of(java)));
        ada = employeeRepository.save(ada);

        Task task = new Task("Build integration planning", TaskPriority.HIGH, 6);
        task.setDeadline(LocalDate.now().plusDays(7));
        task.setRequiredSkills(new HashSet<>(Set.of(java)));
        task = taskRepository.save(task);

        planningService.runPlanning();

        List<PlanningRun> planningRuns = planningRunRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        Task persistedTask = taskRepository.findById(task.getId()).orElseThrow();
        Employee persistedAda = employeeRepository.findById(ada.getId()).orElseThrow();

        assertThat(planningRuns).hasSize(1);
        assertThat(assignments).hasSize(1);

        Assignment assignment = assignments.getFirst();
        assertThat(assignment.getAssigned()).isTrue();
        assertThat(assignment.getEmployee().getId()).isEqualTo(ada.getId());
        assertThat(assignment.getEmployee().getName()).isEqualTo("Ada");
        assertThat(assignment.getTask().getId()).isEqualTo(task.getId());
        assertThat(assignment.getScore()).isPositive();

        assertThat(persistedTask.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
        assertThat(persistedTask.getAssignedEmployee().getId()).isEqualTo(ada.getId());
        assertThat(persistedAda.getCurrentWeeklyHours()).isEqualTo(16);
    }

    @Test
    void planningRun_shouldPersistUnassignedAssignment_whenNoEmployeeIsEligible() {
        Skill java = skillRepository.save(new Skill("Java"));
        Skill python = skillRepository.save(new Skill("Python"));

        Employee employee = new Employee("Grace", "grace.integration@smartops.test", 40, SeniorityLevel.SENIOR);
        employee.setCurrentWeeklyHours(10);
        employee.setSkills(new HashSet<>(Set.of(python)));
        employeeRepository.save(employee);

        Task task = new Task("Build Java integration", TaskPriority.HIGH, 6);
        task.setDeadline(LocalDate.now().plusDays(7));
        task.setRequiredSkills(new HashSet<>(Set.of(java)));
        task = taskRepository.save(task);

        planningService.runPlanning();

        List<PlanningRun> planningRuns = planningRunRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        Task persistedTask = taskRepository.findById(task.getId()).orElseThrow();

        assertThat(planningRuns).hasSize(1);
        assertThat(assignments).hasSize(1);

        Assignment assignment = assignments.getFirst();
        assertThat(assignment.getAssigned()).isFalse();
        assertThat(assignment.getEmployee()).isNull();
        assertThat(assignment.getTask().getId()).isEqualTo(task.getId());
        assertThat(assignment.getExplanation()).containsIgnoringCase("No eligible employee");
        assertThat(persistedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(persistedTask.getAssignedEmployee()).isNull();
    }

}
