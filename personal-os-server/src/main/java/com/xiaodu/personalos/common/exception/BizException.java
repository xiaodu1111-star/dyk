package com.xiaodu.personalos.common.exception;

import com.xiaodu.personalos.common.result.ErrorCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常。
 *
 * <p>Service 层统一抛出此异常，由 {@link GlobalExceptionHandler} 兜住并转为
 * {@code R.fail(code, msg)}；禁止直接 {@code throw new RuntimeException("中文串")}。</p>
 *
 * @author Kou
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务错误码。 */
    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    /** 使用错误码枚举作为 code，但自定义面向用户的提示。 */
    public BizException(ErrorCode errorCode, String msg) {
        super(msg);
        this.code = errorCode.getCode();
    }

    /** 完全自定义 code 与提示。 */
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
