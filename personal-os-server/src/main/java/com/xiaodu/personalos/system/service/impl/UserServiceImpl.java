package com.xiaodu.personalos.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaodu.personalos.common.exception.BizException;
import com.xiaodu.personalos.common.result.ErrorCode;
import com.xiaodu.personalos.common.util.PasswordUtil;
import com.xiaodu.personalos.system.dto.LoginDTO;
import com.xiaodu.personalos.system.entity.SysUser;
import com.xiaodu.personalos.system.mapper.UserMapper;
import com.xiaodu.personalos.system.service.UserService;
import com.xiaodu.personalos.system.vo.LoginVO;
import com.xiaodu.personalos.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户 / 认证服务实现。
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }

        // 建立会话
        StpUtil.login(user.getId());

        // 刷新最近登录时间（仅走 updateFill 的 update_time 自动填充）
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(update);

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setExpiresIn(StpUtil.getTokenTimeout());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO currentUser() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setCity(user.getCity());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}
