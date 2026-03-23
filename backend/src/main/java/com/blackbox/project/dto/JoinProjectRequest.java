package com.blackbox.project.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinProjectRequest {

    @NotBlank(message = "초대 코드는 필수입니다")
    private String inviteCode;

    public JoinProjectRequest() {}

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
