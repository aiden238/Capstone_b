package com.blackbox.vault.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tamper_detection_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TamperDetectionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_id", nullable = false)
    private FileVault vault;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "original_hash", nullable = false, length = 64)
    private String originalHash;

    @Column(name = "new_hash", nullable = false, length = 64)
    private String newHash;

    @Column(name = "detector_type", nullable = false, length = 20)
    private String detectorType;

    @Column(name = "status", length = 20)
    private String status;

    public TamperDetectionLog(FileVault vault, String originalHash, String newHash, String detectorType) {
        this.vault = vault;
        this.detectedAt = OffsetDateTime.now();
        this.originalHash = originalHash;
        this.newHash = newHash;
        this.detectorType = detectorType;
        this.status = "FLAGGED";
    }
}
