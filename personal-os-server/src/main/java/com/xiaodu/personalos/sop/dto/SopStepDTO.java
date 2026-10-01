package com.xiaodu.personalos.sop.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SOP 步骤入参。
 *
 * <p>步骤标题的「空」由业务层判定并返回 10102（{@code STEP_INVALID}），
 * 而非框架 {@code @NotBlank} 的 400 —— 对齐任务书 §4 错误码语义。</p>
 *
 * @author Alex
 */
@Data
public class SopStepDTO {

    /** 步骤标题（必填，空值由服务层返回 10102）。 */
    @Size(max = 200, message = "步骤标题最长 200 字")
    private String title;

    /** 操作说明 / Markdown。 */
    private String detail;

    /** 避坑提示。 */
    @Size(max = 500, message = "避坑提示最长 500 字")
    private String tip;

    /** 预估分钟。 */
    private Integer estimateMin;
}
