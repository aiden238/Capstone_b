package com.blackbox.activity.repository;

import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId);

    List<ActivityLog> findAllByProjectIdAndUserIdOrderByOccurredAtDesc(UUID projectId, UUID userId);

    // 중복 체크 (외부 연동 시 externalId 기반)
    boolean existsByProjectIdAndExternalId(UUID projectId, String externalId);

    // 타임라인: 페이지네이션 + 필터
    List<ActivityLog> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId, Pageable pageable);

    List<ActivityLog> findAllByProjectIdAndSourceOrderByOccurredAtDesc(
            UUID projectId, ActivitySource source, Pageable pageable);

    List<ActivityLog> findAllByProjectIdAndUserIdAndSourceOrderByOccurredAtDesc(
            UUID projectId, UUID userId, ActivitySource source, Pageable pageable);

    List<ActivityLog> findAllByProjectIdAndUserIdOrderByOccurredAtDesc(
            UUID projectId, UUID userId, Pageable pageable);

    // 특정 기간 활동 조회
    List<ActivityLog> findAllByProjectIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            UUID projectId, OffsetDateTime start, OffsetDateTime end);
}
