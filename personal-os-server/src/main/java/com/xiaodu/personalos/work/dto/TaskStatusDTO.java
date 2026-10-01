package com.xiaodu.personalos.work.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 任务状态流转入参。
 *
 * @author Kou
 */
@Data
public class TaskStatusDTO {

    /** 目标状态：todo / doing / done / abandoned。 */
    @NotBlank(message = "目标状态不能为空")
    @Pattern(regexp = "^(todo|doing|done|abandoned)$", message = "状态取值非法")
    private String status;
}
