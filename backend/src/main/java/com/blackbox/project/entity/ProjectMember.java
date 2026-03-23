package com.blackbox.project.entity;

import com.blackbox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_members", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role = ProjectRole.MEMBER;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    // 데이터 수집 동의 필드
    @Column(name = "consent_platform")
    private Boolean consentPlatform = false;

    @Column(name = "consent_github")
    private Boolean consentGithub = false;

    @Column(name = "consent_drive")
    private Boolean consentDrive = false;

    @Column(name = "consent_ai_analysis")
    private Boolean consentAiAnalysis = false;

    @Column(name = "consented_at")
    private OffsetDateTime consentedAt;

    public ProjectMember(Project project, User user, ProjectRole role) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.joinedAt = OffsetDateTime.now();
    }
}
