package com.xiaodu.personalos.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前用户信息出参。
 *
 * @author Kou
 */
@Data
@Schema(description = "用户信息")
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "头像")
    private String avatar;
}
