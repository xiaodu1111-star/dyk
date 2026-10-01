package com.xiaodu.personalos.common.result;

import lombok.Getter;

/**
 * 全局错误码。
 *
 * <p>分段约定（对齐《backend-architecture.md》4.2 节）：</p>
 * <ul>
 *   <li>0        —— 成功</li>
 *   <li>400/401/403/404/500 —— 通用错误，与 HTTP 语义对齐</li>
 *   <li>1000-1099 —— 通用业务失败</li>
 *   <li>10000-10999 —— work 工作域</li>
 *   <li>11000-11999 —— learn 学习域</li>
 *   <li>12000-12999 —— fit 运动域</li>
 *   <li>13000-13999 —— finance 理财域</li>
 *   <li>14000-14999 —— life 生活域</li>
 *   <li>15000-15999 —— system 系统 / 通用引擎</li>
 * </ul>
 *
 * @author Kou
 */
@Getter
public enum ErrorCode {

    // ---- 通用 ----------------------------------------------------------
    SUCCESS(0, "ok"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务端异常"),

    // ---- 业务通用 ------------------------------------------------------
    BIZ_ERROR(1000, "业务处理失败"),

    // ---- system 系统域 -------------------------------------------------
    USER_NOT_FOUND(15002, "用户不存在"),
    PASSWORD_ERROR(15003, "用户名或密码错误");

    /** 业务错误码。 */
    private final int code;

    /** 面向用户可读的默认提示。 */
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
