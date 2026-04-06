package com.blackbox.meeting.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateMeetingRequest {
    private String title;
    private String purpose;
    private String notes;
    private String decisions;
}
