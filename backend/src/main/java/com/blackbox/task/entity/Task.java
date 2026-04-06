package com.blackbox.task.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(length = 30)
    private String tag;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToMany
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Task(Project project, String title, String description,
                TaskPriority priority, String tag, LocalDate dueDate, User createdBy) {
        this.project = project;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.TODO;
        this.priority = priority != null ? priority : TaskPriority.MEDIUM;
        this.tag = tag;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }
}
