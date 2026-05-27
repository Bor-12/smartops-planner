package com.smartops.planner.planning;

import com.smartops.planner.planning.dto.AssignmentResponse;
import com.smartops.planner.planning.dto.PlanningRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planning")
@Tag(name = "Planning", description = "Planning runs and assignment results")
public class PlanningController {

    private final PlanningService planningService;

    public PlanningController(PlanningService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/run")
    @Operation(summary = "Run planning")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning run completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public ResponseEntity<PlanningRunResponse> runPlanning() {
        return ResponseEntity.ok(planningService.runPlanning());
    }

    @GetMapping("/runs")
    @Operation(summary = "List planning runs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning runs returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<PlanningRunResponse> findAllRuns() {
        return planningService.findAllRuns();
    }

    @GetMapping("/runs/{id}")
    @Operation(summary = "Find planning run by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning run found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Planning run not found")
    })
    public PlanningRunResponse findRunById(@PathVariable Long id) {
        return planningService.findRunById(id);
    }

    @GetMapping("/assignments")
    @Operation(summary = "List assignments")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignments returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<AssignmentResponse> findAllAssignments() {
        return planningService.findAllAssignments();
    }

    @GetMapping("/assignments/{id}")
    @Operation(summary = "Find assignment by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment found"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Assignment not found")
    })
    public AssignmentResponse findAssignmentById(@PathVariable Long id) {
        return planningService.findAssignmentById(id);
    }
}
