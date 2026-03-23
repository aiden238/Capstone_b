package com.blackbox.project.dto;

import com.blackbox.project.entity.ProjectRole;
import jakarta.validation.constraints.NotNull;

public class UpdateMemberRoleRequest {

    @NotNull(message = "역할은 필수입니다")
    private ProjectRole role;

    public UpdateMemberRoleRequest() {}

    public ProjectRole getRole() { return role; }
    public void setRole(ProjectRole role) { this.role = role; }
}
