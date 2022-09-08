package com.luban.process.model;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;

@Data
public class CommitBody implements Serializable {

    @NotBlank(message = "提交人ID不允许为空")
    private String commitUserId;

    @NotBlank(message = "任务ID不允许为空")
    private String taskId;

    Map<String,Object> formData;

}
