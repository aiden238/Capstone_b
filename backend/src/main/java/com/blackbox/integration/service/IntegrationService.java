package com.blackbox.integration.service;

import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.integration.dto.*;
import com.blackbox.integration.entity.*;
import com.blackbox.integration.repository.*;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IntegrationService {

    private final ExternalIntegrationRepository integrationRepository;
    private final ExternalAuthRepository authRepository;
    private final GitHubUserMappingRepository mappingRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public IntegrationService(ExternalIntegrationRepository integrationRepository,
                               ExternalAuthRepository authRepository,
                               GitHubUserMappingRepository mappingRepository,
                               ProjectRepository projectRepository,
                               UserRepository userRepository) {
        this.integrationRepository = integrationRepository;
        this.authRepository = authRepository;
        this.mappingRepository = mappingRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }

    /**
     * 프로젝트에 외부 서비스 연동 추가
     */
    @Transactional
    public IntegrationResponse createIntegration(UUID projectId, UUID userId, CreateIntegrationRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        IntegrationProvider provider = IntegrationProvider.valueOf(request.provider());

        // 중복 연동 체크
        integrationRepository.findByProjectIdAndProviderAndExternalId(projectId, provider, request.externalId())
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.INTEGRATION_ALREADY_EXISTS);
                });

        ExternalIntegration integration = new ExternalIntegration(
                project, provider, request.externalId(), request.externalName(), user);

        if (request.installationId() != null) {
            integration.setInstallationId(request.installationId());
        }

        integrationRepository.save(integration);
        return IntegrationResponse.from(integration);
    }

    /**
     * 프로젝트의 모든 연동 조회
     */
    @Transactional(readOnly = true)
    public List<IntegrationResponse> getIntegrations(UUID projectId) {
        return integrationRepository.findAllByProjectId(projectId).stream()
                .map(IntegrationResponse::from)
                .toList();
    }

    /**
     * 연동 삭제
     */
    @Transactional
    public void deleteIntegration(UUID projectId, UUID integrationId) {
        ExternalIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTEGRATION_NOT_FOUND));

        if (!integration.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        integrationRepository.delete(integration);
    }

    /**
     * 연동 상태 업데이트 (ACTIVE/PAUSED/ERROR)
     */
    @Transactional
    public IntegrationResponse updateSyncStatus(UUID projectId, UUID integrationId, String status) {
        ExternalIntegration integration = integrationRepository.findById(integrationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTEGRATION_NOT_FOUND));

        if (!integration.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        integration.setSyncStatus(SyncStatus.valueOf(status));
        integrationRepository.save(integration);
        return IntegrationResponse.from(integration);
    }

    // --- GitHub User Mapping ---

    @Transactional
    public GitHubUserMappingResponse createMapping(UUID projectId, GitHubUserMappingRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 중복 매핑 체크
        if (mappingRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new BusinessException(ErrorCode.MAPPING_ALREADY_EXISTS);
        }

        GitHubUserMapping mapping = new GitHubUserMapping(project, user, request.githubUsername());
        mappingRepository.save(mapping);
        return GitHubUserMappingResponse.from(mapping);
    }

    @Transactional(readOnly = true)
    public List<GitHubUserMappingResponse> getMappings(UUID projectId) {
        return mappingRepository.findAllByProjectId(projectId).stream()
                .map(GitHubUserMappingResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMapping(UUID projectId, UUID mappingId) {
        GitHubUserMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MAPPING_NOT_FOUND));

        if (!mapping.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        mappingRepository.delete(mapping);
    }
}
