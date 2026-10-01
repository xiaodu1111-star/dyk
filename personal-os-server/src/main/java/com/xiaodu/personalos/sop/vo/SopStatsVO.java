package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SOP 执行统计 VO（详情页 stats）。
 *
 * <p>契约：{@code { runCount, avgMinutes, topStuckStep, lastUsedAt }}。</p>
 *
 * @author Alex
 */
@Data
public class SopStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 已完成执行次数。 */
    private Integer runCount;

    /** 平均耗时（分钟）。 */
    private Integer avgMinutes;

    /** 最常卡住的步骤序号（出现次数最多），无则 null。 */
    private Integer topStuckStep;

    /** 最近使用时间。 */
    private java.time.LocalDateTime lastUsedAt;
}
