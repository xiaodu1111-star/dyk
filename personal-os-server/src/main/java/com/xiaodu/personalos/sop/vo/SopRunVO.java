package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 使用记录 VO（执行历史列表）。
 *
 * <p>契约：{@code { id, sopId, startedAt, finishedAt, costMin, stuckStep, deviation }}。</p>
 *
 * @author Alex
 */
@Data
public class SopRunVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 执行记录 id。 */
    private Long id;

    /** 所属 SOP id。 */
    private Long sopId;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间，null 表示进行中。 */
    private LocalDateTime finishedAt;

    /** 实际耗时（分钟）。 */
    private Integer costMin;

    /** 卡在第几步。 */
    private Integer stuckStep;

    /** 执行中发现的问题。 */
    private String deviation;
}
