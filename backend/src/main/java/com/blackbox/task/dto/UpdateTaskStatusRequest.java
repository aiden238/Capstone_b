package com.blackbox.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateTaskStatusRequest {

    @NotBlank(message = "상태값은 필수입니다")
    private String status;  // TODO | IN_PROGRESS | DONE
}
