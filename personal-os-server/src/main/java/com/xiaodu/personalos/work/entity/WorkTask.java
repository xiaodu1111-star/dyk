package com.xiaodu.personalos.work.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务实体（表 {@code work_task}，Flyway V10）。
 *
 * <p>状态：todo / doing / done / abandoned；优先级：1 高 / 2 中 / 3 低。
 * 驼峰 → 下划线由 MyBatis-Plus 默认策略处理，无需逐个 {@code @TableField("xxx")}。</p>
 *
 * @author Kou
 */
@Data
@TableName("work_task")
public class WorkTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 id。 */
    private Long userId;

    /** 所属项目 id（P2 启用，本期不涉及）。 */
    private Long projectId;

    /** 父任务 id，0=顶层（P1 不实现 UI）。 */
    private Long parentId;

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

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）。
     *
     * <p>刻意使用 {@code INSERT_UPDATE}（与 M0 {@code SysTag} 的 {@code INSERT} 不同）：
     * 任务更新频繁，需 {@code MyMetaObjectHandler.updateFill} 对 update_time 生效。</p>
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0 正常，1 已删除）。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
