package com.xiaodu.personalos.work.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务出参 VO。
 *
 * <p>{@code overdue} 由 Service 用 {@code PeriodUtil} 判定后写入，前端不自算。</p>
 *
 * @author Kou
 */
@Data
public class TaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    private Long id;

    /** 归属用户 id。 */
    private Long userId;

    /** 任务标题。 */
    private String title;

    /** 任务描述。 */
    private String description;

    /** 状态：todo / doing / done / abandoned。 */
    private String status;

    /** 优先级：1 高 / 2 中 / 3 低。 */
    private Integer priority;

    /** 计划开始时间。 */
    private LocalDateTime planStart;

    /** 截止时间。 */
    private LocalDateTime dueAt;

    /** 预估工时（分钟）。 */
    private Integer estimateMin;

    /** 实际工时（分钟）。 */
    private Integer actualMin;

    /** 完成时刻。 */
    private LocalDateTime doneAt;

    /** 排序号。 */
    private Integer sortNo;

    /** 是否逾期（未完成且 dueAt < 今日 00:00）。 */
    private Boolean overdue = Boolean.FALSE;

    /** 关联标签。 */
    private List<TaskTagVO> tags = new ArrayList<>();

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;
}
