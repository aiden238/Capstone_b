package com.blackbox.meeting.entity;

import com.blackbox.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "meeting_attendees")
@IdClass(MeetingAttendeeId.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingAttendee {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checked_in")
    private Boolean checkedIn = false;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    public MeetingAttendee(Meeting meeting, User user) {
        this.meeting = meeting;
        this.user = user;
        this.checkedIn = false;
    }
}
