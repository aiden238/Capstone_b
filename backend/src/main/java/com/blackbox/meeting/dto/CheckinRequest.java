package com.blackbox.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CheckinRequest {

    @NotBlank(message = "체크인 코드는 필수입니다")
    private String checkinCode;
}
