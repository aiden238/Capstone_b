package com.blackbox.collector.github.repository;

import com.blackbox.collector.github.entity.ProjectGithubIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectGithubIntegrationRepository extends JpaRepository<ProjectGithubIntegration, UUID> {

    Optional<ProjectGithubIntegration> findByProjectId(UUID projectId);

    Optional<ProjectGithubIntegration> findByRepoFullName(String repoFullName);

    boolean existsByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
