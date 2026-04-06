package com.blackbox.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class TaskAssignRequest {

    @NotNull(message = "담당자 ID는 필수입니다")
    private UUID assigneeId;
}
