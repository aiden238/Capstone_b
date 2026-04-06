package com.blackbox.score.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contribution_scores", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContributionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "git_score", precision = 5, scale = 2)
    private BigDecimal gitScore = BigDecimal.ZERO;

    @Column(name = "doc_score", precision = 5, scale = 2)
    private BigDecimal docScore = BigDecimal.ZERO;

    @Column(name = "meeting_score", precision = 5, scale = 2)
    private BigDecimal meetingScore = BigDecimal.ZERO;

    @Column(name = "task_score", precision = 5, scale = 2)
    private BigDecimal taskScore = BigDecimal.ZERO;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(name = "weight_git", precision = 3, scale = 2)
    private BigDecimal weightGit = new BigDecimal("0.30");

    @Column(name = "weight_doc", precision = 3, scale = 2)
    private BigDecimal weightDoc = new BigDecimal("0.25");

    @Column(name = "weight_meeting", precision = 3, scale = 2)
    private BigDecimal weightMeeting = new BigDecimal("0.20");

    @Column(name = "weight_task", precision = 3, scale = 2)
    private BigDecimal weightTask = new BigDecimal("0.25");

    @Column(name = "calculated_at")
    private OffsetDateTime calculatedAt;

    public ContributionScore(Project project, User user) {
        this.project = project;
        this.user = user;
        this.calculatedAt = OffsetDateTime.now();
    }
}
