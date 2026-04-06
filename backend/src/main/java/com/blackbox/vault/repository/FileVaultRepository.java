package com.blackbox.vault.repository;

import com.blackbox.vault.entity.FileVault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileVaultRepository extends JpaRepository<FileVault, UUID> {
    List<FileVault> findAllByProjectIdOrderByUploadedAtDesc(UUID projectId);
    List<FileVault> findAllByProjectIdAndFileNameOrderByVersionDesc(UUID projectId, String fileName);
    Optional<FileVault> findTopByProjectIdAndFileNameOrderByVersionDesc(UUID projectId, String fileName);
    Optional<FileVault> findByProjectIdAndFileHash(UUID projectId, String fileHash);
}
