package com.xiaodu.personalos.sop.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * SOP 结束执行入参。
 *
 * <p>契约：{@code { finished: true, costMin?, stuckStep?, deviation? }}。
 * {@code costMin} 为空则由 started_at → finished_at 自动计算。</p>
 *
 * @author Alex
 */
@Data
public class SopRunFinishDTO {

    /** 是否结束本次执行；非 true 视为参数非法（10104）。 */
    private Boolean finished;

    /** 实际耗时（分钟），可选；为空时服务端自动计算。 */
    private Integer costMin;

    /** 卡在第几步，可选；为空表示无卡点。 */
    private Integer stuckStep;

    /** 执行中发现的问题 / 需要改进的点，可选。 */
    @Size(max = 500, message = "偏差说明最长 500 字")
    private String deviation;
}
