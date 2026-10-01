package com.xiaodu.personalos.life.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 打卡入参（M3）。
 *
 * <p>打卡型（checkin）忽略 value 字段；计数型（count）value 为当日增量（默认 1）。</p>
 *
 * @author Kou
 */
@Data
public class HabitCheckinDTO {

    /** 计数型增量值；打卡型忽略。为空时计数型默认 1。 */
    @DecimalMin(value = "0", message = "增量不能为负")
    private BigDecimal value;
}
