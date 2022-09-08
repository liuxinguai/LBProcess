package com.luban.process.model;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

@Data
public class DispatchBody implements Serializable {

    @NotBlank(message = "任务ID不允许为空")
    private String taskId;

    @NotBlank(message = "转发人ID")
    private String dispatchUserId;

}
