package com.xiaodu.personalos.life.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 习惯 VO（列表 / 详情，M3）。含今日完成状态。
 *
 * @author Kou
 */
@Data
public class HabitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 习惯 id（即 sys_metric_def.id）。 */
    private Long id;

    /** 指标编码（自动生成）。 */
    private String code;

    /** 习惯名。 */
    private String name;

    /** 类型：checkin 打卡型 / count 计数型。 */
    private String type;

    /** 单位（计数型）。 */
    private String unit;

    /** 目标值（计数型，打卡型为 1）。 */
    private BigDecimal targetValue;

    /** 今日是否完成：打卡型 = 是否已打卡；计数型 = 当日累计是否已达目标。 */
    private Boolean done;

    /** 今日累计值：打卡型 0/1；计数型当日 SUM。 */
    private BigDecimal value;

    /** 今日归属日。 */
    private java.time.LocalDate date;
}
