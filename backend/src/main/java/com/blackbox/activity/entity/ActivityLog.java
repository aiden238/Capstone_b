package com.blackbox.activity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivitySource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "trust_level", precision = 3, scale = 2)
    private BigDecimal trustLevel = new BigDecimal("1.00");

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt = OffsetDateTime.now();

    @Column(name = "quality_score", precision = 3, scale = 2)
    private BigDecimal qualityScore;

    @Column(name = "quality_reason")
    private String qualityReason;

    @Column(name = "analysis_method", length = 20)
    private String analysisMethod;

    public ActivityLog(UUID projectId, UUID userId, ActivitySource source,
                       ActionType actionType, String metadata) {
        this.projectId = projectId;
        this.userId = userId;
        this.source = source;
        this.actionType = actionType;
        this.metadata = metadata;
        this.occurredAt = OffsetDateTime.now();
        this.syncedAt = OffsetDateTime.now();
        this.trustLevel = new BigDecimal("1.00");
    }
}
