package com.xiaodu.personalos.system.result;

import com.xiaodu.personalos.common.exception.BizException;
import lombok.Getter;

/**
 * system 系统域 / 通用引擎错误码（M0）。
 *
 * <p>对齐《backend-architecture.md》0.5 节第 11 条 P1 并行约定：
 * ErrorCode 保持通用枚举不动，各模块错误码定义在自己包内，
 * 通过 {@code BizException(int, String)} 抛出，零侵入 common。</p>
 *
 * <p>号段：15000-15999（system 系统 / 通用引擎）。
 * 15000-15099 预留给登录闭环（已占用 15002/15003）；
 * 15100-15199 标签引擎；15200-15299 指标引擎。</p>
 *
 * @author Kou
 */
@Getter
public enum SystemErrorCode {

    // ---- 标签引擎 15100+ ----------------------------------------------
    TAG_NAME_EXISTS(15100, "同名标签已存在"),
    TAG_NOT_FOUND(15101, "标签不存在"),
    TAG_REL_NOT_FOUND(15102, "标签关联不存在"),

    // ---- 指标引擎 15200+ ----------------------------------------------
    METRIC_CODE_EXISTS(15200, "指标编码已存在"),
    METRIC_NOT_FOUND(15201, "指标不存在"),
    METRIC_VALUE_INVALID(15202, "指标值与类型不匹配"),
    METRIC_PERIOD_INVALID(15203, "周期 key 格式非法，应为 2026-W40 或 2026-10");

    /** 业务错误码。 */
    private final int code;

    /** 面向用户可读的默认提示。 */
    private final String msg;

    SystemErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 直接转为业务异常。 */
    public BizException toException() {
        return new BizException(code, msg);
    }
}
