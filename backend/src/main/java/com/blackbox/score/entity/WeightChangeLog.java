package com.blackbox.score.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weight_change_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeightChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_weights", columnDefinition = "jsonb", nullable = false)
    private String oldWeights;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_weights", columnDefinition = "jsonb", nullable = false)
    private String newWeights;

    @Column(name = "changed_at")
    private OffsetDateTime changedAt = OffsetDateTime.now();

    public WeightChangeLog(UUID projectId, UUID changedBy, String oldWeights, String newWeights) {
        this.projectId = projectId;
        this.changedBy = changedBy;
        this.oldWeights = oldWeights;
        this.newWeights = newWeights;
    }
}
