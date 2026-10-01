package com.xiaodu.personalos.system.service;

import com.xiaodu.personalos.system.dto.MetricDefDTO;
import com.xiaodu.personalos.system.dto.MetricRecordDTO;
import com.xiaodu.personalos.system.vo.MetricDefVO;
import com.xiaodu.personalos.system.vo.MetricRecordVO;
import com.xiaodu.personalos.system.vo.MetricSummaryVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 指标引擎服务（M0 基础设施，表 sys_metric_def / sys_metric_record）。
 *
 * <p>新增追踪项只需插一条 def，不改表不改码。
 * 周期口径（周 key / 月 key / 归属日）一律走 common 的 PeriodUtil。</p>
 *
 * @author Kou
 */
public interface MetricService {

    /**
     * 新增指标定义。
     *
     * @param dto 入参
     * @return 新建定义
     * @throws com.xiaodu.personalos.common.exception.BizException 15200 编码已存在
     */
    MetricDefVO create(MetricDefDTO dto);

    /**
     * 查询指标定义。
     *
     * @param dimension 域过滤，null/空 = 全部
     * @return 定义列表（sort_no 升序，id 升序）
     */
    List<MetricDefVO> list(String dimension);

    /**
     * 写入一条记录（按 metric + recordDate upsert：同日已有则更新值，否则插入）。
     *
     * @param code 指标编码
     * @param dto  记录入参（recordDate 为空取今天，口径见 PeriodUtil）
     * @return 落库后的记录
     * @throws com.xiaodu.personalos.common.exception.BizException 15201 指标不存在；15202 值与类型不匹配
     */
    MetricRecordVO record(String code, MetricRecordDTO dto);

    /**
     * 查询记录（日期闭区间）。
     *
     * @param code  指标编码
     * @param start 起始日（含），null = 不限
     * @param end   结束日（含），null = 不限
     * @return 记录列表（record_date、id 升序）
     * @throws com.xiaodu.personalos.common.exception.BizException 15201 指标不存在
     */
    List<MetricRecordVO> records(String code, LocalDate start, LocalDate end);

    /**
     * 周期汇总：按定义的 aggType 在周期内聚合。
     *
     * @param code      指标编码
     * @param periodKey 周期 key（周 2026-W40 / 月 2026-10），由 PeriodUtil 解析边界
     * @return 汇总结果（无记录时 value=null, count=0）
     * @throws com.xiaodu.personalos.common.exception.BizException 15201 指标不存在；15203 周期 key 非法
     */
    MetricSummaryVO summary(String code, String periodKey);
}
