package com.blackbox.alert.repository;

import com.blackbox.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<Alert> findAllByProjectIdAndIsReadFalseOrderByCreatedAtDesc(UUID projectId);

    long countByProjectIdAndIsReadFalse(UUID projectId);
}
