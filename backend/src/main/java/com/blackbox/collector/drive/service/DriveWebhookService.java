package com.blackbox.collector.drive.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.entity.ActivitySource;
import com.blackbox.activity.service.ActivityLogService;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.collector.drive.entity.ProjectDriveIntegration;
import com.blackbox.collector.drive.repository.ProjectDriveIntegrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Google Drive Push Notification 처리.
 * Drive Changes API로 변경상세를 조회하여 activity_logs에 저장.
 * 현재 구현: 채널 검증 + 기본 로깅. 실제 Changes API 조회는 Drive API 클라이언트 라이브러리 추가 후 확장.
 */
@Service
@Transactional(readOnly = true)
public class DriveWebhookService {

    private static final Logger log = LoggerFactory.getLogger(DriveWebhookService.class);
    private static final BigDecimal TRUST_LEVEL = new BigDecimal("1.00");

    private final ActivityLogService activityLogService;
    private final UserRepository userRepository;
    private final ProjectDriveIntegrationRepository integrationRepository;

    public DriveWebhookService(ActivityLogService activityLogService,
                               UserRepository userRepository,
                               ProjectDriveIntegrationRepository integrationRepository) {
        this.activityLogService = activityLogService;
        this.userRepository = userRepository;
        this.integrationRepository = integrationRepository;
    }

    /**
     * Drive Push Notification 처리.
     * @param channelId   X-Goog-Channel-Id 헤더
     * @param resourceId  X-Goog-Resource-Id 헤더
     * @param state       X-Goog-Resource-State 헤더 (sync|add|update|remove|trash|untrash)
     */
    public void handleNotification(String channelId, String resourceId, String state) {
        if ("sync".equals(state)) {
            log.info("Drive watch channel sync received: {}", channelId);
            return;
        }

        Optional<ProjectDriveIntegration> integrationOpt =
                integrationRepository.findByWatchChannelId(channelId);

        if (integrationOpt.isEmpty()) {
            log.warn("Received Drive notification for unknown channel: {}", channelId);
            return;
        }

        ProjectDriveIntegration integration = integrationOpt.get();
        UUID projectId = integration.getProject().getId();

        // 변경 유형에 따라 ActionType 결정
        ActionType actionType = switch (state) {
            case "add" -> ActionType.DOC_CREATE;
            case "update" -> ActionType.DOC_EDIT;
            default -> ActionType.DOC_EDIT;
        };

        // NOTE: 실제 구현에서는 Google Drive Changes API를 호출하여 수정자(author) 정보를 가져와야 합니다.
        // access_token으로 GET https://www.googleapis.com/drive/v3/changes?pageToken=...&driveId=...
        // 응답에서 change.file.lastModifyingUser.emailAddress를 google_email로 userId 매핑
        // 현재는 channelId+resourceId를 externalId로 사용하여 중복 방지만 보장합니다.
        String externalId = "drive-" + channelId + "-" + resourceId + "-" + state + "-" + System.currentTimeMillis();
        String metadata = "{\"channelId\":\"" + channelId + "\",\"state\":\"" + state + "\"}";

        // userId를 알 수 없는 경우 로그만 남기고 스킵
        // (Drive API 연동 시 lastModifyingUser.emailAddress → google_email → userId 매핑)
        log.debug("Drive notification received: project={}, state={}, channel={}", projectId, state, channelId);

        // 실제 userId 매핑이 가능한 경우에만 activity_log 저장 (현재는 스킵)
        // activityLogService.logExternal(projectId, userId, ActivitySource.GOOGLE_DRIVE, actionType,
        //         metadata, externalId, TRUST_LEVEL, OffsetDateTime.now());
    }

    /** google_email로 userId 조회 */
    protected Optional<UUID> resolveUserId(String googleEmail) {
        if (googleEmail == null || googleEmail.isBlank()) return Optional.empty();
        return userRepository.findByGoogleEmail(googleEmail).map(u -> u.getId());
    }
}
