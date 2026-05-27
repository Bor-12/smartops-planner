package com.smartops.planner.planning;

import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planning")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/run")
    public ResponseEntity<PlanningRunResponse> runPlanning() {
        return ResponseEntity.ok(planningService.runPlanning());
    }

    @GetMapping("/runs")
    public List<PlanningRunResponse> findAllRuns() {
        return planningService.findAllRuns();
    }

    @GetMapping("/runs/{id}")
    public PlanningRunResponse findRunById(@PathVariable Long id) {
        return planningService.findRunById(id);
    }

    @GetMapping("/assignments")
    public List<AssignmentResponse> findAllAssignments() {
        return planningService.findAllAssignments();
    }

    @GetMapping("/assignments/{id}")
    public AssignmentResponse findAssignmentById(@PathVariable Long id) {
        return planningService.findAssignmentById(id);
    }
}
