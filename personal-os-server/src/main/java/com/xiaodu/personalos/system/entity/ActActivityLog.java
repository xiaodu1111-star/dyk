package com.xiaodu.personalos.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 统一活动流实体（表 {@code act_activity_log}，Flyway V4）。
 *
 * <p>全系统时间轴：各域 Service 完成业务后旁路写入，不暴露 REST。</p>
 *
 * @author Kou
 */
@Data
@TableName("act_activity_log")
public class ActActivityLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id。 */
    private Long userId;

    /** 维度：work/learn/fit/finance/life。 */
    private String dimension;

    /** 业务类型：task_done/study/workout/expense/checkin。 */
    private String bizType;

    /** 标题。 */
    private String title;

    /** 内容详情。 */
    private String content;

    /** 时长（分钟）。 */
    private Integer durationMin;

    /** 关联数值（如金额）。 */
    private BigDecimal valueNum;

    /** 来源业务实体类型。 */
    private String refType;

    /** 来源业务实体 id。 */
    private Long refId;

    /** 事件发生时间（非创建时间）。 */
    private LocalDateTime occurredAt;

    /** 归属日（冗余，用于分组统计；由 PeriodUtil 统一折算）。 */
    private LocalDate activityDate;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
