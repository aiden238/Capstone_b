package com.blackbox.task.repository;

import com.blackbox.task.entity.Task;
import com.blackbox.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Task> findAllByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, TaskStatus status);

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE t.project.id = :projectId AND a.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findAllByProjectIdAndAssigneeUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
