package com.xiaodu.personalos.system.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 指标定义 VO。
 *
 * @author Kou
 */
@Data
public class MetricDefVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 指标编码。 */
    private String code;

    /** 展示名。 */
    private String name;

    /** 所属域。 */
    private String dimension;

    /** 值类型：number/duration/enum/bool。 */
    private String valueType;

    /** 单位。 */
    private String unit;

    /** enum 可选值 JSON。 */
    private String optionsJson;

    /** 聚合方式：sum/avg/max/min/last。 */
    private String aggType;

    /** 目标值。 */
    private BigDecimal targetValue;

    /** 是否上首页。 */
    private Integer showOnHome;

    /** 排序号。 */
    private Integer sortNo;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
