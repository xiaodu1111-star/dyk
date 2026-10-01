package com.xiaodu.personalos.life.result;

import com.xiaodu.personalos.common.exception.BizException;
import lombok.Getter;

/**
 * life 生活域错误码（M3）。
 *
 * <p>对齐《backend-architecture.md》0.5 节第 11 条 P1 并行约定：
 * ErrorCode 保持通用枚举不动，各模块错误码定义在自己包内，
 * 通过 {@code BizException(int, String)} 抛出，零侵入 common。</p>
 *
 * <p>号段：14000-14099（life 生活域，见 dev-workflow.md §3.4）。</p>
 *
 * @author Kou
 */
@Getter
public enum LifeErrorCode {

    /** 习惯（指标定义）不存在。 */
    HABIT_NOT_FOUND(14001, "习惯不存在"),

    /** 打卡型习惯今日已打卡（幂等拒绝）。 */
    ALREADY_CHECKED(14002, "今日已打卡，无需重复"),

    /** 快捷记录文本解析失败。 */
    PARSE_FAILED(14003, "未能识别习惯，请换种说法，如「跑步 5」"),

    /** 入参非法。 */
    PARAM_INVALID(14004, "参数不合法");

    /** 业务错误码。 */
    private final int code;

    /** 面向用户可读的默认提示。 */
    private final String msg;

    LifeErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 直接转为业务异常。 */
    public BizException toException() {
        return new BizException(code, msg);
    }

    /** 转为业务异常并覆盖提示信息。 */
    public BizException toException(String message) {
        return new BizException(code, message);
    }
}
