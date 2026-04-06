package com.blackbox.meeting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class CreateMeetingRequest {

    private String title;

    @NotNull(message = "회의 날짜는 필수입니다")
    private OffsetDateTime meetingDate;

    private String purpose;
}
