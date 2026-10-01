package com.xiaodu.personalos.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 使用记录实体（表 {@code work_sop_log}，Flyway V16）。
 *
 * <p>每次「开始执行」插一条，结束执行时回写 finished_at/cost_min/stuck_step/deviation。</p>
 *
 * @author Alex
 */
@Data
@TableName("work_sop_log")
public class WorkSopLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 SOP id。 */
    private Long sopId;

    /** 开始执行时间。 */
    private LocalDateTime startedAt;

    /** 结束执行时间，null 表示进行中。 */
    private LocalDateTime finishedAt;

    /** 实际耗时（分钟）。 */
    private Integer costMin;

    /** 卡在第几步。 */
    private Integer stuckStep;

    /** 执行中发现的问题 / 需要改进的点。 */
    private String deviation;
}
