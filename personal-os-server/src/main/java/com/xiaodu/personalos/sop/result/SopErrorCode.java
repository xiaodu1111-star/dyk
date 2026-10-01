package com.xiaodu.personalos.sop.result;

import com.xiaodu.personalos.common.exception.BizException;
import lombok.Getter;

/**
 * SOP 库（M2）错误码。
 *
 * <p>号段：10100-10199（work 大段内 SOP 子段，与 M1 work 域 10000-10099 互斥）。
 * 约定同 {@code SystemErrorCode}：枚举 + {@link #toException()}，零侵入 common。</p>
 *
 * @author Alex
 */
@Getter
public enum SopErrorCode {

    /** SOP 不存在。 */
    SOP_NOT_FOUND(10101, "SOP 不存在"),

    /** 步骤数据非法（无步骤 / step_no 不连续 / 步骤标题为空）。 */
    STEP_INVALID(10102, "步骤数据非法"),

    /** 该次执行已结束。 */
    RUN_ALREADY_FINISHED(10103, "该次执行已结束"),

    /** 参数非法。 */
    PARAM_INVALID(10104, "参数非法");

    /** 业务错误码。 */
    private final int code;

    /** 面向用户可读的默认提示。 */
    private final String msg;

    SopErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 直接转为业务异常。 */
    public BizException toException() {
        return new BizException(code, msg);
    }

    /** 转为业务异常并使用自定义提示。 */
    public BizException toException(String msg) {
        return new BizException(code, msg);
    }
}
