package com.blackbox.activity.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.project.repository.ProjectMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final ProjectMemberRepository memberRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              ProjectMemberRepository memberRepository) {
        this.activityLogRepository = activityLogRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * 플랫폼 내부 활동 기록 (source = PLATFORM, trust_level = 1.00).
     * INV-07: consent_platform=false인 멤버는 기록하지 않음.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID projectId, UUID userId, ActionType actionType, String metadata) {
        boolean consented = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(m -> Boolean.TRUE.equals(m.getConsentPlatform()))
                .orElse(false);
        if (!consented) return;

        ActivityLog log = new ActivityLog(projectId, userId, ActivitySource.PLATFORM, actionType, metadata);
        activityLogRepository.save(log);
    }

    /**
     * 외부 연동 활동 기록 (GitHub / Google Drive).
     * INV-07: consent_platform=false인 멤버는 기록하지 않음.
     * 멱등성: externalId 중복 시 조용히 리턴.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logExternal(UUID projectId, UUID userId, ActivitySource source,
                            ActionType actionType, String metadata,
                            String externalId, BigDecimal trustLevel,
                            OffsetDateTime occurredAt) {
        boolean consented = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(m -> Boolean.TRUE.equals(m.getConsentPlatform()))
                .orElse(false);
        if (!consented) return;

        if (externalId != null && activityLogRepository.existsByExternalId(externalId)) {
            return;
        }
        ActivityLog log = new ActivityLog(projectId, userId, source, actionType, metadata);
        log.setExternalId(externalId);
        log.setTrustLevel(trustLevel != null ? trustLevel : new BigDecimal("1.00"));
        log.setOccurredAt(occurredAt != null ? occurredAt : OffsetDateTime.now());
        activityLogRepository.save(log);
    }
}
