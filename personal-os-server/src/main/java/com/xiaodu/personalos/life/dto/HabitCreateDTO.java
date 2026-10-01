package com.xiaodu.personalos.life.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建 / 修改习惯入参（M3）。
 *
 * <p>习惯 = 指标引擎的一条定义，本 DTO 只承载映射所需字段，
 * 由 Service 折算到 {@code sys_metric_def} 的 value_type / agg_type / unit / target_value。
 * 打卡型（checkin）：value_type=bool、agg_type=last、target_value=1、unit 空；
 * 计数型（count）：value_type=number、agg_type=sum、unit 如「杯」、target_value 如 8。</p>
 *
 * @author Kou
 */
@Data
public class HabitCreateDTO {

    /** 习惯名（展示名）。 */
    @NotBlank(message = "习惯名不能为空")
    @Size(max = 50, message = "习惯名最长 50 字")
    private String name;

    /** 习惯类型：checkin 打卡型 / count 计数型。 */
    @NotBlank(message = "习惯类型不能为空")
    @Pattern(regexp = "^(checkin|count)$", message = "习惯类型只能是 checkin 或 count")
    private String type;

    /** 单位（计数型用，如「杯」「公里」；打卡型忽略）。 */
    @Size(max = 20, message = "单位最长 20 字")
    private String unit;

    /** 目标值（计数型用，如 8；打卡型忽略，恒为 1）。 */
    private BigDecimal targetValue;
}
