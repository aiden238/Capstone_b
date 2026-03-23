package com.blackbox.project.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateProjectRequest {

    private String name;
    private String description;
    private String courseName;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
}
