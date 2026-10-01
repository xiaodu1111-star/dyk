package com.xiaodu.personalos.system.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 指标周期汇总 VO（按定义的 aggType 在周期内聚合）。
 *
 * @author Kou
 */
@Data
public class MetricSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 指标编码。 */
    private String code;

    /** 指标名。 */
    private String name;

    /** 周期 key（2026-W40 / 2026-10）。 */
    private String periodKey;

    /** 聚合方式。 */
    private String aggType;

    /** 聚合值；周期内无记录时为 null。 */
    private BigDecimal value;

    /** 周期内记录条数。 */
    private int count;
}
