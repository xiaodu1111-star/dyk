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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指标记录实体（表 {@code sys_metric_record}，Flyway V3）。
 *
 * @author Kou
 */
@Data
@TableName("sys_metric_record")
public class SysMetricRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 指标定义 id。 */
    private Long metricId;

    /** 记录归属日（口径见 PeriodUtil）。 */
    private LocalDate recordDate;

    /** 记录写入时刻。 */
    private LocalDateTime recordTime;

    /** 数值（number/duration 类型）。 */
    private BigDecimal valueNum;

    /** 文本值（enum/bool 类型）。 */
    private String valueText;

    /** 来源业务实体类型（可选）。 */
    private String refType;

    /** 来源业务实体 id（可选）。 */
    private Long refId;

    /** 备注。 */
    private String remark;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记（0 正常，1 已删除）。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
