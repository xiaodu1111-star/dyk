package com.xiaodu.personalos.work.result;

import com.xiaodu.personalos.common.exception.BizException;
import lombok.Getter;

/**
 * 工作域错误码（M1，号段 10000-10099）。
 *
 * <p>对齐《backend-architecture.md》0.5 节第 11 条 P1 并行约定：
 * 保持 common 的 ErrorCode 不动，各模块错误码定义在自己包内，
 * 通过 {@code BizException(int, String)} 抛出，零侵入 common。</p>
 *
 * @author Kou
 */
@Getter
public enum WorkErrorCode {

    /** 任务不存在（含不属于当前用户，避免信息泄露）。 */
    TASK_NOT_FOUND(10001, "任务不存在"),

    /** 非法状态流转。 */
    INVALID_STATUS_TRANSITION(10002, "非法的状态流转"),

    /** 参数非法。 */
    PARAM_INVALID(10003, "参数非法");

    /** 业务错误码。 */
    private final int code;

    /** 面向用户可读的默认提示。 */
    private final String msg;

    WorkErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 直接转为业务异常。 */
    public BizException toException() {
        return new BizException(code, msg);
    }
}
