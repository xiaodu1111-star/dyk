package com.xiaodu.personalos.system.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签 VO。
 *
 * @author Kou
 */
@Data
public class TagVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 标签名。 */
    private String name;

    /** 颜色。 */
    private String color;

    /** 所属域，common=通用。 */
    private String scope;

    /** 被引用次数。 */
    private Integer useCount;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
