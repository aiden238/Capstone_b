package com.blackbox.integration.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_integrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "provider", "external_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IntegrationProvider provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_name")
    private String externalName;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "webhook_expiry")
    private OffsetDateTime webhookExpiry;

    @Column(name = "installation_id")
    private Long installationId;

    @Column(name = "last_synced")
    private OffsetDateTime lastSynced;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 10)
    private SyncStatus syncStatus = SyncStatus.ACTIVE;

    @Column(name = "error_message")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ExternalIntegration(Project project, IntegrationProvider provider,
                                String externalId, String externalName, User createdBy) {
        this.project = project;
        this.provider = provider;
        this.externalId = externalId;
        this.externalName = externalName;
        this.createdBy = createdBy;
        this.syncStatus = SyncStatus.ACTIVE;
    }
}
