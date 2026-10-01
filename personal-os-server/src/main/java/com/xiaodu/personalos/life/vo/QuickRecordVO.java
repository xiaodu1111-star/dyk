package com.xiaodu.personalos.life.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 快捷记录预填 VO（M3）。
 *
 * <p>{@code POST /api/life/quick-record} 的返回：<b>只解析不落库</b>，
 * 前端拿到本对象弹确认卡，确认后再调 {@code POST /api/life/records}。</p>
 *
 * @author Kou
 */
@Data
public class QuickRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 命中的指标定义 id。 */
    private Long metricId;

    /** 命中的指标名。 */
    private String metricName;

    /** 解析出的值：计数型为文本中的数字（无则 1）；打卡型为 1。 */
    private BigDecimal value;

    /** 归属日（默认今天）。 */
    private LocalDate date;
}
