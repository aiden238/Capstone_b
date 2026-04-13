package com.blackbox.collector.drive.entity;

import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_drive_integrations")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectDriveIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(name = "drive_folder_id", nullable = false, length = 255)
    private String driveFolderId;

    @Column(name = "watch_channel_id", length = 255)
    private String watchChannelId;

    @Column(name = "watch_resource_id", length = 255)
    private String watchResourceId;

    @Column(name = "watch_expiry")
    private OffsetDateTime watchExpiry;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expiry")
    private OffsetDateTime tokenExpiry;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public ProjectDriveIntegration(Project project, String driveFolderId,
                                   String accessToken, String refreshToken,
                                   OffsetDateTime tokenExpiry) {
        this.project = project;
        this.driveFolderId = driveFolderId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiry = tokenExpiry;
    }
}
