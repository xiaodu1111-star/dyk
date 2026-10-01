package com.xiaodu.personalos.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一标签实体（表 {@code sys_tag}，Flyway V2）。
 *
 * @author Kou
 */
@Data
@TableName("sys_tag")
public class SysTag implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名（与 scope 联合唯一）。 */
    private String name;

    /** 颜色。 */
    private String color;

    /** 所属域，common=通用。 */
    private String scope;

    /** 被引用次数（绑定关联时维护）。 */
    private Integer useCount;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除标记（0 正常，1 已删除）。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
