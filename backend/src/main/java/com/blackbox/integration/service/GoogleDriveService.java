package com.blackbox.integration.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivityLog;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.repository.ActivityLogRepository;
import com.blackbox.integration.entity.ExternalIntegration;
import com.blackbox.integration.entity.IntegrationProvider;
import com.blackbox.integration.entity.SyncStatus;
import com.blackbox.integration.repository.ExternalIntegrationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Google Drive Push Notification 처리 서비스.
 *
 * Drive Changes: watch API를 통해 파일 변경 이벤트를 수신하고,
 * revision history를 기반으로 활동 로그를 기록한다.
 */
@Service
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);

    private final ExternalIntegrationRepository integrationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ObjectMapper objectMapper;

    public GoogleDriveService(ExternalIntegrationRepository integrationRepository,
                               ActivityLogRepository activityLogRepository,
                               ObjectMapper objectMapper) {
        this.integrationRepository = integrationRepository;
        this.activityLogRepository = activityLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Google Drive Push Notification 처리.
     * Drive API의 Changes: watch로부터 전달되는 알림을 처리한다.
     *
     * @param channelId   Drive push channel ID (X-Goog-Channel-ID 헤더)
     * @param resourceId  변경된 리소스 ID (X-Goog-Resource-ID 헤더)
     * @param state       채널 상태 (X-Goog-Resource-State 헤더)
     */
    @Transactional
    public void handlePushNotification(String channelId, String resourceId, String state) {
        log.info("Google Drive push notification: channelId={}, resourceId={}, state={}", channelId, resourceId, state);

        if ("sync".equals(state)) {
            log.info("Drive push channel sync confirmation for channelId={}", channelId);
            return;
        }

        // channelId로 integration 찾기
        List<ExternalIntegration> integrations = integrationRepository
                .findAllByProviderAndSyncStatus(IntegrationProvider.GOOGLE_DRIVE, SyncStatus.ACTIVE);

        for (ExternalIntegration integration : integrations) {
            if (channelId.equals(integration.getWebhookId())) {
                log.info("Processing Drive change for project: {}", integration.getProject().getId());
                integration.setLastSynced(OffsetDateTime.now());
                integrationRepository.save(integration);
                break;
            }
        }
    }

    /**
     * Drive API 폴링 결과를 활동 로그로 변환.
     * 외부에서 Drive API를 호출한 후 결과를 이 메서드로 전달한다.
     */
    @Transactional
    public void processRevisionData(UUID projectId, UUID userId, JsonNode revisionData) {
        String fileId = revisionData.path("fileId").asText(null);
        String fileName = revisionData.path("fileName").asText("unknown");
        String action = revisionData.path("action").asText("edit");

        if (fileId == null) return;

        ActionType actionType = switch (action) {
            case "create" -> ActionType.DOC_CREATE;
            case "comment" -> ActionType.DOC_COMMENT;
            default -> ActionType.DOC_EDIT;
        };

        String externalId = "drive-" + fileId + "-" + revisionData.path("revisionId").asText(
                String.valueOf(System.currentTimeMillis()));

        if (activityLogRepository.existsByProjectIdAndExternalId(projectId, externalId)) {
            return;
        }

        String metadata = toJson(Map.of(
                "fileId", fileId,
                "fileName", truncate(fileName, 200),
                "action", action,
                "mimeType", revisionData.path("mimeType").asText(""),
                "modifiedTime", revisionData.path("modifiedTime").asText("")
        ));

        ActivityLog activityLog = new ActivityLog(projectId, userId, ActivitySource.GOOGLE_DRIVE, actionType, metadata);
        activityLog.setExternalId(externalId);

        String modifiedTime = revisionData.path("modifiedTime").asText(null);
        if (modifiedTime != null && !modifiedTime.isEmpty()) {
            try {
                activityLog.setOccurredAt(OffsetDateTime.parse(modifiedTime));
            } catch (Exception ignored) {}
        }

        activityLogRepository.save(activityLog);
    }

    /**
     * 댓글 활동 기록
     */
    @Transactional
    public void processCommentData(UUID projectId, UUID userId, String fileId, String fileName, String commentId) {
        String externalId = "drive-comment-" + commentId;
        if (activityLogRepository.existsByProjectIdAndExternalId(projectId, externalId)) {
            return;
        }

        String metadata = toJson(Map.of(
                "fileId", fileId,
                "fileName", truncate(fileName, 200),
                "commentId", commentId,
                "action", "comment"
        ));

        ActivityLog activityLog = new ActivityLog(projectId, userId, ActivitySource.GOOGLE_DRIVE, ActionType.DOC_COMMENT, metadata);
        activityLog.setExternalId(externalId);
        activityLogRepository.save(activityLog);
    }

    private String truncate(String str, int maxLen) {
        return str != null && str.length() > maxLen ? str.substring(0, maxLen) : str;
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
