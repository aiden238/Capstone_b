package com.blackbox.alert.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId; // NULL이면 팀 전체 경보

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Alert(UUID projectId, UUID userId, AlertType alertType,
                 AlertSeverity severity, String message) {
        this.projectId = projectId;
        this.userId = userId;
        this.alertType = alertType;
        this.severity = severity;
        this.message = message;
    }
}
