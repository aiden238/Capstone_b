package com.blackbox.score.repository;

import com.blackbox.score.entity.ContributionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContributionScoreRepository extends JpaRepository<ContributionScore, UUID> {

    List<ContributionScore> findAllByProjectId(UUID projectId);

    Optional<ContributionScore> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
