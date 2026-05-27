package com.smartops.planner.dashboard;

import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.dashboard.dto.TaskStatusSummaryResponse;
import com.smartops.planner.dashboard.dto.WorkloadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Operational dashboards and summaries")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/workload")
    @Operation(summary = "Get employee workload")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workload returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<WorkloadResponse> getWorkload() {
        return dashboardService.getWorkload();
    }

    @GetMapping("/task-status")
    @Operation(summary = "Get task status summary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task status summary returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<TaskStatusSummaryResponse> getTaskStatusSummary() {
        return dashboardService.getTaskStatusSummary();
    }

    @GetMapping("/planning-summary")
    @Operation(summary = "Get planning summary")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planning summary returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public PlanningSummaryResponse getPlanningSummary() {
        return dashboardService.getPlanningSummary();
    }
}
