package com.xiaodu.personalos.system.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指标记录 VO。
 *
 * @author Kou
 */
@Data
public class MetricRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 指标定义 id。 */
    private Long metricId;

    /** 记录归属日。 */
    private LocalDate recordDate;

    /** 记录写入时刻。 */
    private LocalDateTime recordTime;

    /** 数值。 */
    private BigDecimal valueNum;

    /** 文本值。 */
    private String valueText;

    /** 来源业务实体类型。 */
    private String refType;

    /** 来源业务实体 id。 */
    private Long refId;

    /** 备注。 */
    private String remark;
}
