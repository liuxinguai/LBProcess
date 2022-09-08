package com.luban.process.model;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

@Data
public class ClaimBody implements Serializable {

    @NotBlank(message = "任务人ID")
    private String userId;

    @NotBlank(message = "任务ID")
    private String taskId;

}
