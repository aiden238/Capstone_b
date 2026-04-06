package com.blackbox.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CreateTaskRequest {

    @NotBlank(message = "태스크 제목은 필수입니다")
    @Size(max = 255)
    private String title;

    private String description;

    private String priority;  // LOW | MEDIUM | HIGH | URGENT

    @Size(max = 30)
    private String tag;

    private LocalDate dueDate;

    private List<UUID> assigneeIds;
}
