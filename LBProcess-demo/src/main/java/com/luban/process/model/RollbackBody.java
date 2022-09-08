package com.luban.process.model;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;
import lombok.Data;

@Data
public class RollbackBody implements Serializable {

    /**当前任务ID*/
    @NotBlank(message = "当前任务ID不允许为空")
    private String currentTaskId;

    /**当前用户ID*/
    @NotBlank(message = "当前用户ID不允许为空")
    private String currentUserId;

    /**回退任务ID不允许为空*/
    @NotBlank(message = "回退任务ID不允许为空")
    private String rollbackTaskId;

}
