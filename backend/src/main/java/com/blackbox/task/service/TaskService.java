package com.blackbox.task.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.service.ActivityLogService;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.project.security.ProjectAccessChecker;
import com.blackbox.task.dto.*;
import com.blackbox.task.entity.Task;
import com.blackbox.task.entity.TaskPriority;
import com.blackbox.task.entity.TaskStatus;
import com.blackbox.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessChecker accessChecker;
    private final ActivityLogService activityLogService;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       ProjectAccessChecker accessChecker,
                       ActivityLogService activityLogService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public TaskResponse create(UUID projectId, UUID userId, CreateTaskRequest request) {
        accessChecker.checkMemberOrAbove(projectId, userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        TaskPriority priority = TaskPriority.MEDIUM;
        if (request.getPriority() != null) {
            try {
                priority = TaskPriority.valueOf(request.getPriority().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Task task = new Task(project, request.getTitle(), request.getDescription(),
                priority, request.getTag(), request.getDueDate(), creator);

        // 담당자 배정
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            for (UUID assigneeId : request.getAssigneeIds()) {
                accessChecker.checkAnyMember(projectId, assigneeId);
                User assignee = userRepository.findById(assigneeId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
                task.getAssignees().add(assignee);
            }
        }

        Task saved = taskRepository.save(task);

        activityLogService.log(projectId, userId, ActionType.TASK_CREATE,
                "{\"taskId\":\"" + saved.getId() + "\",\"title\":\"" + saved.getTitle() + "\"}");

        return TaskResponse.from(saved);
    }

    public List<TaskResponse> getTasks(UUID projectId, UUID userId, String status) {
        accessChecker.checkAnyMember(projectId, userId);

        List<Task> tasks;
        if (status != null && !status.isEmpty()) {
            try {
                TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
                tasks = taskRepository.findAllByProjectIdAndStatusOrderByCreatedAtDesc(projectId, taskStatus);
            } catch (IllegalArgumentException e) {
                tasks = taskRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
            }
        } else {
            tasks = taskRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId);
        }

        return tasks.stream().map(TaskResponse::from).collect(Collectors.toList());
    }

    public TaskResponse getTask(UUID projectId, UUID taskId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        Task task = findTaskInProject(projectId, taskId);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(UUID projectId, UUID taskId, UUID userId, UpdateTaskRequest request) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Task task = findTaskInProject(projectId, taskId);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getTag() != null) task.setTag(request.getTag());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getPriority() != null) {
            try {
                task.setPriority(TaskPriority.valueOf(request.getPriority().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        activityLogService.log(projectId, userId, ActionType.TASK_UPDATE,
                "{\"taskId\":\"" + task.getId() + "\",\"title\":\"" + task.getTitle() + "\"}");

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateStatus(UUID projectId, UUID taskId, UUID userId, UpdateTaskStatusRequest request) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Task task = findTaskInProject(projectId, taskId);

        TaskStatus oldStatus = task.getStatus();
        TaskStatus newStatus;
        try {
            newStatus = TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        task.setStatus(newStatus);

        if (newStatus == TaskStatus.DONE && oldStatus != TaskStatus.DONE) {
            task.setCompletedAt(OffsetDateTime.now());
            activityLogService.log(projectId, userId, ActionType.TASK_COMPLETE,
                    "{\"taskId\":\"" + task.getId() + "\",\"title\":\"" + task.getTitle() + "\"}");
        } else if (newStatus != TaskStatus.DONE) {
            task.setCompletedAt(null);
            activityLogService.log(projectId, userId, ActionType.TASK_STATUS_CHANGE,
                    "{\"taskId\":\"" + task.getId() + "\",\"from\":\"" + oldStatus + "\",\"to\":\"" + newStatus + "\"}");
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(UUID projectId, UUID taskId, UUID userId) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Task task = findTaskInProject(projectId, taskId);

        activityLogService.log(projectId, userId, ActionType.TASK_DELETE,
                "{\"taskId\":\"" + task.getId() + "\",\"title\":\"" + task.getTitle() + "\"}");

        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse addAssignee(UUID projectId, UUID taskId, UUID userId, UUID assigneeId) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        accessChecker.checkAnyMember(projectId, assigneeId);

        Task task = findTaskInProject(projectId, taskId);
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        task.getAssignees().add(assignee);

        activityLogService.log(projectId, userId, ActionType.TASK_ASSIGN,
                "{\"taskId\":\"" + task.getId() + "\",\"assigneeId\":\"" + assigneeId + "\"}");

        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse removeAssignee(UUID projectId, UUID taskId, UUID userId, UUID assigneeId) {
        accessChecker.checkMemberOrAbove(projectId, userId);
        Task task = findTaskInProject(projectId, taskId);

        task.getAssignees().removeIf(u -> u.getId().equals(assigneeId));

        activityLogService.log(projectId, userId, ActionType.TASK_UNASSIGN,
                "{\"taskId\":\"" + task.getId() + "\",\"assigneeId\":\"" + assigneeId + "\"}");

        return TaskResponse.from(task);
    }

    private Task findTaskInProject(UUID projectId, UUID taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));
        if (!task.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        }
        return task;
    }
}
