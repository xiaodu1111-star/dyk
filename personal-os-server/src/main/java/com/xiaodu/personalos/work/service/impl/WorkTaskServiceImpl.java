package com.xiaodu.personalos.work.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.system.entity.SysTagRel;
import com.xiaodu.personalos.system.mapper.TagRelMapper;
import com.xiaodu.personalos.system.service.ActivityLogService;
import com.xiaodu.personalos.system.service.TagService;
import com.xiaodu.personalos.system.vo.TagVO;
import com.xiaodu.personalos.work.dto.TaskCreateDTO;
import com.xiaodu.personalos.work.dto.TaskStatusDTO;
import com.xiaodu.personalos.work.dto.TaskUpdateDTO;
import com.xiaodu.personalos.work.entity.WorkTask;
import com.xiaodu.personalos.work.mapper.WorkTaskMapper;
import com.xiaodu.personalos.work.result.WorkErrorCode;
import com.xiaodu.personalos.work.service.WorkTaskService;
import com.xiaodu.personalos.work.vo.TaskTagVO;
import com.xiaodu.personalos.work.vo.TaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作域任务服务实现（M1）。
 *
 * <p>关键口径：
 * <ul>
 *   <li>时间一律走 {@link PeriodUtil}，严禁 {@code LocalDate.now()} 或自拼边界。</li>
 *   <li>标签关联 bizType 恒为 {@code "task"}。</li>
 *   <li>三视图互斥：overdue 为 {@code dueAt < 今日 00:00}，today 为 {@code dueAt 为空 或 dueAt >= 今日 00:00}。</li>
 * </ul></p>
 *
 * @author Kou
 */
@Service
@RequiredArgsConstructor
public class WorkTaskServiceImpl implements WorkTaskService {

    /** 标签关联业务类型，恒为 task。 */
    private static final String BIZ_TYPE = "task";

    /** 是否完成判定用的未完成状态集合。 */
    private static final Set<String> UNFINISHED = Set.of("todo", "doing");

    /** 状态默认为 todo。 */
    private static final String STATUS_TODO = "todo";

    /** 状态：完成。 */
    private static final String STATUS_DONE = "done";

    /** 默认优先级：中。 */
    private static final int DEFAULT_PRIORITY = 2;

    /** 默认每页条数。 */
    private static final long DEFAULT_SIZE = 20L;

    /** 每页条数上限。 */
    private static final long MAX_SIZE = 100L;

    /**
     * 状态机（常量表驱动）。
     *
     * <p>注意：{@code done → todo} 是<b>合法</b>迁移（允许回退重开，前端二次确认后触发）；
     * {@code abandoned} 是严格终态，不接收任何目标状态。</p>
     */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "todo", Set.of("doing", "done", "abandoned"),
            "doing", Set.of("todo", "done", "abandoned"),
            "done", Set.of("todo"),
            "abandoned", Set.of()
    );

    private final WorkTaskMapper workTaskMapper;

    private final TagService tagService;

    private final ActivityLogService activityLogService;

    private final TagRelMapper tagRelMapper;

    // ---- 创建 ----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskCreateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();

        WorkTask task = new WorkTask();
        task.setUserId(userId);
        task.setParentId(0L);
        task.setTitle(dto.getTitle().trim());
        task.setDescription(dto.getDescription());
        task.setStatus(STATUS_TODO);
        task.setPriority(dto.getPriority() == null ? DEFAULT_PRIORITY : dto.getPriority());
        task.setDueAt(dto.getDueAt());
        task.setEstimateMin(dto.getEstimateMin());
        task.setActualMin(0);
        task.setSortNo(0);
        workTaskMapper.insert(task);

        // 标签绑定：bind 自身幂等，无需 diff
        if (dto.getTagIds() != null) {
            dto.getTagIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .forEach(tagId -> tagService.bind(tagId, BIZ_TYPE, task.getId()));
        }
        return task.getId();
    }

    // ---- 分页查询 ------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResult<TaskVO> page(String view, String status, long page, long size) {
        long userId = StpUtil.getLoginIdAsLong();
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        LocalDateTime todayStart = PeriodUtil.dayStart(PeriodUtil.today());

        LambdaQueryWrapper<WorkTask> q = Wrappers.<WorkTask>lambdaQuery()
                .eq(WorkTask::getUserId, userId);

        String safeView = StringUtils.hasText(view) ? view : "all";
        if ("overdue".equals(safeView)) {
            q.in(WorkTask::getStatus, UNFINISHED)
                    .isNotNull(WorkTask::getDueAt)
                    .lt(WorkTask::getDueAt, todayStart);
        } else if ("today".equals(safeView)) {
            q.in(WorkTask::getStatus, UNFINISHED)
                    .and(w -> w.isNull(WorkTask::getDueAt).or().ge(WorkTask::getDueAt, todayStart));
        }
        // view = all 或非法 → 不加 status/时间条件（deleted=0 由 @TableLogic 自动加）

        if (StringUtils.hasText(status)) {
            q.eq(WorkTask::getStatus, status);
        }

        // 排序：单条 last 串，避免拼出两个 ORDER BY。（due_at IS NULL）得 0/1 → NULL 排最后
        q.last("ORDER BY priority ASC, (due_at IS NULL) ASC, due_at ASC, create_time DESC");

        Page<WorkTask> result = workTaskMapper.selectPage(new Page<>(safePage, safeSize), q);

        List<TaskVO> records = toVOList(result.getRecords(), userId, todayStart);
        PageResult<TaskVO> pr = new PageResult<>();
        pr.setRecords(records);
        pr.setTotal(result.getTotal());
        pr.setPage(result.getCurrent());
        pr.setSize(result.getSize());
        return pr;
    }

    // ---- 详情 ----------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TaskVO detail(Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkTask task = requireOwnedTask(id, userId);
        return toVO(task, loadTagMap(), loadTagIdsByTask(task.getId()), PeriodUtil.dayStart(PeriodUtil.today()));
    }

    // ---- 全量更新 ------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TaskUpdateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkTask task = requireOwnedTask(id, userId);

        // 全量覆盖语义：用 LambdaUpdateWrapper 显式 set，保证 description / dueAt / estimateMin
        // 在被清空（传 null）时也能落库（updateById 默认忽略 null 字段，不清空）。
        workTaskMapper.update(null, Wrappers.<WorkTask>lambdaUpdate()
                .eq(WorkTask::getId, task.getId())
                .set(WorkTask::getTitle, dto.getTitle().trim())
                .set(WorkTask::getDescription, dto.getDescription())
                .set(WorkTask::getPriority, dto.getPriority() == null ? DEFAULT_PRIORITY : dto.getPriority())
                .set(WorkTask::getDueAt, dto.getDueAt())
                .set(WorkTask::getEstimateMin, dto.getEstimateMin())
                .set(WorkTask::getUpdateTime, LocalDateTime.now()));

        syncTags(task.getId(), dto.getTagIds());
    }

    // ---- 状态流转 ------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, TaskStatusDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkTask task = requireOwnedTask(id, userId);

        String current = task.getStatus();
        String target = dto.getStatus();

        // 相同状态：幂等返回，不写库、不报错
        if (Objects.equals(current, target)) {
            return;
        }

        Set<String> allowed = TRANSITIONS.getOrDefault(current, Collections.emptySet());
        if (!allowed.contains(target)) {
            throw WorkErrorCode.INVALID_STATUS_TRANSITION.toException();
        }

        // 用 LambdaUpdateWrapper 显式 set：保证 doneAt 清空（done → todo）时能落库 null
        LambdaUpdateWrapper<WorkTask> update = Wrappers.<WorkTask>lambdaUpdate()
                .eq(WorkTask::getId, task.getId())
                .set(WorkTask::getStatus, target)
                .set(WorkTask::getUpdateTime, LocalDateTime.now());
        if (STATUS_DONE.equals(target)) {
            update.set(WorkTask::getDoneAt, LocalDateTime.now());
        } else if (STATUS_TODO.equals(target)) {
            // 从 done 回退重开：清空完成时刻，不写活动流
            update.set(WorkTask::getDoneAt, null);
        }
        workTaskMapper.update(null, update);

        // 完成 → 事务内、update 之后写活动流（内部吞异常，勿 try/catch）
        if (STATUS_DONE.equals(target)) {
            activityLogService.log(userId, "work", "task_done", task.getTitle());
        }
    }

    // ---- 删除 ----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        WorkTask task = requireOwnedTask(id, userId);

        // sys_tag_rel 物理表无逻辑删除，必须先清空关联，否则 use_count 永久偏高 + 孤儿行
        syncTags(task.getId(), List.of());
        workTaskMapper.deleteById(task.getId());
    }

    // ---- 首页聚合预留 --------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public long countToday(long userId) {
        LocalDateTime todayStart = PeriodUtil.dayStart(PeriodUtil.today());
        return workTaskMapper.selectCount(Wrappers.<WorkTask>lambdaQuery()
                .eq(WorkTask::getUserId, userId)
                .in(WorkTask::getStatus, UNFINISHED)
                .and(w -> w.isNull(WorkTask::getDueAt).or().ge(WorkTask::getDueAt, todayStart)));
    }

    @Override
    @Transactional(readOnly = true)
    public long countTodayDone(long userId) {
        LocalDateTime todayStart = PeriodUtil.dayStart(PeriodUtil.today());
        LocalDateTime todayEnd = PeriodUtil.dayEnd(PeriodUtil.today());
        return workTaskMapper.selectCount(Wrappers.<WorkTask>lambdaQuery()
                .eq(WorkTask::getUserId, userId)
                .eq(WorkTask::getStatus, STATUS_DONE)
                .ge(WorkTask::getDoneAt, todayStart)
                .lt(WorkTask::getDoneAt, todayEnd));
    }

    @Override
    @Transactional(readOnly = true)
    public long countOverdue(long userId) {
        LocalDateTime todayStart = PeriodUtil.dayStart(PeriodUtil.today());
        return workTaskMapper.selectCount(Wrappers.<WorkTask>lambdaQuery()
                .eq(WorkTask::getUserId, userId)
                .in(WorkTask::getStatus, UNFINISHED)
                .isNotNull(WorkTask::getDueAt)
                .lt(WorkTask::getDueAt, todayStart));
    }

    // ---- 内部工具 ------------------------------------------------------

    /**
     * 按 id 取当前用户的任务，做归属校验。
     *
     * <p>不区分「不存在」与「不属于你」，统一 10001，避免信息泄露。
     * {@code @TableLogic} 自动过滤已删任务。</p>
     */
    private WorkTask requireOwnedTask(Long id, long userId) {
        WorkTask task = workTaskMapper.selectById(id);
        if (task == null || !Objects.equals(task.getUserId(), userId)) {
            throw WorkErrorCode.TASK_NOT_FOUND.toException();
        }
        return task;
    }

    /** 批量组装 VO 列表（非 N+1：全量标签一次 + 本页关联一次）。 */
    private List<TaskVO> toVOList(List<WorkTask> tasks, long userId, LocalDateTime todayStart) {
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, TagVO> tagMap = loadTagMap();
        List<Long> taskIds = tasks.stream().map(WorkTask::getId).collect(Collectors.toList());

        // 一次性查本页所有任务的标签关联，按 bizId 分组
        Map<Long, Set<Long>> taskTagIds = new HashMap<>();
        if (!taskIds.isEmpty()) {
            List<SysTagRel> rels = tagRelMapper.selectList(Wrappers.<SysTagRel>lambdaQuery()
                    .eq(SysTagRel::getBizType, BIZ_TYPE)
                    .in(SysTagRel::getBizId, taskIds));
            for (SysTagRel rel : rels) {
                taskTagIds.computeIfAbsent(rel.getBizId(), k -> new HashSet<>()).add(rel.getTagId());
            }
        }

        List<TaskVO> list = new ArrayList<>(tasks.size());
        for (WorkTask t : tasks) {
            Set<Long> ids = taskTagIds.getOrDefault(t.getId(), Collections.emptySet());
            list.add(toVO(t, tagMap, ids, todayStart));
        }
        return list;
    }

    /** 全量标签 → Map（id → TagVO），列表与详情复用一次查询。 */
    private Map<Long, TagVO> loadTagMap() {
        List<TagVO> allTags = tagService.list(null, null);
        Map<Long, TagVO> map = new HashMap<>();
        if (allTags != null) {
            for (TagVO tag : allTags) {
                map.put(tag.getId(), tag);
            }
        }
        return map;
    }

    /** 单任务查标签关联 id 集合。 */
    private Set<Long> loadTagIdsByTask(Long taskId) {
        return tagRelMapper.selectList(Wrappers.<SysTagRel>lambdaQuery()
                        .eq(SysTagRel::getBizType, BIZ_TYPE)
                        .eq(SysTagRel::getBizId, taskId))
                .stream()
                .map(SysTagRel::getTagId)
                .collect(Collectors.toSet());
    }

    /** 组装单个 VO。 */
    private TaskVO toVO(WorkTask t, Map<Long, TagVO> tagMap, Set<Long> myTagIds, LocalDateTime todayStart) {
        TaskVO vo = new TaskVO();
        vo.setId(t.getId());
        vo.setUserId(t.getUserId());
        vo.setTitle(t.getTitle());
        vo.setDescription(t.getDescription());
        vo.setStatus(t.getStatus());
        vo.setPriority(t.getPriority());
        vo.setPlanStart(t.getPlanStart());
        vo.setDueAt(t.getDueAt());
        vo.setEstimateMin(t.getEstimateMin());
        vo.setActualMin(t.getActualMin());
        vo.setDoneAt(t.getDoneAt());
        vo.setSortNo(t.getSortNo());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());

        // 逾期判定：未完成 且 dueAt 非空 且 dueAt < 今日 00:00
        boolean overdue = t.getDueAt() != null
                && UNFINISHED.contains(t.getStatus())
                && t.getDueAt().isBefore(todayStart);
        vo.setOverdue(overdue);

        List<TaskTagVO> tags = new ArrayList<>();
        if (myTagIds != null) {
            for (Long tagId : myTagIds) {
                TagVO tag = tagMap.get(tagId);
                if (tag == null) {
                    continue;
                }
                TaskTagVO tv = new TaskTagVO();
                tv.setId(tag.getId());
                tv.setName(tag.getName());
                tv.setColor(tag.getColor());
                tags.add(tv);
            }
        }
        vo.setTags(tags);
        return vo;
    }

    /**
     * 标签关联 diff 同步（bizType 恒为 task）。
     *
     * @param taskId       任务 id
     * @param targetTagIds 目标标签 id 集合，null 视为空集合
     */
    private void syncTags(Long taskId, Collection<Long> targetTagIds) {
        Set<Long> target = targetTagIds == null
                ? new HashSet<>()
                : targetTagIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> current = loadTagIdsByTask(taskId);

        target.stream()
                .filter(t -> !current.contains(t))
                .forEach(t -> tagService.bind(t, BIZ_TYPE, taskId));
        current.stream()
                .filter(c -> !target.contains(c))
                .forEach(c -> tagService.unbind(c, BIZ_TYPE, taskId));
    }
}
