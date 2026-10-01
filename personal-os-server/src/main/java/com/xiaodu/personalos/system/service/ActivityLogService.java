package com.xiaodu.personalos.system.service;

import com.xiaodu.personalos.system.entity.ActActivityLog;

/**
 * 活动流引擎服务（M0 基础设施，表 act_activity_log）。
 *
 * <p><b>旁路写入</b>：仅供各模块 Service 在业务落库后调用，不暴露 REST。
 * 内部吞掉一切异常（只 warn 不抛），<b>严禁</b>因活动流失败影响主流程。</p>
 *
 * @author Kou
 */
public interface ActivityLogService {

    /**
     * 写入一条活动流（完整入口）。
     *
     * <p>自动补齐：occurredAt 为空取当前时刻；activityDate 由 PeriodUtil 统一折算。
     * 任何异常（含 DB 异常、必填缺失）只记 warn，返回 null，不影响调用方事务。</p>
     *
     * @param entry 活动流实体（调用方填 userId/dimension/bizType/title 等）
     * @return 落库后的 id；失败返回 null
     */
    Long log(ActActivityLog entry);

    /**
     * 写入一条活动流（核心字段便捷入口）。
     *
     * @param userId    用户 id
     * @param dimension 维度：work/learn/fit/finance/life
     * @param bizType   业务类型：task_done/study/workout/expense/checkin
     * @param title     标题
     * @return 落库后的 id；失败返回 null
     */
    Long log(Long userId, String dimension, String bizType, String title);
}
