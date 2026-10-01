package com.xiaodu.personalos.sop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * SOP 步骤实体（表 {@code work_sop_step}，Flyway V15）。
 *
 * <p>本表无 {@code deleted} 字段，逻辑删除不适用于此表，故不配置 {@link com.baomidou.mybatisplus.annotation.TableLogic}。</p>
 *
 * @author Alex
 */
@Data
@TableName("work_sop_step")
public class WorkSopStep implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属 SOP id。 */
    private Long sopId;

    /** 步骤序号，从 1 连续递增。 */
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
