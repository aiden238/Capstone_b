package com.blackbox.score.repository;

import com.blackbox.score.entity.WeightChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeightChangeLogRepository extends JpaRepository<WeightChangeLog, UUID> {

    List<WeightChangeLog> findAllByProjectIdOrderByChangedAtDesc(UUID projectId);
}
