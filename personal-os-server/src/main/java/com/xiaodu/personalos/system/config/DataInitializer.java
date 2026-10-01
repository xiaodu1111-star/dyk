package com.xiaodu.personalos.system.config;

import com.xiaodu.personalos.common.util.PasswordUtil;
import com.xiaodu.personalos.system.entity.SysUser;
import com.xiaodu.personalos.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动数据初始化：若 {@code sys_user} 表为空则播种默认账号。
 *
 * <p>默认账号：{@code admin / admin123}（BCrypt 现算，不落库脚本）。
 * 带存在性判断，重复启动不会重复插入。</p>
 *
 * @author Kou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    /** 默认管理员用户名。 */
    private static final String DEFAULT_USERNAME = "admin";

    /** 默认管理员明文密码。 */
    private static final String DEFAULT_PASSWORD = "admin123";

    private final UserMapper userMapper;

    @Override
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            log.info("sys_user 已存在 {} 条记录，跳过默认账号初始化", count);
            return;
        }

        SysUser admin = new SysUser();
        admin.setUsername(DEFAULT_USERNAME);
        admin.setPassword(PasswordUtil.encode(DEFAULT_PASSWORD));
        admin.setNickname("小杜");
        admin.setCity("嘉兴");
        admin.setDailyTargetMinutes(480);
        userMapper.insert(admin);

        // 安全：日志不输出明文密码
        log.info("已初始化默认账号: {}（初始密码见 README-dev.md），id={}", DEFAULT_USERNAME, admin.getId());
    }
}
