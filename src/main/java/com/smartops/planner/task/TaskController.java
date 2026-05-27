package com.smartops.planner.task;

import com.smartops.planner.task.dto.CreateTaskRequest;
import com.smartops.planner.task.dto.TaskResponse;
import com.smartops.planner.task.dto.UpdateTaskRequest;
import com.smartops.planner.task.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> findAll() {
        return taskService.findAll();
    }

    @GetMapping("/{id}")
    public TaskResponse findById(@PathVariable Long id) {
        return taskService.findById(id);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.create(request);
        return ResponseEntity
                .created(URI.create("/api/tasks/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public TaskResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return taskService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
