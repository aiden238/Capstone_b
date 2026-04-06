package com.blackbox.activity.repository;

import com.blackbox.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId);

    List<ActivityLog> findAllByProjectIdAndUserIdOrderByOccurredAtDesc(UUID projectId, UUID userId);
}
