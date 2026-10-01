package com.xiaodu.personalos.work.service;

import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.work.dto.TaskCreateDTO;
import com.xiaodu.personalos.work.dto.TaskStatusDTO;
import com.xiaodu.personalos.work.dto.TaskUpdateDTO;
import com.xiaodu.personalos.work.vo.TaskVO;

/**
 * 工作域任务服务（M1，表 work_task）。
 *
 * <p>时间口径一律走 M0 的 {@code PeriodUtil}；标签落库走 M0 的 {@code TagService}；
 * 完成时旁路写活动流走 M0 的 {@code ActivityLogService}。</p>
 *
 * @author Kou
 */
public interface WorkTaskService {

    /**
     * 新建任务（只有 title 必填）。
     *
     * @param dto 入参
     * @return 新建任务 id
     */
    Long create(TaskCreateDTO dto);

    /**
     * 分页查询任务（三视图 + 可选状态过滤）。
     *
     * @param view   视图：today / all / overdue；非法或空兜底为 all
     * @param status 状态过滤，null/空 = 不过滤
     * @param page   页码，默认 1、最小 1
     * @param size   每页条数，默认 20、上限 100
     * @return 分页结果
     */
    PageResult<TaskVO> page(String view, String status, long page, long size);

    /**
     * 任务详情（含标签）。
     *
     * @param id 任务 id
     * @return 任务 VO
     * @throws com.xiaodu.personalos.common.exception.BizException 10001 任务不存在
     */
    TaskVO detail(Long id);

    /**
     * 全量更新任务（标签走 diff 同步）。
     *
     * @param id  任务 id
     * @param dto 入参
     * @throws com.xiaodu.personalos.common.exception.BizException 10001 任务不存在
     */
    void update(Long id, TaskUpdateDTO dto);

    /**
     * 状态流转（状态机校验；完成写 done_at + 活动流；从 done 回 todo 清 done_at）。
     *
     * @param id  任务 id
     * @param dto 目标状态
     * @throws com.xiaodu.personalos.common.exception.BizException 10001 / 10002
     */
    void updateStatus(Long id, TaskStatusDTO dto);

    /**
     * 删除任务（逻辑删；先清空标签关联）。
     *
     * @param id 任务 id
     * @throws com.xiaodu.personalos.common.exception.BizException 10001 任务不存在
     */
    void delete(Long id);

    // ---- 预留给 P1 首页聚合接线（M1 不调用，但已实现） ------------------

    /** 今日视图且未完成的条数（口径同 today）。 */
    long countToday(long userId);

    /** 今日完成的条数（status=done 且 done_at 落在今日 [起, 止) 区间）。 */
    long countTodayDone(long userId);

    /** 逾期条数（口径同 overdue 视图）。 */
    long countOverdue(long userId);
}
