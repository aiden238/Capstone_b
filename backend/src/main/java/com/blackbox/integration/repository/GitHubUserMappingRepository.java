package com.blackbox.integration.repository;

import com.blackbox.integration.entity.GitHubUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GitHubUserMappingRepository extends JpaRepository<GitHubUserMapping, UUID> {

    Optional<GitHubUserMapping> findByProjectIdAndGithubUsername(UUID projectId, String githubUsername);

    Optional<GitHubUserMapping> findByProjectIdAndUserId(UUID projectId, UUID userId);

    List<GitHubUserMapping> findAllByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
