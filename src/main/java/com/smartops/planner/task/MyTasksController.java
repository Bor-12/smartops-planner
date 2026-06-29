package com.smartops.planner.task;

import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-tasks")
@Tag(name = "My Tasks", description = "Tasks assigned to the authenticated employee")
public class MyTasksController {

    private final MyTasksService myTasksService;

    public MyTasksController(MyTasksService myTasksService) {
        this.myTasksService = myTasksService;
    }

    @GetMapping
    @Operation(summary = "List my tasks")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assigned tasks returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    public List<TaskResponse> findMyTasks(Principal principal) {
        return myTasksService.findMyTasks(principal.getName());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update my task status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Insufficient role"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public TaskResponse updateMyTaskStatus(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return myTasksService.updateMyTaskStatus(principal.getName(), id, request);
    }
}
