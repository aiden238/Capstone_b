package com.blackbox.vault.dto;

import com.blackbox.vault.entity.FileVault;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FileVaultResponse {
    private UUID id;
    private UUID projectId;
    private String fileName;
    private String fileHash;
    private Long fileSize;
    private Integer version;
    private UUID uploaderId;
    private String uploaderName;
    private OffsetDateTime uploadedAt;

    public static FileVaultResponse from(FileVault vault) {
        return new FileVaultResponse(
                vault.getId(),
                vault.getProject().getId(),
                vault.getFileName(),
                vault.getFileHash(),
                vault.getFileSize(),
                vault.getVersion(),
                vault.getUploader().getId(),
                vault.getUploader().getName(),
                vault.getUploadedAt()
        );
    }
}
