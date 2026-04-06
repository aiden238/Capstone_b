package com.blackbox.vault.repository;

import com.blackbox.vault.entity.TamperDetectionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TamperDetectionLogRepository extends JpaRepository<TamperDetectionLog, UUID> {
}
