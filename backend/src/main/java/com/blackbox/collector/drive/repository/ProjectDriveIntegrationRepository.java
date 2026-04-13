package com.blackbox.collector.drive.repository;

import com.blackbox.collector.drive.entity.ProjectDriveIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectDriveIntegrationRepository extends JpaRepository<ProjectDriveIntegration, UUID> {

    Optional<ProjectDriveIntegration> findByProjectId(UUID projectId);

    Optional<ProjectDriveIntegration> findByWatchChannelId(String watchChannelId);

    boolean existsByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);

    /** watch 만료 임박 채널 목록 (현재시간+1시간 이전) — 갱신 스케줄러용 */
    List<ProjectDriveIntegration> findAllByWatchExpiryBefore(OffsetDateTime threshold);
}
