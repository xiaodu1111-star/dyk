package com.xiaodu.personalos.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.xiaodu.personalos.common.result.ErrorCode;
import com.xiaodu.personalos.common.result.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * <p>统一返回 HTTP 200 + {@link R} 内的 {@code code}（对齐《backend-architecture.md》4.2 节）。
 * 技术细节只进日志，不泄露给前端。</p>
 *
 * @author Kou
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：原样透出 code / msg。 */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException ex) {
        log.warn("[业务异常] code={}, msg={}", ex.getCode(), ex.getMessage());
        return R.fail(ex.getCode(), ex.getMessage());
    }

    /** 请求体校验失败（@RequestBody + @Valid）。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = firstErrorMessage(ex.getBindingResult().getFieldErrors());
        log.warn("[参数校验失败] {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 表单/查询参数绑定校验失败。 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException ex) {
        String msg = firstErrorMessage(ex.getBindingResult().getFieldErrors());
        log.warn("[参数绑定失败] {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** @Validated 参数级校验失败（@PathVariable / @RequestParam）。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(ErrorCode.PARAM_ERROR.getMsg());
        log.warn("[参数约束失败] {}", msg);
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 请求体为畸形 JSON / 无法反序列化。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("[请求体解析失败] {}", ex.getMessage());
        return R.fail(ErrorCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /** Sa-Token 未登录。 */
    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException ex) {
        log.warn("[未登录] type={}, msg={}", ex.getType(), ex.getMessage());
        return R.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMsg());
    }

    /** Sa-Token 无权限。 */
    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException ex) {
        log.warn("[无权限] code={}, msg={}", ex.getCode(), ex.getMessage());
        return R.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMsg());
    }

    /** 数据访问异常：记录摘要，不向前端泄露 SQL。 */
    @ExceptionHandler(DataAccessException.class)
    public R<Void> handleDataAccess(DataAccessException ex) {
        log.error("[数据访问异常]", ex);
        return R.fail(ErrorCode.SERVER_ERROR.getCode(), "数据访问异常");
    }

    /** 兜底异常。 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception ex) {
        log.error("[系统异常]", ex);
        return R.fail(ErrorCode.SERVER_ERROR.getCode(), ErrorCode.SERVER_ERROR.getMsg());
    }

    /** 取首个字段校验错误信息。 */
    private String firstErrorMessage(java.util.List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.PARAM_ERROR.getMsg());
    }
}
