package com.smartops.planner.planning;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.employee.SeniorityLevel;
import com.smartops.planner.skill.Skill;
import com.smartops.planner.task.Task;
import com.smartops.planner.task.TaskPriority;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ScoringService {

    private static final int POINTS_PER_MATCHING_SKILL = 20;
    private static final int POINTS_PER_EXTRA_SKILL = 5;
    private static final int MAX_EXTRA_SKILL_BONUS = 10;
    private static final int PENALTY_PER_MISSING_SKILL = -25;

    private final Clock clock;

    public ScoringService() {
        this(Clock.systemDefaultZone());
    }

    public ScoringService(Clock clock) {
        this.clock = clock;
    }

    public AssignmentScore calculateScore(Task task, Employee employee) {
        ScoreContext context = new ScoreContext(task, employee);

        applySkillScore(context);
        applySeniorityScore(context);
        applyPriorityScore(context);
        applyWorkloadScore(context);
        applyCapacityScore(context);
        applyDeadlineScore(context);

        return buildAssignmentScore(context);
    }

    private void applySkillScore(ScoreContext context) {
        if (context.skillMatch.requiredCount() == 0) {
            addPositive(context, ScoreReason.MATCHING_SKILLS, 10, "La tarea no requiere habilidades específicas.");
        } else {
            applyRequiredSkillScore(context);
        }

        applyExtraSkillScore(context);
    }

    private void applyRequiredSkillScore(ScoreContext context) {
        SkillMatch skillMatch = context.skillMatch;
        int skillPoints = skillMatch.matchingCount() * POINTS_PER_MATCHING_SKILL;

        if (skillPoints > 0) {
            addPositive(
                    context,
                    ScoreReason.MATCHING_SKILLS,
                    skillPoints,
                    "Cumple " + skillMatch.matchingCount() + "/" + skillMatch.requiredCount() + " habilidades requeridas."
            );
        }

        if (skillMatch.missingCount() > 0) {
            context.eligible = false;
            addPenalty(
                    context,
                    ScoreReason.MISSING_SKILLS,
                    skillMatch.missingCount() * PENALTY_PER_MISSING_SKILL,
                    "Le faltan " + skillMatch.missingCount() + "/" + skillMatch.requiredCount() + " habilidades requeridas."
            );
        }
    }

    private void applyExtraSkillScore(ScoreContext context) {
        if (context.skillMatch.extraCount() <= 0) {
            return;
        }

        int extraSkillPoints = Math.min(
                context.skillMatch.extraCount() * POINTS_PER_EXTRA_SKILL,
                MAX_EXTRA_SKILL_BONUS
        );
        addPositive(
                context,
                ScoreReason.MATCHING_SKILLS,
                extraSkillPoints,
                "Aporta " + context.skillMatch.extraCount() + " habilidades adicionales utiles."
        );
    }

    private void applySeniorityScore(ScoreContext context) {
        if (hasEnoughSeniority(context.task, context.employee)) {
            addPositive(context, ScoreReason.SENIORITY_MATCH, 15, "Tiene seniority suficiente para la prioridad de la tarea.");
        } else {
            addPenalty(context, ScoreReason.SENIORITY_GAP, -15, "Su seniority es bajo para la prioridad de la tarea.");
        }
    }

    private void applyPriorityScore(ScoreContext context) {
        if (context.task.getPriority() == TaskPriority.URGENT && !isSenior(context.employee)) {
            addPenalty(context, ScoreReason.CRITICAL_TASK, -30, "La tarea es crítica y el empleado no es senior.");
        }
    }

    private void applyWorkloadScore(ScoreContext context) {
        if (context.workloadRatio < 0.60) {
            addPositive(context, ScoreReason.LOW_WORKLOAD, 15, "Su carga semanal es inferior al 60%.");
        } else if (context.workloadRatio < 0.80) {
            addPositive(context, ScoreReason.LOW_WORKLOAD, 8, "Tiene carga semanal moderada.");
        } else {
            addPenalty(context, ScoreReason.HIGH_WORKLOAD, -10, "Tiene una carga semanal alta.");
        }
    }

    private void applyCapacityScore(ScoreContext context) {
        int remainingHours = context.employee.getMaxWeeklyHours() - context.employee.getCurrentWeeklyHours();
        if (remainingHours >= context.task.getEstimatedHours()) {
            addPositive(context, ScoreReason.CAPACITY_AVAILABLE, 20, "Tiene disponibilidad suficiente para asumir las horas estimadas.");
        } else {
            context.eligible = false;
            addPenalty(context, ScoreReason.CAPACITY_EXCEEDED, -40, "Superaría sus horas máximas semanales.");
        }
    }

    private void applyDeadlineScore(ScoreContext context) {
        if (isDeadlineClose(context.task) && context.workloadRatio >= 0.60) {
            addPenalty(context, ScoreReason.NEAR_DEADLINE, -15, "El deadline está cercano y el empleado ya tiene carga relevante.");
        }
    }

    private AssignmentScore buildAssignmentScore(ScoreContext context) {
        return new AssignmentScore(
                context.score,
                context.eligible,
                buildExplanation(
                        context.task,
                        context.employee,
                        context.skillMatch,
                        context.workloadRatio,
                        context.eligible
                ),
                List.copyOf(context.positives),
                List.copyOf(context.penalties)
        );
    }

    private SkillMatch calculateSkillMatch(Task task, Employee employee) {
        Set<String> requiredSkills = task.getRequiredSkills()
                .stream()
                .map(this::skillKey)
                .collect(Collectors.toSet());

        Set<String> employeeSkills = employee.getSkills()
                .stream()
                .map(this::skillKey)
                .collect(Collectors.toSet());

        long matching = requiredSkills
                .stream()
                .filter(employeeSkills::contains)
                .count();

        long extra = employeeSkills
                .stream()
                .filter(skill -> !requiredSkills.contains(skill))
                .count();

        int required = requiredSkills.size();
        return new SkillMatch(required, (int) matching, required - (int) matching, (int) extra);
    }

    private String skillKey(Skill skill) {
        if (skill.getId() != null) {
            return "id:" + skill.getId();
        }

        return "name:" + skill.getName().trim().toLowerCase();
    }

    private boolean hasEnoughSeniority(Task task, Employee employee) {
        return seniorityRank(employee.getSeniorityLevel()) >= requiredSeniorityRank(task.getPriority());
    }

    private boolean isSenior(Employee employee) {
        return employee.getSeniorityLevel() == SeniorityLevel.SENIOR
                || employee.getSeniorityLevel() == SeniorityLevel.LEAD;
    }

    private int seniorityRank(SeniorityLevel seniorityLevel) {
        return switch (seniorityLevel) {
            case JUNIOR -> 1;
            case MID -> 2;
            case SENIOR -> 3;
            case LEAD -> 4;
        };
    }

    private int requiredSeniorityRank(TaskPriority priority) {
        return switch (priority) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH, URGENT -> 3;
        };
    }

    private double workloadRatio(Employee employee) {
        if (employee.getMaxWeeklyHours() == null || employee.getMaxWeeklyHours() <= 0) {
            return 1.0;
        }

        return employee.getCurrentWeeklyHours() / (double) employee.getMaxWeeklyHours();
    }

    private boolean isDeadlineClose(Task task) {
        if (task.getDeadline() == null) {
            return false;
        }

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(clock), task.getDeadline());
        return daysUntilDeadline >= 0 && daysUntilDeadline <= 3;
    }

    private int addPositive(List<ScoreExplanation> positives, ScoreReason reason, int points, String message) {
        positives.add(new ScoreExplanation(reason, points, message));
        return points;
    }

    private int addPenalty(List<ScoreExplanation> penalties, ScoreReason reason, int points, String message) {
        penalties.add(new ScoreExplanation(reason, points, message));
        return points;
    }

    private void addPositive(ScoreContext context, ScoreReason reason, int points, String message) {
        context.score += addPositive(context.positives, reason, points, message);
    }

    private void addPenalty(ScoreContext context, ScoreReason reason, int points, String message) {
        context.score += addPenalty(context.penalties, reason, points, message);
    }

    private String buildExplanation(
            Task task,
            Employee employee,
            SkillMatch skillMatch,
            double workloadRatio,
            boolean eligible
    ) {
        String result = eligible ? "es apto" : "no es apto";
        int workloadPercentage = (int) Math.round(workloadRatio * 100);

        return "La tarea " + task.getTitle()
                + " para " + employee.getName()
                + " " + result
                + " porque cumple " + skillMatch.matchingCount() + "/" + skillMatch.requiredCount()
                + " habilidades requeridas, tiene una carga semanal del " + workloadPercentage
                + "% y " + (employee.getMaxWeeklyHours() - employee.getCurrentWeeklyHours())
                + " horas disponibles.";
    }

    private record SkillMatch(
            int requiredCount,
            int matchingCount,
            int missingCount,
            int extraCount
    ) {
    }

    private class ScoreContext {

        private final Task task;
        private final Employee employee;
        private final List<ScoreExplanation> positives = new ArrayList<>();
        private final List<ScoreExplanation> penalties = new ArrayList<>();
        private final SkillMatch skillMatch;
        private final double workloadRatio;
        private int score = 0;
        private boolean eligible = true;

        private ScoreContext(Task task, Employee employee) {
            this.task = task;
            this.employee = employee;
            this.skillMatch = calculateSkillMatch(task, employee);
            this.workloadRatio = workloadRatio(employee);
        }
    }
}
