package com.xiaodu.personalos.life.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 连续打卡（streak）VO（M3）。
 *
 * @author Kou
 */
@Data
public class StreakVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 习惯 id。 */
    private Long habitId;

    /** 习惯名。 */
    private String name;

    /** 连续有效天数（自然日口径）。 */
    private Integer streak;
}
