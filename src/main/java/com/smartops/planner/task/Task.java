package com.smartops.planner.task;

import com.smartops.planner.employee.Employee;
import com.smartops.planner.skill.Skill;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskPriority priority;

    @Column(nullable = false)
    private Integer estimatedHours;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskStatus status = TaskStatus.TODO;

    @ManyToMany
    @JoinTable(
            name = "task_required_skills",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> requiredSkills = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public Task(String title, TaskPriority priority, Integer estimatedHours) {
        this.title = title;
        this.priority = priority;
        this.estimatedHours = estimatedHours;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Integer getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Integer estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Set<Skill> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(Set<Skill> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public Employee getAssignedEmployee() {
        return assignedEmployee;
    }

    public void setAssignedEmployee(Employee assignedEmployee) {
        this.assignedEmployee = assignedEmployee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
