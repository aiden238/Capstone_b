package com.blackbox.meeting.dto;

import com.blackbox.meeting.entity.Meeting;
import com.blackbox.meeting.entity.MeetingAttendee;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class MeetingResponse {
    private UUID id;
    private UUID projectId;
    private String title;
    private OffsetDateTime meetingDate;
    private String purpose;
    private String notes;
    private String decisions;
    private String checkinCode;
    private UUID createdBy;
    private List<AttendeeInfo> attendees;
    private OffsetDateTime createdAt;

    @Getter
    @AllArgsConstructor
    public static class AttendeeInfo {
        private UUID userId;
        private String name;
        private String email;
        private boolean checkedIn;
        private OffsetDateTime checkedAt;
    }

    public static MeetingResponse from(Meeting meeting) {
        List<AttendeeInfo> attendeeInfos = meeting.getAttendees().stream()
                .map(a -> new AttendeeInfo(
                        a.getUser().getId(),
                        a.getUser().getName(),
                        a.getUser().getEmail(),
                        Boolean.TRUE.equals(a.getCheckedIn()),
                        a.getCheckedAt()
                ))
                .collect(Collectors.toList());

        return new MeetingResponse(
                meeting.getId(),
                meeting.getProject().getId(),
                meeting.getTitle(),
                meeting.getMeetingDate(),
                meeting.getPurpose(),
                meeting.getNotes(),
                meeting.getDecisions(),
                meeting.getCheckinCode(),
                meeting.getCreatedBy().getId(),
                attendeeInfos,
                meeting.getCreatedAt()
        );
    }
}
