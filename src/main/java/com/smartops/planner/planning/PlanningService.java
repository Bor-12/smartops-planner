package com.smartops.planner.planning;

import com.smartops.planner.common.exception.ResourceNotFoundException;
import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.EmployeeRepository;
import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskRepository;
import com.smartops.planner.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanningService {

    private final PlanningRunRepository planningRunRepository;
    private final AssignmentRepository assignmentRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final ScoringService scoringService;
    private final Clock clock;

    @Autowired
    public PlanningService(
            PlanningRunRepository planningRunRepository,
            AssignmentRepository assignmentRepository,
            TaskRepository taskRepository,
            EmployeeRepository employeeRepository,
            ScoringService scoringService
    ) {
        this(planningRunRepository, assignmentRepository, taskRepository, employeeRepository, scoringService, Clock.systemDefaultZone());
    }

    PlanningService(
            PlanningRunRepository planningRunRepository,
            AssignmentRepository assignmentRepository,
            TaskRepository taskRepository,
            EmployeeRepository employeeRepository,
            ScoringService scoringService,
            Clock clock
    ) {
        this.planningRunRepository = planningRunRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.scoringService = scoringService;
        this.clock = clock;
    }

    @Transactional
    public PlanningRunResponse runPlanning() {
        PlanningRun planningRun = planningRunRepository.save(new PlanningRun(now()));
        List<Task> pendingTasks = taskRepository.findByStatus(TaskStatus.PENDING);
        List<Employee> availableEmployees = findAvailableEmployees();

        int assignedTasks = 0;
        int unassignedTasks = 0;

        for (Task task : pendingTasks) {
            Optional<CandidateScore> bestCandidate = findBestCandidate(task, availableEmployees);
            if (bestCandidate.isPresent()) {
                assignTask(planningRun, task, bestCandidate.get());
                assignedTasks++;
            } else {
                registerUnassignedTask(planningRun, task);
                unassignedTasks++;
            }
        }

        String summary = "Planning run completed: " + assignedTasks
                + " assigned, " + unassignedTasks
                + " unassigned out of " + pendingTasks.size() + " pending tasks.";
        planningRun.complete(pendingTasks.size(), assignedTasks, unassignedTasks, summary, now());
        PlanningRun savedRun = planningRunRepository.save(planningRun);

        return toPlanningRunResponse(savedRun, assignmentRepository.findByPlanningRunId(savedRun.getId()));
    }

    @Transactional(readOnly = true)
    public List<PlanningRunResponse> findAllRuns() {
        return planningRunRepository.findAll()
                .stream()
                .map(run -> toPlanningRunResponse(run, assignmentRepository.findByPlanningRunId(run.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanningRunResponse findRunById(Long id) {
        PlanningRun planningRun = planningRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Planning run not found with id " + id));

        return toPlanningRunResponse(planningRun, assignmentRepository.findByPlanningRunId(id));
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> findAllAssignments() {
        return assignmentRepository.findAll()
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse findAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .map(this::toAssignmentResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id " + id));
    }

    private List<Employee> findAvailableEmployees() {
        return employeeRepository.findAll()
                .stream()
                .filter(employee -> employee.getCurrentWeeklyHours() < employee.getMaxWeeklyHours())
                .toList();
    }

    private Optional<CandidateScore> findBestCandidate(Task task, List<Employee> employees) {
        return employees.stream()
                .map(employee -> new CandidateScore(employee, scoringService.calculateScore(task, employee)))
                .filter(candidate -> candidate.score().eligible())
                .max(Comparator.comparingInt(candidate -> candidate.score().score()));
    }

    private void assignTask(PlanningRun planningRun, Task task, CandidateScore candidate) {
        Employee employee = candidate.employee();
        AssignmentScore score = candidate.score();

        task.setAssignedEmployee(employee);
        task.setStatus(TaskStatus.ASSIGNED);
        employee.setCurrentWeeklyHours(employee.getCurrentWeeklyHours() + task.getEstimatedHours());

        employeeRepository.save(employee);
        taskRepository.save(task);
        assignmentRepository.save(new Assignment(
                planningRun,
                task,
                employee,
                score.score(),
                true,
                score.explanation(),
                now()
        ));
    }

    private void registerUnassignedTask(PlanningRun planningRun, Task task) {
        String explanation = "No eligible employee found for task " + task.getTitle() + ".";
        assignmentRepository.save(new Assignment(
                planningRun,
                task,
                null,
                0,
                false,
                explanation,
                now()
        ));
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private PlanningRunResponse toPlanningRunResponse(PlanningRun planningRun, List<Assignment> assignments) {
        return new PlanningRunResponse(
                planningRun.getId(),
                planningRun.getStatus(),
                planningRun.getStartedAt(),
                planningRun.getFinishedAt(),
                planningRun.getTotalTasks(),
                planningRun.getAssignedTasks(),
                planningRun.getUnassignedTasks(),
                planningRun.getSummary(),
                assignments.stream()
                        .map(this::toAssignmentResponse)
                        .toList()
        );
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment) {
        Employee employee = assignment.getEmployee();
        Task task = assignment.getTask();

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getPlanningRun().getId(),
                task.getId(),
                task.getTitle(),
                employee == null ? null : employee.getId(),
                employee == null ? null : employee.getName(),
                assignment.getScore(),
                assignment.getAssigned(),
                assignment.getExplanation(),
                assignment.getCreatedAt()
        );
    }

    private record CandidateScore(Employee employee, AssignmentScore score) {
    }
}
