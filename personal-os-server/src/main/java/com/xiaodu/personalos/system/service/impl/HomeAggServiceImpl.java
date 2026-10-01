package com.xiaodu.personalos.system.service.impl;

import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.system.service.HomeAggService;
import com.xiaodu.personalos.system.vo.HomeAggVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 首页聚合服务实现（M0 骨架）。
 *
 * <p>M0 只返回契约结构与骨架值（0 / 空数组）；P1 集成阶段：</p>
 * <ul>
 *   <li>M1 工作域接线 work（todayTotal / todayDone / overdue）</li>
 *   <li>M3 生活域接线 life（checkin / habits）与 streakDays</li>
 *   <li>M2 SOP 库接线 sop.top 与 sopHints</li>
 * </ul>
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAggServiceImpl implements HomeAggService {

    @Override
    @Transactional(readOnly = true)
    public HomeAggVO aggregate() {
        HomeAggVO vo = new HomeAggVO();

        // 今日信息：日期与星期统一走 PeriodUtil 口径
        LocalDate today = PeriodUtil.today();
        vo.getToday().setDate(PeriodUtil.formatDate(today));
        vo.getToday().setWeek(PeriodUtil.dayOfWeekCn(today));

        // ---- 以下为骨架值，P1 各模块接线 ----
        // TODO(P1-M1): work.todayTotal / todayDone / overdue ← 工作任务域
        // TODO(P1-M3): life.checkinDone / checkinTotal / habits ← 生活习惯域
        // TODO(P1-M3): streakDays ← 习惯连击
        // TODO(P1-M2): sop.top / sopHints ← SOP 库
        return vo;
    }
}
