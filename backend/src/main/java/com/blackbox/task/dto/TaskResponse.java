package com.blackbox.task.dto;

import com.blackbox.task.entity.Task;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class TaskResponse {
    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String tag;
    private LocalDate dueDate;
    private OffsetDateTime completedAt;
    private UUID createdBy;
    private List<AssigneeInfo> assignees;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Getter
    @AllArgsConstructor
    public static class AssigneeInfo {
        private UUID userId;
        private String name;
        private String email;
    }

    public static TaskResponse from(Task task) {
        List<AssigneeInfo> assigneeInfos = task.getAssignees().stream()
                .map(u -> new AssigneeInfo(u.getId(), u.getName(), u.getEmail()))
                .collect(Collectors.toList());

        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority() != null ? task.getPriority().name() : null,
                task.getTag(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedBy().getId(),
                assigneeInfos,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
