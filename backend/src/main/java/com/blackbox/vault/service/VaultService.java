package com.blackbox.vault.service;

import com.blackbox.activity.entity.ActionType;
import com.blackbox.activity.service.ActivityLogService;
import com.blackbox.auth.entity.User;
import com.blackbox.auth.repository.UserRepository;
import com.blackbox.common.exception.BusinessException;
import com.blackbox.common.exception.ErrorCode;
import com.blackbox.project.entity.Project;
import com.blackbox.project.repository.ProjectRepository;
import com.blackbox.project.security.ProjectAccessChecker;
import com.blackbox.vault.dto.FileVaultResponse;
import com.blackbox.vault.entity.FileVault;
import com.blackbox.vault.entity.TamperDetectionLog;
import com.blackbox.vault.repository.FileVaultRepository;
import com.blackbox.vault.repository.TamperDetectionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class VaultService {

    private final FileVaultRepository vaultRepository;
    private final TamperDetectionLogRepository tamperRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectAccessChecker accessChecker;
    private final HashService hashService;
    private final FileStorageService storageService;
    private final ActivityLogService activityLogService;

    public VaultService(FileVaultRepository vaultRepository,
                        TamperDetectionLogRepository tamperRepository,
                        ProjectRepository projectRepository,
                        UserRepository userRepository,
                        ProjectAccessChecker accessChecker,
                        HashService hashService,
                        FileStorageService storageService,
                        ActivityLogService activityLogService) {
        this.vaultRepository = vaultRepository;
        this.tamperRepository = tamperRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.accessChecker = accessChecker;
        this.hashService = hashService;
        this.storageService = storageService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public FileVaultResponse upload(UUID projectId, UUID userId, MultipartFile file) {
        accessChecker.checkMemberOrAbove(projectId, userId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        try {
            byte[] fileBytes = file.getBytes();
            String hash = hashService.sha256(new ByteArrayInputStream(fileBytes));
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";

            // Check for duplicate hash in same project
            var existingByHash = vaultRepository.findByProjectIdAndFileHash(projectId, hash);
            if (existingByHash.isPresent()) {
                // Same file content already exists — return existing record
                return FileVaultResponse.from(existingByHash.get());
            }

            // Check for same filename — detect tamper or new version
            var latestVersion = vaultRepository.findTopByProjectIdAndFileNameOrderByVersionDesc(projectId, originalName);
            int nextVersion = 1;
            if (latestVersion.isPresent()) {
                FileVault prev = latestVersion.get();
                nextVersion = prev.getVersion() + 1;
                // Hash differs → tamper detection log
                if (!prev.getFileHash().equals(hash)) {
                    TamperDetectionLog tamperLog = new TamperDetectionLog(
                            prev, prev.getFileHash(), hash, "REUPLOAD"
                    );
                    tamperRepository.save(tamperLog);
                }
            }

            // Store file
            Path stored = storageService.store(projectId, hash, originalName, new ByteArrayInputStream(fileBytes));

            // Save vault record
            FileVault vault = new FileVault(
                    project, uploader, originalName, hash,
                    (long) fileBytes.length, stored.toString(), nextVersion
            );
            FileVault saved = vaultRepository.save(vault);

            activityLogService.log(projectId, userId, ActionType.FILE_UPLOAD,
                    "{\"vaultId\":\"" + saved.getId() + "\",\"fileName\":\"" + originalName + "\",\"hash\":\"" + hash + "\"}");

            return FileVaultResponse.from(saved);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류 발생", e);
        }
    }

    public List<FileVaultResponse> getFiles(UUID projectId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        return vaultRepository.findAllByProjectIdOrderByUploadedAtDesc(projectId).stream()
                .map(FileVaultResponse::from)
                .collect(Collectors.toList());
    }

    public FileVaultResponse getFile(UUID projectId, UUID vaultId, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        FileVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        if (!vault.getProject().getId().equals(projectId)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return FileVaultResponse.from(vault);
    }

    public List<FileVaultResponse> getFileHistory(UUID projectId, String fileName, UUID userId) {
        accessChecker.checkAnyMember(projectId, userId);
        return vaultRepository.findAllByProjectIdAndFileNameOrderByVersionDesc(projectId, fileName).stream()
                .map(FileVaultResponse::from)
                .collect(Collectors.toList());
    }

    public Path getFilePath(UUID vaultId) {
        FileVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return storageService.resolve(vault.getStoragePath());
    }

    public String getFileName(UUID vaultId) {
        FileVault vault = vaultRepository.findById(vaultId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        return vault.getFileName();
    }
}
