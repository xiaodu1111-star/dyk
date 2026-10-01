package com.xiaodu.personalos.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建标签入参。
 *
 * @author Kou
 */
@Data
public class TagCreateDTO {

    /** 标签名（与 scope 联合唯一）。 */
    @NotBlank(message = "标签名不能为空")
    @Size(max = 50, message = "标签名最长 50 字")
    private String name;

    /** 颜色，默认 #639922。 */
    @Size(max = 20, message = "颜色值最长 20 字")
    private String color = "#639922";

    /** 所属域，common=通用。 */
    @Pattern(regexp = "^(common|work|learn|fit|finance|life)$", message = "scope 取值非法")
    private String scope = "common";
}
