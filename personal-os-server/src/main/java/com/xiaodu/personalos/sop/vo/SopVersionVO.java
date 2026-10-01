package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SOP 版本摘要 VO（详情页版本时间线用，不含 content_json 全量）。
 *
 * @author Alex
 */
@Data
public class SopVersionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 版本记录 id（回看完整快照时用）。 */
    private Long id;

    /** 版本号。 */
    private String version;

    /** 本次优化了什么。 */
    private String changeNote;

    /** 创建时间。 */
    private LocalDateTime createTime;
}
