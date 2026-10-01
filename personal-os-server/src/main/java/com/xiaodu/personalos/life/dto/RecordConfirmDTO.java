package com.xiaodu.personalos.life.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 记录确认落库入参（M3）。快捷记录与手动的统一入口。
 *
 * @author Kou
 */
@Data
public class RecordConfirmDTO {

    /** 指标定义 id。 */
    @NotNull(message = "指标 id 不能为空")
    private Long metricId;

    /** 记录值；打卡型忽略（恒记 1），计数型为增量。 */
    private BigDecimal value;

    /** 归属日；为空取今天（口径见 PeriodUtil）。 */
    private LocalDate date;
}
