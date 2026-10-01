package com.xiaodu.personalos.sop.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SOP 步骤 VO（详情 / 版本快照渲染共用）。
 *
 * @author Alex
 */
@Data
public class SopStepVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 步骤序号，从 1 连续。 */
    private Integer stepNo;

    /** 步骤标题。 */
    private String title;

    /** 操作说明 / Markdown。 */
    private String detail;

    /** 避坑提示。 */
    private String tip;

    /** 预估分钟。 */
    private Integer estimateMin;
}
