package com.blackbox.task.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UpdateTaskRequest {

    @Size(max = 255)
    private String title;

    private String description;

    private String priority;

    @Size(max = 30)
    private String tag;

    private LocalDate dueDate;
}
