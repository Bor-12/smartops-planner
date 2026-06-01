package com.smartops.planner.task;

import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
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
public class MyTasksController {

    private final MyTasksService myTasksService;

    public MyTasksController(MyTasksService myTasksService) {
        this.myTasksService = myTasksService;
    }

    @GetMapping
    public List<TaskResponse> findMyTasks(Principal principal) {
        return myTasksService.findMyTasks(principal.getName());
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateMyTaskStatus(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return myTasksService.updateMyTaskStatus(principal.getName(), id, request);
    }
}
