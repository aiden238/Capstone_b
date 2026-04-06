package com.blackbox.meeting.repository;

import com.blackbox.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findAllByProjectIdOrderByMeetingDateDesc(UUID projectId);

    Optional<Meeting> findByCheckinCode(String checkinCode);
}
