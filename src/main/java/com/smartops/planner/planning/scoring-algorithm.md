# Assignment Scoring Algorithm

## Purpose

`ScoringService` evaluates how suitable an employee is for a task.

It receives a `Task` and an `Employee`, applies a set of scoring rules, and returns an `AssignmentScore`.

```java
AssignmentScore calculateScore(Task task, Employee employee)
```

The result is used by the planning module to compare candidates and select the best assignment.

---

## Output

```java
public record AssignmentScore(
        int score,
        boolean eligible,
        String explanation,
        List<ScoreExplanation> positives,
        List<ScoreExplanation> penalties
) {
}
```

| Field | Description |
|---|---|
| `score` | Final numeric score. Higher means better candidate. |
| `eligible` | Whether the employee can be assigned to the task. |
| `explanation` | Human-readable summary. |
| `positives` | Positive scoring reasons. |
| `penalties` | Negative scoring reasons. |

Each scoring reason is represented as:

```java
public record ScoreExplanation(
        ScoreReason reason,
        int points,
        String message
) {
}
```

---

## Scoring rules

### Required skills

Each required skill matched by the employee adds:

```text
+20 points
```

Each missing required skill applies:

```text
-25 points
eligible = false
```

Reasons:

```java
MATCHING_SKILLS
MISSING_SKILLS
```

---

### Extra skills

Employee skills not required by the task add a capped bonus:

```text
+5 points per extra skill
maximum bonus: +10 points
```

Reason:

```java
MATCHING_SKILLS
```

---

### Seniority

Task priority defines the recommended minimum seniority:

| Priority | Minimum seniority |
|---|---|
| `LOW` | `JUNIOR` |
| `MEDIUM` | `MID` |
| `HIGH` | `SENIOR` |
| `URGENT` | `SENIOR` |

If the employee meets the required seniority:

```text
+15 points
```

Otherwise:

```text
-15 points
```

Reasons:

```java
SENIORITY_MATCH
SENIORITY_GAP
```

---

### Urgent tasks

If the task priority is `URGENT` and the employee is not `SENIOR` or `LEAD`:

```text
-30 points
```

Reason:

```java
CRITICAL_TASK
```

This is a soft penalty. It does not make the employee ineligible by itself.

---

### Workload

Workload is calculated as:

```text
currentWeeklyHours / maxWeeklyHours
```

| Workload | Score |
|---|---|
| `< 60%` | `+15` |
| `>= 60%` and `< 80%` | `+8` |
| `>= 80%` | `-10` |

Reasons:

```java
LOW_WORKLOAD
HIGH_WORKLOAD
```

---

### Weekly capacity

Remaining capacity is calculated as:

```text
maxWeeklyHours - currentWeeklyHours
```

If the employee has enough remaining hours for the task:

```text
+20 points
```

If the task would exceed the employee's weekly limit:

```text
-40 points
eligible = false
```

Reasons:

```java
CAPACITY_AVAILABLE
CAPACITY_EXCEEDED
```

---

### Deadline pressure

A deadline is considered close when it is within the next 3 days:

```text
0 <= daysUntilDeadline <= 3
```

If the deadline is close and the employee already has relevant workload:

```text
-15 points
```

Reason:

```java
NEAR_DEADLINE
```

---

## Eligibility

`eligible = false` only for hard constraints:

```text
- missing required skills
- exceeding max weekly hours
```

Other rules affect the score but do not block assignment.

---

## Score reasons

Current `ScoreReason` values:

```java
MATCHING_SKILLS
MISSING_SKILLS
SENIORITY_MATCH
SENIORITY_GAP
LOW_WORKLOAD
HIGH_WORKLOAD
CAPACITY_AVAILABLE
CAPACITY_EXCEEDED
NEAR_DEADLINE
CRITICAL_TASK
```

---

## Example

Task:

```text
priority = HIGH
estimatedHours = 6
requiredSkills = Java, Spring Boot
```

Employee:

```text
seniorityLevel = SENIOR
maxWeeklyHours = 40
currentWeeklyHours = 10
skills = Java, Spring Boot, Docker
```

Evaluation:

```text
+40 MATCHING_SKILLS
+5  MATCHING_SKILLS extra skill bonus
+15 SENIORITY_MATCH
+15 LOW_WORKLOAD
+20 CAPACITY_AVAILABLE
```

Result:

```text
eligible = true
score = 95
```

The exact score may change if scoring weights are updated.

---

## Tests

The algorithm is covered by unit tests in:

```text
src/test/java/com/smartops/planner/planning/ScoringServiceTest.java
```

Covered scenarios:

```text
- full skill match
- missing required skills
- extra skill bonus
- weekly capacity exceeded
- urgent task with non-senior employee
- workload comparison
- deadline pressure
- best candidate ranking
- expected positive reasons
- expected penalty reasons
```

---

## Current limitations

```text
- employee availability calendar is not modelled yet
- skill proficiency levels are not modelled yet
- weights are hardcoded
- extra skills are treated generically
- forced assignments are not supported yet
```