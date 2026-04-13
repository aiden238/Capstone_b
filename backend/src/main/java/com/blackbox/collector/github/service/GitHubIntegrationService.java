package com.blackbox.collector.github.service;

import com.blackbox.collector.github.dto.ConnectGithubRequest;
import com.blackbox.collector.github.dto.GithubIntegrationResponse;
import com.blackbox.collector.github.entity.ProjectGithubIntegration;
import com.blackbox.collector.github.repository.ProjectGithubIntegrationRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.project.security.ProjectAccessChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GitHubIntegrationService {

    private final ProjectGithubIntegrationRepository integrationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessChecker accessChecker;

    public GitHubIntegrationService(ProjectGithubIntegrationRepository integrationRepository,
                                    ProjectRepository projectRepository,
                                    ProjectAccessChecker accessChecker) {
        this.integrationRepository = integrationRepository;
        this.projectRepository = projectRepository;
        this.accessChecker = accessChecker;
    }

    /** 연동 정보 조회 */
    public GithubIntegrationResponse getIntegration(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        ProjectGithubIntegration integration = integrationRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GITHUB_INTEGRATION_NOT_FOUND));
        return GithubIntegrationResponse.from(integration);
    }

    /** GitHub 저장소 연동 (LEADER만). 이미 연동된 경우 덮어쓰기 */
    @Transactional
    public GithubIntegrationResponse connectRepo(UUID projectId, UUID userId, ConnectGithubRequest req) {
        accessChecker.checkLeader(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        // 기존 연동이 있으면 업데이트, 없으면 신규 생성
        ProjectGithubIntegration integration = integrationRepository.findByProjectId(projectId)
                .orElseGet(() -> new ProjectGithubIntegration(project, req.installationId(), req.repoFullName()));

        integration.setInstallationId(req.installationId());
        integration.setRepoFullName(req.repoFullName());
        integrationRepository.save(integration);
        return GithubIntegrationResponse.from(integration);
    }

    /** 연동 해제 (LEADER만) */
    @Transactional
    public void disconnectRepo(UUID projectId, UUID userId) {
        accessChecker.checkLeader(projectId, userId);
        if (!integrationRepository.existsByProjectId(projectId)) {
            throw new BusinessException(ErrorCode.GITHUB_INTEGRATION_NOT_FOUND);
        }
        integrationRepository.deleteByProjectId(projectId);
    }
}
