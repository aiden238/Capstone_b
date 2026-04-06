package com.blackbox.meeting.entity;

import com.blackbox.auth.entity.User;
import com.blackbox.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "meetings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(length = 255)
    private String title;

    @Column(name = "meeting_date", nullable = false)
    private OffsetDateTime meetingDate;

    private String purpose;

    private String notes;

    private String decisions;

    @Column(name = "checkin_code", unique = true, length = 8)
    private String checkinCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingAttendee> attendees = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public Meeting(Project project, String title, OffsetDateTime meetingDate,
                   String purpose, String checkinCode, User createdBy) {
        this.project = project;
        this.title = title;
        this.meetingDate = meetingDate;
        this.purpose = purpose;
        this.checkinCode = checkinCode;
        this.createdBy = createdBy;
    }
}
