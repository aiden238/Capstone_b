package com.blackbox.integration.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "github_user_mappings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"project_id", "user_id"}),
        @UniqueConstraint(columnNames = {"project_id", "github_username"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GitHubUserMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_username", nullable = false, length = 100)
    private String githubUsername;

    @Column(name = "github_id")
    private Long githubId;

    @Column(name = "mapped_at")
    private OffsetDateTime mappedAt = OffsetDateTime.now();

    public GitHubUserMapping(Project project, User user, String githubUsername) {
        this.project = project;
        this.user = user;
        this.githubUsername = githubUsername;
        this.mappedAt = OffsetDateTime.now();
    }
}
