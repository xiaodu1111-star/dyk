package com.xiaodu.personalos.system.service;

import com.xiaodu.personalos.system.dto.LoginDTO;
import com.xiaodu.personalos.system.vo.LoginVO;
import com.xiaodu.personalos.system.vo.UserVO;

/**
 * 用户 / 认证服务。
 *
 * @author Kou
 */
public interface UserService {

    /**
     * 登录：校验用户名密码，建立 Sa-Token 会话并刷新最近登录时间。
     *
     * @param dto 登录入参
     * @return 登录结果（含 token）
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户
     */
    UserVO currentUser();

    /** 退出登录。 */
    void logout();
}
