package com.xiaodu.personalos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Personal OS 后端启动类。
 *
 * <p>个人自托管「个人操作系统」服务端，当前阶段仅包含登录闭环。</p>
 *
 * @author Kou
 */
@SpringBootApplication
public class PersonalOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalOsApplication.class, args);
    }
}
