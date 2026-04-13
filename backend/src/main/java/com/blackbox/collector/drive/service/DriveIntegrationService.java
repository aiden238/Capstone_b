package com.blackbox.collector.drive.service;

import com.blackbox.collector.drive.dto.DriveIntegrationResponse;
import com.blackbox.collector.drive.entity.ProjectDriveIntegration;
import com.blackbox.collector.drive.repository.ProjectDriveIntegrationRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Google Drive 연동 서비스.
 * OAuth 2.0 코드 교환, watch 채널 관리, 연동 해제 담당.
 *
 * NOTE: 실제 Google API 호출(token exchange, watch channel 생성)은
 *       google-api-services-drive 라이브러리 추가 후 DriveApiClient에서 처리.
 *       현재는 플로우 골격만 구현. build.gradle에 아래 의존성 추가 필요:
 *       implementation 'com.google.apis:google-api-services-drive:v3-rev20231128-2.0.0'
 *       implementation 'com.google.oauth-client:google-oauth-client-jetty:1.34.1'
 */
@Service
@Transactional(readOnly = true)
public class DriveIntegrationService {

    private final ProjectDriveIntegrationRepository integrationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessChecker accessChecker;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.redirect-uri:http://localhost/api/oauth/google/callback}")
    private String redirectUri;

    public DriveIntegrationService(ProjectDriveIntegrationRepository integrationRepository,
                                   ProjectRepository projectRepository,
                                   ProjectAccessChecker accessChecker) {
        this.integrationRepository = integrationRepository;
        this.projectRepository = projectRepository;
        this.accessChecker = accessChecker;
    }

    /** 연동 상태 조회 */
    public DriveIntegrationResponse getIntegration(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        ProjectDriveIntegration integration = integrationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVE_INTEGRATION_NOT_FOUND));
        return DriveIntegrationResponse.from(integration);
    }

    /** OAuth 인증 URL 생성 (state에 projectId 포함) */
    public String buildAuthUrl(UUID projectId, UUID userId) {
        accessChecker.checkLeader(projectId, userId);
        return UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "https://www.googleapis.com/auth/drive.readonly " +
                        "https://www.googleapis.com/auth/drive.metadata.readonly")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", projectId.toString())
                .build()
                .toUriString();
    }

    /**
     * OAuth 콜백 처리 (코드 교환 + watch 채널 등록).
     * NOTE: 실제로는 Google API 클라이언트로 token exchange 후 watch channel 생성 필요.
     *       현재는 토큰을 저장하는 골격만 구현.
     */
    @Transactional
    public DriveIntegrationResponse handleOAuthCallback(String code, UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // TODO: Google OAuth token exchange:
        // TokenResponse tokenResponse = new GoogleAuthorizationCodeFlow.Builder(...)
        //         .build().newTokenRequest(code).setRedirectUri(redirectUri).execute();
        // String accessToken = tokenResponse.getAccessToken();
        // String refreshToken = tokenResponse.getRefreshToken();
        // OffsetDateTime tokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds());

        // TODO: Drive API로 watch channel 등록:
        // Channel channel = driveService.files().watch(driveFolderId, new Channel()
        //         .setId(UUID.randomUUID().toString())
        //         .setType("web_hook")
        //         .setAddress(webhookCallbackUrl)).execute();

        // 임시 플레이스 홀더 저장 (실제 구현 시 교체)
        ProjectDriveIntegration integration = integrationRepository.findByProjectId(projectId)
                .orElseGet(() -> new ProjectDriveIntegration(project, "TODO_FOLDER_ID",
                        "TODO_ACCESS_TOKEN", "TODO_REFRESH_TOKEN",
                        OffsetDateTime.now().plusHours(1)));
        integrationRepository.save(integration);
        return DriveIntegrationResponse.from(integration);
    }

    /** 연동 해제 (LEADER만) */
    @Transactional
    public void disconnectDrive(UUID projectId, UUID userId) {
        accessChecker.checkLeader(projectId, userId);
        if (!integrationRepository.existsByProjectId(projectId)) {
            throw new BusinessException(ErrorCode.DRIVE_INTEGRATION_NOT_FOUND);
        }
        // TODO: Google API로 watch 채널 stop 호출 후 삭제
        integrationRepository.deleteByProjectId(projectId);
    }

    /**
     * Watch 채널 자동 갱신 스케줄러 (23시간마다 실행).
     * 만료 1시간 이내 채널을 미리 갱신.
     */
    @Scheduled(fixedRate = 23 * 60 * 60 * 1000L)
    @Transactional
    public void renewExpiringWatchChannels() {
        OffsetDateTime threshold = OffsetDateTime.now().plusHours(1);
        integrationRepository.findAllByWatchExpiryBefore(threshold).forEach(integration -> {
            // TODO: Drive API watch 채널 갱신 (stop + re-watch)
            // 현재는 만료 시간만 연장하는 플레이스 홀더
            integration.setWatchExpiry(OffsetDateTime.now().plusHours(24));
            integrationRepository.save(integration);
        });
    }
}
