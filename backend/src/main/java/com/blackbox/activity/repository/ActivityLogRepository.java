package com.blackbox.activity.repository;

import com.blackbox.activity.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId);

    List<ActivityLog> findAllByProjectIdAndUserIdOrderByOccurredAtDesc(UUID projectId, UUID userId);

    Page<ActivityLog> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId, Pageable pageable);

    boolean existsByExternalId(String externalId);

    /**
     * AI 분석이 필요한 COMMIT 로그:
     * - quality_score IS NULL (아직 분석 안 됨)
     * - action_type = 'COMMIT'
     * - 해당 유저가 consent_ai_analysis = true 인 경우만
     */
    @Query("""
            SELECT a FROM ActivityLog a
            WHERE a.actionType = com.blackbox.activity.entity.ActionType.COMMIT
              AND a.qualityScore IS NULL
              AND EXISTS (
                  SELECT 1 FROM ProjectMember pm
                  WHERE pm.user.id = a.userId
                    AND pm.project.id = a.projectId
                    AND pm.consentAiAnalysis = true
              )
            ORDER BY a.occurredAt ASC
            """)
    List<ActivityLog> findUnanalyzedCommitsWithConsent(Pageable pageable);
}
