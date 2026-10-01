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
 * 用户实体（表 {@code sys_user}）。
 *
 * @author Kou
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名（唯一）。 */
    private String username;

    /** 密码（BCrypt）。 */
    private String password;

    /** 昵称。 */
    private String nickname;

    /** 头像。 */
    private String avatar;

    /** 城市。 */
    private String city;

    /** 每日目标投入分钟。 */
    private Integer dailyTargetMinutes;

    /** 最近登录时间。 */
    private LocalDateTime lastLoginAt;

    /** 创建时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充）。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0 正常，1 已删除）。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
