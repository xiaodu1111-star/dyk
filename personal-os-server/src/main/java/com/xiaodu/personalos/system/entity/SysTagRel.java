package com.xiaodu.personalos.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签多态关联实体（表 {@code sys_tag_rel}，Flyway V2）。
 *
 * <p>多态关联：{@code (tag_id, biz_type, biz_id)} 联合唯一；
 * biz_type 取值 task / note / workout / transaction / sop ...。</p>
 *
 * @author Kou
 */
@Data
@TableName("sys_tag_rel")
public class SysTagRel implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签 id。 */
    private Long tagId;

    /** 业务实体类型（task/note/workout/transaction/sop...）。 */
    private String bizType;

    /** 业务实体 id。 */
    private Long bizId;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
