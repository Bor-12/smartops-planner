package com.smartops.planner.planning;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private PlanningRunRepository planningRunRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private ScoringService scoringService;

    private PlanningService planningService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-27T10:00:00Z"), ZoneOffset.UTC);
        planningService = new PlanningService(
                planningRunRepository,
                assignmentRepository,
                taskRepository,
                employeeRepository,
                scoringService,
                clock
        );
    }

    @Test
    void runPlanning_shouldAssignTaskToBestEmployee() {
        stubPlanningRunPersistence();
        Skill java = skill(1L, "Java");
        Task task = task(1L, "Build planning API", 6, Set.of(java));
        Employee goodEmployee = employee(1L, "Good Employee", 10, 40, Set.of(java));
        Employee bestEmployee = employee(2L, "Best Employee", 5, 40, Set.of(java));

        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(employeeRepository.findAll()).thenReturn(List.of(goodEmployee, bestEmployee));
        when(scoringService.calculateScore(task, goodEmployee))
                .thenReturn(score(80, true, "Good employee is valid."));
        when(scoringService.calculateScore(task, bestEmployee))
                .thenReturn(score(95, true, "Best employee is better."));

        planningService.runPlanning();

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());

        Assignment assignment = assignmentCaptor.getValue();
        assertTrue(assignment.getAssigned());
        assertEquals(bestEmployee, assignment.getEmployee());
        assertEquals(task, assignment.getTask());
        assertEquals(95, assignment.getScore());
        assertEquals("Best employee is better.", assignment.getExplanation());
    }

    @Test
    void runPlanning_shouldNotAssignTask_whenNoEmployeeIsEligible() {
        stubPlanningRunPersistence();
        Skill java = skill(1L, "Java");
        Task task = task(1L, "Build planning API", 6, Set.of(java));
        Employee employee = employee(1L, "Unavailable Employee", 10, 40, Set.of());

        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(scoringService.calculateScore(task, employee))
                .thenReturn(score(10, false, "Missing required skills."));

        planningService.runPlanning();

        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());

        Assignment assignment = assignmentCaptor.getValue();
        assertFalse(assignment.getAssigned());
        assertNull(assignment.getEmployee());
        assertEquals(task, assignment.getTask());
        assertEquals(0, assignment.getScore());
        assertTrue(assignment.getExplanation().contains("No se ha encontrado ningun empleado apto"));
        verify(taskRepository, never()).save(any(Task.class));
        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void runPlanning_shouldSavePlanningRunSummary() {
        stubPlanningRunPersistence();
        Task task = task(1L, "Build planning API", 6, Set.of(skill(1L, "Java")));

        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(employeeRepository.findAll()).thenReturn(List.of());

        PlanningRunResponse response = planningService.runPlanning();

        ArgumentCaptor<PlanningRun> runCaptor = ArgumentCaptor.forClass(PlanningRun.class);
        verify(planningRunRepository, org.mockito.Mockito.times(2)).save(runCaptor.capture());

        PlanningRun completedRun = runCaptor.getAllValues().get(1);
        assertEquals(PlanningRunStatus.COMPLETED, completedRun.getStatus());
        assertEquals(1, completedRun.getTotalTasks());
        assertEquals(0, completedRun.getAssignedTasks());
        assertEquals(1, completedRun.getUnassignedTasks());
        assertTrue(completedRun.getSummary().contains("0 assigned, 1 unassigned"));
        assertEquals(PlanningRunStatus.COMPLETED, response.status());
    }

    @Test
    void runPlanning_shouldUpdateTaskStatusAndEmployeeWeeklyLoad() {
        stubPlanningRunPersistence();
        Skill java = skill(1L, "Java");
        Task task = task(1L, "Build planning API", 6, Set.of(java));
        Employee employee = employee(1L, "Ada", 10, 40, Set.of(java));

        when(taskRepository.findByStatus(TaskStatus.PENDING)).thenReturn(List.of(task));
        when(employeeRepository.findAll()).thenReturn(List.of(employee));
        when(scoringService.calculateScore(task, employee))
                .thenReturn(score(90, true, "Ada is the best match."));

        planningService.runPlanning();

        assertEquals(TaskStatus.ASSIGNED, task.getStatus());
        assertEquals(employee, task.getAssignedEmployee());
        assertEquals(16, employee.getCurrentWeeklyHours());
        verify(taskRepository).save(task);
        verify(employeeRepository).save(employee);
    }

    @Test
    void findRunById_shouldReturnPlanningRun() {
        PlanningRun run = new PlanningRun(Instant.parse("2026-05-27T10:00:00Z"));
        ReflectionTestUtils.setField(run, "id", 1L);
        run.complete(0, 0, 0, "No pending tasks.", Instant.parse("2026-05-27T10:00:01Z"));

        when(planningRunRepository.findById(1L)).thenReturn(Optional.of(run));
        when(assignmentRepository.findByPlanningRunId(1L)).thenReturn(List.of());

        PlanningRunResponse response = planningService.findRunById(1L);

        assertEquals(1L, response.id());
        assertEquals(PlanningRunStatus.COMPLETED, response.status());
        assertEquals("No pending tasks.", response.summary());
    }

    private AssignmentScore score(int score, boolean eligible, String explanation) {
        return new AssignmentScore(score, eligible, explanation, List.of(), List.of());
    }

    private void stubPlanningRunPersistence() {
        when(taskRepository.findByStatus(TaskStatus.ASSIGNED)).thenReturn(List.of());
        when(planningRunRepository.save(any(PlanningRun.class))).thenAnswer(invocation -> {
            PlanningRun planningRun = invocation.getArgument(0);
            if (planningRun.getId() == null) {
                ReflectionTestUtils.setField(planningRun, "id", 1L);
            }
            return planningRun;
        });
        when(assignmentRepository.findByPlanningRunId(1L)).thenReturn(List.of());
    }

    private Task task(Long id, String title, int estimatedHours, Set<Skill> requiredSkills) {
        Task task = new Task(title, TaskPriority.HIGH, estimatedHours);
        task.setDeadline(LocalDate.of(2026, 6, 15));
        task.setRequiredSkills(requiredSkills);
        ReflectionTestUtils.setField(task, "id", id);
        return task;
    }

    private Employee employee(Long id, String name, int currentWeeklyHours, int maxWeeklyHours, Set<Skill> skills) {
        Employee employee = new Employee(name, name.toLowerCase().replace(" ", ".") + "@smartops.test", maxWeeklyHours, SeniorityLevel.SENIOR);
        employee.setCurrentWeeklyHours(currentWeeklyHours);
        employee.setSkills(skills);
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private Skill skill(Long id, String name) {
        Skill skill = new Skill(name);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }
}
