package com.xiaodu.personalos.system.controller;

import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.system.dto.LoginDTO;
import com.xiaodu.personalos.system.service.UserService;
import com.xiaodu.personalos.system.vo.LoginVO;
import com.xiaodu.personalos.system.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录 / 登出 / 当前用户")
public class AuthController {

    private final UserService userService;

    /** 登录（无需 token）。 */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    /** 登出（需登录）。 */
    @PostMapping("/logout")
    @Operation(summary = "登出")
    public R<Void> logout() {
        userService.logout();
        return R.ok();
    }

    /** 当前用户信息（需登录）。 */
    @GetMapping("/me")
    @Operation(summary = "当前用户信息")
    public R<UserVO> me() {
        return R.ok(userService.currentUser());
    }
}
