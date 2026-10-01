package com.xiaodu.personalos.work.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 新建任务入参（只有 title 必填，对齐「15 秒新增」硬指标）。
 *
 * @author Kou
 */
@Data
public class TaskCreateDTO {

    /** 任务标题（必填）。 */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 200, message = "任务标题最长 200 字")
    private String title;

    /** 描述。 */
    @Size(max = 5000, message = "描述最长 5000 字")
    private String description;

    /** 优先级：1 高 / 2 中 / 3 低，默认 2。 */
    @Min(value = 1, message = "优先级取值为 1/2/3")
    @Max(value = 3, message = "优先级取值为 1/2/3")
    private Integer priority = 2;

    /** 截止时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueAt;

    /** 预估工时（分钟）。 */
    @Min(value = 0, message = "预估工时不能为负")
    @Max(value = 100000, message = "预估工时过大")
    private Integer estimateMin;

    /** 标签 id 列表。 */
    private List<Long> tagIds;
}
