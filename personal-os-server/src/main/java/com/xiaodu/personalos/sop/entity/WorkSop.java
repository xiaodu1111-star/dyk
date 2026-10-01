package com.xiaodu.personalos.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 主表实体（表 {@code work_sop}，Flyway V15）。
 *
 * <p>逻辑删除字段 {@code deleted} 由全局配置生效。</p>
 *
 * @author Alex
 */
@Data
@TableName("work_sop")
public class WorkSop implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属用户 id。 */
    private Long userId;

    /** 标题。 */
    private String title;

    /** 分类，如 需求评审/故障处理/周报。 */
    private String category;

    /** 什么场景下用这个 SOP。 */
    private String triggerScene;

    /** 产出什么结果。 */
    private String goal;

    /** 版本号，形如 v1.0。 */
    private String version;

    /** 被复用次数。 */
    private Integer useCount;

    /** 平均耗时（分钟）。 */
    private Integer avgMinutes;

    /** 最近一次使用时间。 */
    private LocalDateTime lastUsedAt;

    /** 状态：draft/active/deprecated。 */
    private String status;

    /** 由哪个任务沉淀而来。 */
    private Long sourceTaskId;

    /** 是否置顶：0 否 / 1 是。 */
    private Integer pinned;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /** 更新时间。 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0 正常 / 1 已删。 */
    @TableLogic
    private Integer deleted;
}
