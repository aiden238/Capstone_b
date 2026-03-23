package com.blackbox.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다")
    @Size(max = 255, message = "프로젝트 이름은 255자 이내여야 합니다")
    private String name;

    private String description;
    private String courseName;
    private String semester;
    private LocalDate startDate;
    private LocalDate endDate;
}
