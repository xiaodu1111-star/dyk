package com.xiaodu.personalos.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标定义实体（表 {@code sys_metric_def}，Flyway V3）。
 *
 * <p>新增追踪项只需插一条 def，不改表不改码。</p>
 *
 * @author Kou
 */
@Data
@TableName("sys_metric_def")
public class SysMetricDef implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标编码（唯一），如 weight / water_cup / mood。 */
    private String code;

    /** 展示名。 */
    private String name;

    /** 所属域：work/learn/fit/finance/life。 */
    private String dimension;

    /** 值类型：number/duration/enum/bool。 */
    private String valueType;

    /** 单位：kg / 分钟 / 杯。 */
    private String unit;

    /** enum 类型的可选值 JSON。 */
    private String optionsJson;

    /** 聚合方式：sum/avg/max/min/last。 */
    private String aggType;

    /** 目标值，用于进度条。 */
    private BigDecimal targetValue;

    /** 是否上首页：1 是 / 0 否。 */
    private Integer showOnHome;

    /** 排序号。 */
    private Integer sortNo;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记（0 正常，1 已删除）。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
