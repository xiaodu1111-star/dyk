package com.xiaodu.personalos.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签绑定 / 解绑入参（多态关联）。
 *
 * @author Kou
 */
@Data
public class TagRelDTO {

    /** 业务实体类型：task/note/workout/transaction/sop...。 */
    @NotBlank(message = "bizType 不能为空")
    @Pattern(regexp = "^[a-z][a-z_]{1,29}$", message = "bizType 只允许小写字母与下划线")
    private String bizType;

    /** 业务实体 id。 */
    @NotNull(message = "bizId 不能为空")
    private Long bizId;
}
