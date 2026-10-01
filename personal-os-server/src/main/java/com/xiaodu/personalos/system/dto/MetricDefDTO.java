package com.xiaodu.personalos.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 新建指标定义入参。
 *
 * @author Kou
 */
@Data
public class MetricDefDTO {

    /** 指标编码（唯一），如 weight / water_cup / mood。 */
    @NotBlank(message = "指标编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "编码只允许小写字母、数字与下划线")
    private String code;

    /** 展示名。 */
    @NotBlank(message = "指标名不能为空")
    @Size(max = 50, message = "指标名最长 50 字")
    private String name;

    /** 所属域。 */
    @NotBlank(message = "dimension 不能为空")
    @Pattern(regexp = "^(work|learn|fit|finance|life)$", message = "dimension 取值非法")
    private String dimension;

    /** 值类型：number/duration/enum/bool。 */
    @Pattern(regexp = "^(number|duration|enum|bool)$", message = "valueType 取值非法")
    private String valueType = "number";

    /** 单位。 */
    @Size(max = 20, message = "单位最长 20 字")
    private String unit;

    /** enum 类型的可选值 JSON。 */
    @Size(max = 500, message = "optionsJson 最长 500 字")
    private String optionsJson;

    /** 聚合方式：sum/avg/max/min/last。 */
    @Pattern(regexp = "^(sum|avg|max|min|last)$", message = "aggType 取值非法")
    private String aggType = "last";

    /** 目标值，用于进度条。 */
    private BigDecimal targetValue;

    /** 是否上首页：1 是 / 0 否。 */
    private Integer showOnHome = 1;

    /** 排序号。 */
    private Integer sortNo = 0;
}
