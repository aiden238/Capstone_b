package com.blackbox.project.dto;

import com.blackbox.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String courseName;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
    private String inviteCode;
    private UUID createdBy;
    private OffsetDateTime createdAt;

    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getCourseName(),
                p.getSemester(),
                p.getStartDate(),
                p.getEndDate(),
                p.getInviteCode(),
                p.getCreatedBy().getId(),
                p.getCreatedAt()
        );
    }
}
