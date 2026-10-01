package com.xiaodu.personalos.life.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记录落库结果 VO（M3）。
 *
 * @author Kou
 */
@Data
public class RecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录 id。 */
    private Long id;

    /** 指标定义 id。 */
    private Long metricId;

    /** 指标名。 */
    private String metricName;

    /** 归属日。 */
    private LocalDate date;

    /** 记录写入时刻。 */
    private LocalDateTime recordTime;

    /** 记录值（打卡型 1，计数型增量）。 */
    private BigDecimal value;
}
