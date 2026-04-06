package com.blackbox.vault.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "file_vault")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileVault {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "is_immutable")
    private Boolean isImmutable;

    @Column(name = "version", nullable = false)
    private Integer version;

    public FileVault(Project project, User uploader, String fileName,
                     String fileHash, Long fileSize, String storagePath, Integer version) {
        this.project = project;
        this.uploader = uploader;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.uploadedAt = OffsetDateTime.now();
        this.isImmutable = true;
        this.version = version;
    }
}
