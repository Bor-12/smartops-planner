package com.smartops.planner.planning;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    void calculateScore_shouldReturnEligibleAndHighScore_whenEmployeeMatchesAllRequiredSkills() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java, spring));

        Employee employee = employee(
                "Ada Lovelace",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java, spring)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertTrue(eligibleOf(result), "Employee with all required skills should be eligible");
        assertTrue(scoreOf(result) > 0, "Score should be positive when employee matches the task");
        assertExplanationContains(result, "skill", "habilidad", "match", "cumple");
    }

    @Test
    void calculateScore_shouldPenalizeOrReject_whenEmployeeMissesRequiredSkills() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java, spring));

        Employee completeEmployee = employee(
                "Complete Employee",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java, spring)
        );

        Employee incompleteEmployee = employee(
                "Incomplete Employee",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java)
        );

        Object completeResult = scoringService.calculateScore(task, completeEmployee);
        Object incompleteResult = scoringService.calculateScore(task, incompleteEmployee);

        assertTrue(
                scoreOf(completeResult) > scoreOf(incompleteResult),
                "Employee with all skills should score higher than employee missing skills"
        );

        assertTrue(
                !eligibleOf(incompleteResult) || explanationContains(incompleteResult, "missing", "falt", "skill", "habilidad"),
                "Missing skills should either make the employee ineligible or appear in the explanation"
        );
    }

    @Test
    void calculateScore_shouldReject_whenTaskWouldExceedMaxWeeklyHours() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee overloadedEmployee = employee(
                "Overloaded Employee",
                SeniorityLevel.SENIOR,
                40,
                38,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, overloadedEmployee);

        assertFalse(eligibleOf(result), "Employee should be rejected if task exceeds max weekly hours");
        assertExplanationContains(result, "hour", "hora", "overload", "sobrecarga", "max");
    }

    @Test
    void calculateScore_shouldPreferSeniorEmployee_forCriticalTask() {
        Skill java = skill(1L, "Java");

        Task task = task("Fix production incident", criticalPriority(), 4, Set.of(java));

        Employee junior = employee(
                "Junior Employee",
                SeniorityLevel.JUNIOR,
                40,
                5,
                Set.of(java)
        );

        Employee senior = employee(
                "Senior Employee",
                SeniorityLevel.SENIOR,
                40,
                5,
                Set.of(java)
        );

        Object juniorResult = scoringService.calculateScore(task, junior);
        Object seniorResult = scoringService.calculateScore(task, senior);

        assertTrue(
                scoreOf(seniorResult) > scoreOf(juniorResult),
                "Senior employee should score higher than junior employee for critical tasks"
        );
    }

    @Test
    void calculateScore_shouldPreferLessLoadedEmployee_whenBothAreQualified() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee lessLoaded = employee(
                "Less Loaded",
                SeniorityLevel.SENIOR,
                40,
                5,
                Set.of(java)
        );

        Employee moreLoaded = employee(
                "More Loaded",
                SeniorityLevel.SENIOR,
                40,
                30,
                Set.of(java)
        );

        Object lessLoadedResult = scoringService.calculateScore(task, lessLoaded);
        Object moreLoadedResult = scoringService.calculateScore(task, moreLoaded);

        assertTrue(
                scoreOf(lessLoadedResult) > scoreOf(moreLoadedResult),
                "Less loaded employee should score higher when both employees are qualified"
        );
    }

    @Test
    void calculateScore_shouldIncludeHumanReadableExplanation() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee employee = employee(
                "Ada Lovelace",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        String explanation = explanationOf(result);

        assertNotNull(explanation);
        assertFalse(explanation.isBlank(), "Explanation should not be blank");
    }

    @Test
    void calculateScore_shouldPenalize_whenDeadlineIsCloseAndEmployeeIsHighlyLoaded() {
        Skill java = skill(1L, "Java");

        Employee loadedEmployee = employee(
                "Loaded Employee",
                SeniorityLevel.SENIOR,
                40,
                28,
                Set.of(java)
        );

        Task closeDeadlineTask = taskWithDeadline(
                "Close deadline task",
                TaskPriority.HIGH,
                4,
                Set.of(java),
                LocalDate.now().plusDays(1)
        );

        Task farDeadlineTask = taskWithDeadline(
                "Far deadline task",
                TaskPriority.HIGH,
                4,
                Set.of(java),
                LocalDate.now().plusDays(30)
        );

        Object closeDeadlineResult = scoringService.calculateScore(closeDeadlineTask, loadedEmployee);
        Object farDeadlineResult = scoringService.calculateScore(farDeadlineTask, loadedEmployee);

        assertTrue(
                scoreOf(farDeadlineResult) > scoreOf(closeDeadlineResult),
                "A close deadline with a highly loaded employee should receive a lower score"
        );
    }

    @Test
    void calculateScore_shouldRewardHighlyQualifiedEmployee() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");
        Skill docker = skill(3L, "Docker");
        Skill postgres = skill(4L, "PostgreSQL");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java, spring));

        Employee exactMatchEmployee = employee(
                "Exact Match Employee",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java, spring)
        );

        Employee highlyQualifiedEmployee = employee(
                "Highly Qualified Employee",
                SeniorityLevel.LEAD,
                40,
                10,
                Set.of(java, spring, docker, postgres)
        );

        Object exactMatchResult = scoringService.calculateScore(task, exactMatchEmployee);
        Object highlyQualifiedResult = scoringService.calculateScore(task, highlyQualifiedEmployee);

        assertTrue(
                scoreOf(highlyQualifiedResult) > scoreOf(exactMatchResult),
                "A highly qualified employee should score higher than an employee who only meets the minimum requirements"
        );
    }

    @Test
    void calculateScore_shouldRankBestCandidateAmongSeveralEmployees() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java, spring));

        Employee badCandidate = employee(
                "Bad Candidate",
                SeniorityLevel.JUNIOR,
                40,
                32,
                Set.of(java)
        );

        Employee acceptableCandidate = employee(
                "Acceptable Candidate",
                SeniorityLevel.MID,
                40,
                15,
                Set.of(java, spring)
        );

        Employee bestCandidate = employee(
                "Best Candidate",
                SeniorityLevel.SENIOR,
                40,
                5,
                Set.of(java, spring)
        );

        Object badResult = scoringService.calculateScore(task, badCandidate);
        Object acceptableResult = scoringService.calculateScore(task, acceptableCandidate);
        Object bestResult = scoringService.calculateScore(task, bestCandidate);

        assertTrue(
                scoreOf(bestResult) > scoreOf(acceptableResult),
                "Best candidate should score higher than acceptable candidate"
        );

        assertTrue(
                scoreOf(acceptableResult) > scoreOf(badResult),
                "Acceptable candidate should score higher than bad candidate"
        );

        assertTrue(
                eligibleOf(bestResult),
                "Best candidate should be eligible"
        );
    }
    @Test
    void calculateScore_shouldIncludeMissingSkillsReason_whenEmployeeMissesRequiredSkills() {
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring Boot");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java, spring));

        Employee employee = employee(
                "Incomplete Employee",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertFalse(eligibleOf(result));
        assertPenaltyReason(result, ScoreReason.MISSING_SKILLS);
    }

    @Test
    void calculateScore_shouldIncludeCapacityExceededReason_whenTaskExceedsMaxWeeklyHours() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee employee = employee(
                "Overloaded Employee",
                SeniorityLevel.SENIOR,
                40,
                38,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertFalse(eligibleOf(result));
        assertPenaltyReason(result, ScoreReason.CAPACITY_EXCEEDED);
    }

    @Test
    void calculateScore_shouldIncludeNearDeadlineReason_whenDeadlineIsCloseAndEmployeeIsLoaded() {
        Skill java = skill(1L, "Java");

        Task task = taskWithDeadline(
                "Close deadline task",
                TaskPriority.HIGH,
                4,
                Set.of(java),
                LocalDate.now().plusDays(1)
        );

        Employee employee = employee(
                "Loaded Employee",
                SeniorityLevel.SENIOR,
                40,
                28,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertPenaltyReason(result, ScoreReason.NEAR_DEADLINE);
    }

    @Test
    void calculateScore_shouldIncludeCriticalTaskReason_whenTaskIsCriticalAndEmployeeIsNotSenior() {
        Skill java = skill(1L, "Java");

        Task task = task(
                "Fix production incident",
                criticalPriority(),
                4,
                Set.of(java)
        );

        Employee junior = employee(
                "Junior Employee",
                SeniorityLevel.JUNIOR,
                40,
                5,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, junior);

        assertPenaltyReason(result, ScoreReason.CRITICAL_TASK);
    }

    @Test
    void calculateScore_shouldIncludeCapacityAvailableReason_whenEmployeeHasEnoughRemainingHours() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee employee = employee(
                "Available Employee",
                SeniorityLevel.SENIOR,
                40,
                10,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertPositiveReason(result, ScoreReason.CAPACITY_AVAILABLE);
    }

    @Test
    void calculateScore_shouldIncludeLowWorkloadReason_whenEmployeeHasLowWorkload() {
        Skill java = skill(1L, "Java");

        Task task = task("Build planning API", TaskPriority.HIGH, 6, Set.of(java));

        Employee employee = employee(
                "Low Workload Employee",
                SeniorityLevel.SENIOR,
                40,
                5,
                Set.of(java)
        );

        Object result = scoringService.calculateScore(task, employee);

        assertPositiveReason(result, ScoreReason.LOW_WORKLOAD);
    }

    private Task task(String title, TaskPriority priority, int estimatedHours, Set<Skill> requiredSkills) {
        Task task = new Task(title, priority, estimatedHours);
        task.setDeadline(LocalDate.of(2026, 6, 15));
        task.setRequiredSkills(requiredSkills);
        return task;
    }

    private Task taskWithDeadline(
            String title,
            TaskPriority priority,
            int estimatedHours,
            Set<Skill> requiredSkills,
            LocalDate deadline
    ) {
        Task task = task(title, priority, estimatedHours, requiredSkills);
        task.setDeadline(deadline);
        return task;
    }

    private Employee employee(
            String name,
            SeniorityLevel seniorityLevel,
            int maxWeeklyHours,
            int currentWeeklyHours,
            Set<Skill> skills
    ) {
        Employee employee = new Employee(
                name,
                name.toLowerCase().replace(" ", ".") + "@smartops.test",
                maxWeeklyHours,
                seniorityLevel
        );
        employee.setCurrentWeeklyHours(currentWeeklyHours);
        employee.setSkills(skills);
        return employee;
    }

    private Skill skill(Long id, String name) {
        Skill skill = new Skill(name);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    private TaskPriority criticalPriority() {
        try {
            return TaskPriority.valueOf("CRITICAL");
        } catch (IllegalArgumentException exception) {
            return TaskPriority.valueOf("URGENT");
        }
    }

    private int scoreOf(Object result) {
        Object value = readProperty(result, "score", "getScore");
        assertTrue(value instanceof Number, "AssignmentScore must expose a numeric score");
        return ((Number) value).intValue();
    }

    private boolean eligibleOf(Object result) {
        Object value = readProperty(
                result,
                "eligible",
                "isEligible",
                "assignable",
                "isAssignable",
                "suitable",
                "isSuitable"
        );

        assertTrue(value instanceof Boolean, "AssignmentScore must expose an eligible/assignable boolean");
        return (Boolean) value;
    }

    private String explanationOf(Object result) {
        Object value = readProperty(
                result,
                "explanation",
                "getExplanation",
                "scoreExplanation",
                "getScoreExplanation",
                "message",
                "getMessage",
                "summary",
                "getSummary"
        );

        return String.valueOf(value);
    }

    private boolean explanationContains(Object result, String... fragments) {
        String explanation = explanationOf(result).toLowerCase();

        for (String fragment : fragments) {
            if (explanation.contains(fragment.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private void assertExplanationContains(Object result, String... fragments) {
        assertTrue(
                explanationContains(result, fragments),
                "Explanation should contain one of: " + String.join(", ", fragments)
                        + ". Actual explanation: " + explanationOf(result)
        );
    }

    private Object readProperty(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = target.getClass().getMethod(name);
                return method.invoke(target);
            } catch (ReflectiveOperationException ignored) {
                // Try next method name.
            }
        }

        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                // Try next field name.
            }
        }

        fail("Could not read any property from "
                + target.getClass().getSimpleName()
                + ". Tried: "
                + String.join(", ", names));

        return null;
    }
    @SuppressWarnings("unchecked")
    private List<Object> positivesOf(Object result) {
        Object value = readProperty(result, "positives", "getPositives", "positiveReasons", "getPositiveReasons");
        assertTrue(value instanceof List<?>, "AssignmentScore must expose positives as a List");
        return (List<Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> penaltiesOf(Object result) {
        Object value = readProperty(result, "penalties", "getPenalties", "penaltyReasons", "getPenaltyReasons");
        assertTrue(value instanceof List<?>, "AssignmentScore must expose penalties as a List");
        return (List<Object>) value;
    }

    private void assertPositiveReason(Object result, ScoreReason expectedReason) {
        assertTrue(
                positivesOf(result).stream()
                        .map(this::reasonOf)
                        .anyMatch(expectedReason::equals),
                "Expected positive reason: " + expectedReason
        );
    }

    private void assertPenaltyReason(Object result, ScoreReason expectedReason) {
        assertTrue(
                penaltiesOf(result).stream()
                        .map(this::reasonOf)
                        .anyMatch(expectedReason::equals),
                "Expected penalty reason: " + expectedReason
        );
    }

    private ScoreReason reasonOf(Object scoreExplanation) {
        Object value = readProperty(scoreExplanation, "reason", "getReason");
        assertTrue(value instanceof ScoreReason, "ScoreExplanation must expose a ScoreReason");
        return (ScoreReason) value;
    }
}
