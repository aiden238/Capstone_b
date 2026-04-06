package com.blackbox.meeting.repository;

import com.blackbox.meeting.entity.MeetingAttendee;
import com.blackbox.meeting.entity.MeetingAttendeeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeetingAttendeeRepository extends JpaRepository<MeetingAttendee, MeetingAttendeeId> {

    Optional<MeetingAttendee> findByMeetingIdAndUserId(UUID meetingId, UUID userId);
}
