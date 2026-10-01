package com.xiaodu.personalos.common.result;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

import lombok.AllArgsConstructor;

/**
 * 统一响应体。
 *
 * <p>约定：成功 {@code {"code":0,"msg":"ok","data":{...}}}；失败 {@code {"code":xxx,"msg":"...","data":null}}。
 * HTTP 状态码恒为 200，业务结果由 {@code code} 表达（对齐《backend-architecture.md》4.1 节）。</p>
 *
 * @param <T> 业务数据类型
 * @author Kou
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 错误码，0 表示成功。 */
    private int code;

    /** 提示信息。 */
    private String msg;

    /** 业务数据。 */
    private T data;

    /** 成功（无数据）。 */
    public static <T> R<T> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    /** 成功（带数据）。 */
    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    /** 失败（自定义码与信息）。 */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /** 失败（默认业务通用码）。 */
    public static <T> R<T> fail(String msg) {
        return new R<>(ErrorCode.BIZ_ERROR.getCode(), msg, null);
    }

    /** 失败（使用错误码枚举的默认提示）。 */
    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMsg(), null);
    }

    /** 失败（使用错误码枚举但覆盖提示信息）。 */
    public static <T> R<T> fail(ErrorCode errorCode, String msg) {
        return new R<>(errorCode.getCode(), msg, null);
    }
}
