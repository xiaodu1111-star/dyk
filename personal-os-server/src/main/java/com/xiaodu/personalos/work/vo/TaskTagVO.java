package com.xiaodu.personalos.work.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 任务标签精简 VO（仅 id / name / color，前端 chips 使用）。
 *
 * @author Kou
 */
@Data
public class TaskTagVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 标签 id。 */
    private Long id;

    /** 标签名。 */
    private String name;

    /** 颜色。 */
    private String color;
}
