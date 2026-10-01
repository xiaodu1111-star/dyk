package com.xiaodu.personalos.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 写入指标记录入参（按 code 定位指标，按 recordDate upsert）。
 *
 * @author Kou
 */
@Data
public class MetricRecordDTO {

    /** 记录归属日；为空则取今天（口径见 PeriodUtil）。 */
    private LocalDate recordDate;

    /** 数值（number/duration 类型必填）。 */
    private BigDecimal valueNum;

    /** 文本值（enum/bool 类型必填）。 */
    @Size(max = 200, message = "valueText 最长 200 字")
    private String valueText;

    /** 来源业务实体类型（可选）。 */
    @Size(max = 30, message = "refType 最长 30 字")
    private String refType;

    /** 来源业务实体 id（可选）。 */
    private Long refId;

    /** 备注。 */
    @Size(max = 255, message = "备注最长 255 字")
    private String remark;
}
