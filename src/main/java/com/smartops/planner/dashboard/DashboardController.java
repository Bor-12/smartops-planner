package com.smartops.planner.dashboard;

import com.smartops.planner.dashboard.dto.PlanningSummaryResponse;
import com.smartops.planner.dashboard.dto.TaskStatusSummaryResponse;
import com.smartops.planner.dashboard.dto.WorkloadResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/workload")
    public List<WorkloadResponse> getWorkload() {
        return dashboardService.getWorkload();
    }

    @GetMapping("/task-status")
    public List<TaskStatusSummaryResponse> getTaskStatusSummary() {
        return dashboardService.getTaskStatusSummary();
    }

    @GetMapping("/planning-summary")
    public PlanningSummaryResponse getPlanningSummary() {
        return dashboardService.getPlanningSummary();
    }
}
