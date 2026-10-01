package com.xiaodu.personalos.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.life.service.HabitService;
import com.xiaodu.personalos.life.vo.HabitVO;
import com.xiaodu.personalos.life.vo.StreakVO;
import com.xiaodu.personalos.sop.entity.WorkSop;
import com.xiaodu.personalos.sop.mapper.WorkSopMapper;
import com.xiaodu.personalos.system.mapper.ActivityLogMapper;
import com.xiaodu.personalos.system.service.HomeAggService;
import com.xiaodu.personalos.system.vo.HomeAggVO;
import com.xiaodu.personalos.work.service.WorkTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 首页聚合服务实现（P1 集成：M1 工作域 + M2 SOP 库 + M3 生活域接线）。
 *
 * <p><b>接线口径（RIce 定，2026-10-01 集成）</b>：</p>
 * <ul>
 *   <li>work.todayTotal / todayDone / overdue ← {@link WorkTaskService} 的三视图计数</li>
 *   <li>life.checkinDone / checkinTotal / habits ← {@link HabitService#list()}（今日口径）</li>
 *   <li>streakDays ← 各习惯 streak 的**最大值**（单个数字取最长连击，语义最贴近"坚持了几天"）</li>
 *   <li>sop.top ← use_count 倒序前 3（排除 deprecated / 已删 / 非本人）</li>
 *   <li>sopHints ← 近 30 天维度内同一标题出现 ≥3 次的**任务**，且该标题尚无同名 SOP（已沉淀的不再提示）</li>
 * </ul>
 *
 * <p><b>容错口径</b>：首页是聚合展示，任一域查询失败只降级为该项空值 + warn 日志，
 * 绝不让整个首页 500（与活动流旁路同一设计哲学）。</p>
 *
 * @author Kou / RIce
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAggServiceImpl implements HomeAggService {

    /** 首页展示的高频 SOP 条数。 */
    private static final int SOP_TOP_LIMIT = 3;

    /** SOP 提示：窗口期内最少重复次数。 */
    private static final int SOP_HINT_MIN_COUNT = 3;

    /** SOP 提示：最多条数。 */
    private static final int SOP_HINT_LIMIT = 5;

    /** SOP 提示：统计窗口（天）。 */
    private static final int SOP_HINT_WINDOW_DAYS = 30;

    /** dimension 常量：工作域。 */
    private static final String DIM_WORK = "work";

    /** biz_type 常量：任务完成。 */
    private static final String BIZ_TASK_DONE = "task_done";

    private final WorkTaskService workTaskService;
    private final HabitService habitService;
    private final WorkSopMapper workSopMapper;
    private final ActivityLogMapper activityLogMapper;

    @Override
    @Transactional(readOnly = true)
    public HomeAggVO aggregate() {
        HomeAggVO vo = new HomeAggVO();

        // 今日信息：日期与星期统一走 PeriodUtil 口径
        LocalDate today = PeriodUtil.today();
        vo.getToday().setDate(PeriodUtil.formatDate(today));
        vo.getToday().setWeek(PeriodUtil.dayOfWeekCn(today));

        long userId = StpUtil.getLoginIdAsLong();

        fillWork(vo, userId);
        fillLife(vo);
        fillSopTop(vo, userId);
        fillSopHints(vo, userId, today);

        return vo;
    }

    // ---- M1 工作域 ------------------------------------------------------

    private void fillWork(HomeAggVO vo, long userId) {
        try {
            vo.getWork().setTodayTotal((int) workTaskService.countToday(userId));
            vo.getWork().setTodayDone((int) workTaskService.countTodayDone(userId));
            vo.getWork().setOverdue((int) workTaskService.countOverdue(userId));
        } catch (Exception e) {
            log.warn("[首页聚合] 工作域查询失败，降级为空值: {}", e.getMessage());
        }
    }

    // ---- M3 生活域 ------------------------------------------------------

    private void fillLife(HomeAggVO vo) {
        try {
            List<HabitVO> habits = habitService.list();
            if (habits == null) {
                return;
            }

            List<HomeAggVO.HabitVO> view = new ArrayList<>(habits.size());
            int done = 0;
            for (HabitVO h : habits) {
                if (Boolean.TRUE.equals(h.getDone())) {
                    done++;
                }
                HomeAggVO.HabitVO item = new HomeAggVO.HabitVO();
                item.setId(h.getId());
                item.setName(h.getName());
                item.setDone(Boolean.TRUE.equals(h.getDone()));
                view.add(item);
            }
            vo.getLife().setCheckinDone(done);
            vo.getLife().setCheckinTotal(habits.size());
            vo.getLife().setHabits(view);

            // 连续天数取各习惯最大值（逐习惯查询，习惯量级小，可接受）
            int maxStreak = 0;
            for (HabitVO h : habits) {
                try {
                    StreakVO s = habitService.streak(h.getId());
                    if (s != null && s.getStreak() != null) {
                        maxStreak = Math.max(maxStreak, s.getStreak());
                    }
                } catch (Exception e) {
                    log.warn("[首页聚合] 习惯 {} 连击查询失败，跳过: {}", h.getId(), e.getMessage());
                }
            }
            vo.setStreakDays(maxStreak);
        } catch (Exception e) {
            log.warn("[首页聚合] 生活域查询失败，降级为空值: {}", e.getMessage());
        }
    }

    // ---- M2 SOP 库 ------------------------------------------------------

    private void fillSopTop(HomeAggVO vo, long userId) {
        try {
            List<WorkSop> tops = workSopMapper.selectList(new LambdaQueryWrapper<WorkSop>()
                    .eq(WorkSop::getUserId, userId)
                    .eq(WorkSop::getDeleted, 0)
                    .ne(WorkSop::getStatus, "deprecated")
                    .orderByDesc(WorkSop::getUseCount)
                    .orderByDesc(WorkSop::getId)
                    .last("LIMIT " + SOP_TOP_LIMIT));

            List<HomeAggVO.SopItemVO> view = new ArrayList<>();
            for (WorkSop s : tops) {
                HomeAggVO.SopItemVO item = new HomeAggVO.SopItemVO();
                item.setId(s.getId());
                item.setTitle(s.getTitle());
                item.setUseCount(s.getUseCount() == null ? 0 : s.getUseCount());
                view.add(item);
            }
            vo.getSop().setTop(view);
        } catch (Exception e) {
            log.warn("[首页聚合] SOP 高频榜查询失败，降级为空值: {}", e.getMessage());
        }
    }

    /**
     * SOP 提示：窗口期内反复完成、且尚未沉淀成 SOP 的任务标题。
     */
    private void fillSopHints(HomeAggVO vo, long userId, LocalDate today) {
        try {
            List<HomeAggVO.SopHintVO> hints = activityLogMapper.countRepeatedTitles(
                    userId, DIM_WORK, BIZ_TASK_DONE,
                    today.minusDays(SOP_HINT_WINDOW_DAYS), SOP_HINT_MIN_COUNT, SOP_HINT_LIMIT);
            if (hints == null || hints.isEmpty()) {
                return;
            }

            // 已有同名 SOP（含草稿）则不再提示——重复工作已经沉淀过了
            Set<String> existing = new HashSet<>();
            List<WorkSop> sops = workSopMapper.selectList(new LambdaQueryWrapper<WorkSop>()
                    .select(WorkSop::getTitle)
                    .eq(WorkSop::getUserId, userId)
                    .eq(WorkSop::getDeleted, 0));
            for (WorkSop s : sops) {
                if (s.getTitle() != null) {
                    existing.add(s.getTitle().trim());
                }
            }
            hints.removeIf(h -> h.getTaskTitle() == null || existing.contains(h.getTaskTitle().trim()));

            vo.setSopHints(hints);
        } catch (Exception e) {
            log.warn("[首页聚合] SOP 提示查询失败，降级为空值: {}", e.getMessage());
        }
    }
}
