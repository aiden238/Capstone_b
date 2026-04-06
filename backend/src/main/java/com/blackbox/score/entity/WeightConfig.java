package com.blackbox.score.entity;

import com.blackbox.project.entity.Project;
import com.blackbox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weight_configs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeightConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false)
    private User professor;

    @Column(name = "weight_git", precision = 3, scale = 2)
    private BigDecimal weightGit = new BigDecimal("0.30");

    @Column(name = "weight_doc", precision = 3, scale = 2)
    private BigDecimal weightDoc = new BigDecimal("0.25");

    @Column(name = "weight_meeting", precision = 3, scale = 2)
    private BigDecimal weightMeeting = new BigDecimal("0.20");

    @Column(name = "weight_task", precision = 3, scale = 2)
    private BigDecimal weightTask = new BigDecimal("0.25");

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public WeightConfig(Project project, User professor) {
        this.project = project;
        this.professor = professor;
    }
}
