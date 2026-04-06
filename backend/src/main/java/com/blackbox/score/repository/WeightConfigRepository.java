package com.blackbox.score.repository;

import com.blackbox.score.entity.WeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WeightConfigRepository extends JpaRepository<WeightConfig, UUID> {

    Optional<WeightConfig> findByProjectId(UUID projectId);
}
