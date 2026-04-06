package com.blackbox.task.controller;

import com.blackbox.auth.security.CustomUserDetails;
import com.blackbox.common.dto.ApiResponse;
import com.blackbox.task.dto.*;
import com.blackbox.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.create(projectId, user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String status) {
        List<TaskResponse> response = taskService.getTasks(projectId, user.getUserId(), status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails user) {
        TaskResponse response = taskService.getTask(projectId, taskId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.update(projectId, taskId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        TaskResponse response = taskService.updateStatus(projectId, taskId, user.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails user) {
        taskService.delete(projectId, taskId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{taskId}/assignees")
    public ResponseEntity<ApiResponse<TaskResponse>> addAssignee(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody TaskAssignRequest request) {
        TaskResponse response = taskService.addAssignee(projectId, taskId, user.getUserId(), request.getAssigneeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @DeleteMapping("/{taskId}/assignees/{assigneeId}")
    public ResponseEntity<ApiResponse<TaskResponse>> removeAssignee(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @PathVariable UUID assigneeId,
            @AuthenticationPrincipal CustomUserDetails user) {
        TaskResponse response = taskService.removeAssignee(projectId, taskId, user.getUserId(), assigneeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
