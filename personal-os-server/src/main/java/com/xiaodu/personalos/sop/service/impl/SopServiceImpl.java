package com.xiaodu.personalos.sop.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.sop.dto.SopRunFinishDTO;
import com.xiaodu.personalos.sop.dto.SopSaveDTO;
import com.xiaodu.personalos.sop.dto.SopStepDTO;
import com.xiaodu.personalos.sop.entity.WorkSop;
import com.xiaodu.personalos.sop.entity.WorkSopLog;
import com.xiaodu.personalos.sop.entity.WorkSopStep;
import com.xiaodu.personalos.sop.entity.WorkSopVersion;
import com.xiaodu.personalos.sop.mapper.WorkSopLogMapper;
import com.xiaodu.personalos.sop.mapper.WorkSopMapper;
import com.xiaodu.personalos.sop.mapper.WorkSopStepMapper;
import com.xiaodu.personalos.sop.mapper.WorkSopVersionMapper;
import com.xiaodu.personalos.sop.result.SopErrorCode;
import com.xiaodu.personalos.sop.service.SopService;
import com.xiaodu.personalos.sop.vo.SopDetailVO;
import com.xiaodu.personalos.sop.vo.SopRunVO;
import com.xiaodu.personalos.sop.vo.SopStatsVO;
import com.xiaodu.personalos.sop.vo.SopStepVO;
import com.xiaodu.personalos.sop.vo.SopVO;
import com.xiaodu.personalos.sop.vo.SopVersionDetailVO;
import com.xiaodu.personalos.sop.vo.SopVersionVO;
import com.xiaodu.personalos.system.entity.ActActivityLog;
import com.xiaodu.personalos.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SOP 库服务实现（M2）。
 *
 * <p>铁律：所有查询强制拼 {@code user_id}；写操作统一 {@code rollbackFor = Exception.class}；
 * 只读操作 {@code readOnly = true}。</p>
 *
 * @author Alex
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SopServiceImpl implements SopService {

    /** 默认页码。 */
    private static final long DEFAULT_PAGE = 1L;

    /** 默认每页条数。 */
    private static final long DEFAULT_SIZE = 10L;

    /** 最大每页条数。 */
    private static final long MAX_SIZE = 100L;

    /** 初始版本号。 */
    private static final String INIT_VERSION = "v1.0";

    /** 版本号递增步长。 */
    private static final BigDecimal VERSION_STEP = new BigDecimal("0.1");

    /** 快照 / 版本号前缀。 */
    private static final String VERSION_PREFIX = "v";

    /** 版本号解析失败时的基线（v1.0 的数字部分）。 */
    private static final BigDecimal INIT_VERSION_FALLBACK = new BigDecimal("1.0");

    private final WorkSopMapper sopMapper;
    private final WorkSopStepMapper stepMapper;
    private final WorkSopVersionMapper versionMapper;
    private final WorkSopLogMapper logMapper;
    private final ActivityLogService activityLogService;
    private final ObjectMapper objectMapper;

    // ---- 创建 -----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SopSaveDTO dto) {
        Long userId = currentUserId();

        WorkSop sop = new WorkSop();
        sop.setUserId(userId);
        sop.setTitle(dto.getTitle().trim());
        sop.setCategory(dto.getCategory());
        sop.setTriggerScene(dto.getTriggerScene());
        sop.setGoal(dto.getGoal());
        sop.setVersion(INIT_VERSION);
        sop.setUseCount(0);
        sop.setAvgMinutes(0);
        sop.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "draft");
        sop.setSourceTaskId(dto.getSourceTaskId());
        sop.setPinned(dto.getPinned() == null ? 0 : dto.getPinned());
        sopMapper.insert(sop);

        // 步骤：按入参顺序重排 step_no = 1..N
        replaceSteps(sop.getId(), dto.getSteps());

        log.info("创建 SOP: id={}, userId={}, title={}, stepCount={}",
                sop.getId(), userId, sop.getTitle(), sizeOf(dto.getSteps()));
        return sop.getId();
    }

    // ---- 列表 -----------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResult<SopVO> list(String keyword, String category, long page, long size) {
        Long userId = currentUserId();
        long safePage = page <= 0 ? DEFAULT_PAGE : page;
        long safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        // 排序：pinned desc → last_used_at desc(NULL 最后) → use_count desc → id desc
        // 说明：MySQL 无 NULLS LAST 语法，用「last_used_at IS NULL」升序把 NULL 排到最后，
        // 再由随后的 last_used_at desc 对非 NULL 值降序。
        QueryWrapper<WorkSop> wrapper = Wrappers.<WorkSop>query()
                .eq("user_id", userId)
                .like(StringUtils.hasText(keyword), "title", keyword)
                .eq(StringUtils.hasText(category), "category", category)
                .orderByDesc("pinned")
                .orderByAsc("last_used_at IS NULL")
                .orderByDesc("last_used_at")
                .orderByDesc("use_count")
                .orderByDesc("id");
        IPage<WorkSop> p = sopMapper.selectPage(new Page<>(safePage, safeSize), wrapper);

        // 批量取步骤数，避免 N+1
        List<Long> sopIds = p.getRecords().stream().map(WorkSop::getId).toList();
        Map<Long, Integer> stepCountMap = countSteps(sopIds);

        List<SopVO> records = p.getRecords().stream()
                .map(s -> toVO(s, stepCountMap.getOrDefault(s.getId(), 0)))
                .toList();
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ---- 详情 -----------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SopDetailVO detail(Long id) {
        Long userId = currentUserId();
        WorkSop sop = requireOwnedSop(id, userId);

        SopDetailVO vo = new SopDetailVO();
        vo.setId(sop.getId());
        vo.setTitle(sop.getTitle());
        vo.setCategory(sop.getCategory());
        vo.setTriggerScene(sop.getTriggerScene());
        vo.setGoal(sop.getGoal());
        vo.setVersion(sop.getVersion());
        vo.setUseCount(sop.getUseCount());
        vo.setAvgMinutes(sop.getAvgMinutes());
        vo.setLastUsedAt(sop.getLastUsedAt());
        vo.setStatus(sop.getStatus());
        vo.setPinned(sop.getPinned());
        vo.setSourceTaskId(sop.getSourceTaskId());
        vo.setCreateTime(sop.getCreateTime());
        vo.setUpdateTime(sop.getUpdateTime());

        // 步骤
        List<WorkSopStep> steps = listSteps(sop.getId());
        vo.setSteps(steps.stream().map(this::toStepVO).toList());

        // 版本摘要（倒序，不含 content_json）
        List<WorkSopVersion> versions = versionMapper.selectList(Wrappers.<WorkSopVersion>lambdaQuery()
                .eq(WorkSopVersion::getSopId, sop.getId())
                .orderByDesc(WorkSopVersion::getId));
        vo.setVersions(versions.stream().map(this::toVersionVO).toList());

        // stats
        vo.setStats(buildStats(sop));

        return vo;
    }

    // ---- 编辑 -----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SopSaveDTO dto) {
        Long userId = currentUserId();
        WorkSop sop = requireOwnedSop(id, userId);

        // 规则 1：若该 SOP 已有执行记录 → 先写旧内容快照 + 版本号 +0.1
        Long runCount = logMapper.selectCount(Wrappers.<WorkSopLog>lambdaQuery()
                .eq(WorkSopLog::getSopId, id));
        boolean hasRuns = runCount != null && runCount > 0;
        if (hasRuns) {
            String nextVersion = bumpVersion(sop.getVersion());
            WorkSopVersion snapshot = new WorkSopVersion();
            snapshot.setSopId(sop.getId());
            snapshot.setVersion(sop.getVersion());
            snapshot.setContentJson(buildSnapshotJson(sop));
            snapshot.setChangeNote(dto.getChangeNote());
            versionMapper.insert(snapshot);

            sop.setVersion(nextVersion);
            log.info("编辑留版本: sopId={}, 快照版本={}, 新版本={}", id, snapshot.getVersion(), nextVersion);
        }

        // 更新主表字段
        sop.setTitle(dto.getTitle().trim());
        sop.setCategory(dto.getCategory());
        sop.setTriggerScene(dto.getTriggerScene());
        sop.setGoal(dto.getGoal());
        if (StringUtils.hasText(dto.getStatus())) {
            sop.setStatus(dto.getStatus());
        }
        if (dto.getPinned() != null) {
            sop.setPinned(dto.getPinned());
        }
        if (dto.getSourceTaskId() != null) {
            sop.setSourceTaskId(dto.getSourceTaskId());
        }
        sopMapper.updateById(sop);

        // 规则 2：步骤覆盖式全量替换
        replaceSteps(sop.getId(), dto.getSteps());

        log.info("编辑 SOP: id={}, userId={}, hasRuns={}", id, userId, hasRuns);
    }

    // ---- 删除 -----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Long userId = currentUserId();
        WorkSop sop = requireOwnedSop(id, userId);

        // 逻辑删主表（@TableLogic 自动置 deleted=1）
        sopMapper.deleteById(sop.getId());
        // 级联逻辑删步骤：work_sop_step 无 deleted 字段，采用物理删除（覆盖式更新语义一致）
        stepMapper.delete(Wrappers.<WorkSopStep>lambdaQuery().eq(WorkSopStep::getSopId, sop.getId()));

        log.info("删除 SOP: id={}, userId={}", id, userId);
    }

    // ---- 版本回看 -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public SopVersionDetailVO versionDetail(Long sopId, Long versionId) {
        Long userId = currentUserId();
        requireOwnedSop(sopId, userId);

        WorkSopVersion version = versionMapper.selectOne(Wrappers.<WorkSopVersion>lambdaQuery()
                .eq(WorkSopVersion::getId, versionId)
                .eq(WorkSopVersion::getSopId, sopId));
        if (version == null) {
            throw SopErrorCode.SOP_NOT_FOUND.toException("版本记录不存在");
        }

        SopVersionDetailVO vo = new SopVersionDetailVO();
        vo.setId(version.getId());
        vo.setSopId(version.getSopId());
        vo.setVersion(version.getVersion());
        vo.setChangeNote(version.getChangeNote());
        vo.setCreateTime(version.getCreateTime());
        vo.setContentJson(version.getContentJson());

        // 解析快照 JSON 为结构化字段
        parseSnapshotInto(version.getContentJson(), vo);
        return vo;
    }

    // ---- 执行 -----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startRun(Long sopId) {
        Long userId = currentUserId();
        requireOwnedSop(sopId, userId);

        WorkSopLog runLog = new WorkSopLog();
        runLog.setSopId(sopId);
        runLog.setStartedAt(LocalDateTime.now());
        logMapper.insert(runLog);

        log.info("发起执行: sopId={}, runId={}, userId={}", sopId, runLog.getId(), userId);
        return runLog.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishRun(Long runId, SopRunFinishDTO dto) {
        Long userId = currentUserId();

        WorkSopLog runLog = logMapper.selectById(runId);
        if (runLog == null) {
            throw SopErrorCode.PARAM_INVALID.toException("执行记录不存在");
        }
        // 校验 SOP 归属；不存在或不属于当前用户 → SOP_NOT_FOUND
        WorkSop sop = requireOwnedSop(runLog.getSopId(), userId);

        if (!Boolean.TRUE.equals(dto.getFinished())) {
            throw SopErrorCode.PARAM_INVALID.toException("finished 必须为 true 才能结束执行");
        }
        if (runLog.getFinishedAt() != null) {
            throw SopErrorCode.RUN_ALREADY_FINISHED.toException();
        }

        LocalDateTime now = LocalDateTime.now();
        Integer costMin = dto.getCostMin();
        if (costMin == null) {
            costMin = calcCostMinutes(runLog.getStartedAt(), now);
        }
        if (costMin < 1) {
            costMin = 1;
        }

        // 回写本次执行
        runLog.setFinishedAt(now);
        runLog.setCostMin(costMin);
        runLog.setStuckStep(dto.getStuckStep());
        runLog.setDeviation(dto.getDeviation());
        logMapper.updateById(runLog);

        // 聚合：use_count + 1、avg_minutes 重算、last_used_at = now
        int newUseCount = (sop.getUseCount() == null ? 0 : sop.getUseCount()) + 1;
        int avgMinutes = calcAvgMinutes(sop.getId());

        sop.setUseCount(newUseCount);
        sop.setAvgMinutes(avgMinutes);
        sop.setLastUsedAt(now);
        sopMapper.updateById(sop);

        // 活动流旁路写入
        ActActivityLog entry = new ActActivityLog();
        entry.setUserId(userId);
        entry.setDimension("work");
        entry.setBizType("sop_run");
        entry.setTitle(sop.getTitle());
        entry.setDurationMin(costMin);
        entry.setRefType("sop");
        entry.setRefId(sop.getId());
        entry.setOccurredAt(now);
        activityLogService.log(entry);

        log.info("结束执行: sopId={}, runId={}, costMin={}, useCount={}, avgMinutes={}",
                sop.getId(), runId, costMin, newUseCount, avgMinutes);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SopRunVO> runHistory(Long sopId, long page, long size) {
        Long userId = currentUserId();
        requireOwnedSop(sopId, userId);

        long safePage = page <= 0 ? DEFAULT_PAGE : page;
        long safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        IPage<WorkSopLog> p = logMapper.selectPage(new Page<>(safePage, safeSize),
                Wrappers.<WorkSopLog>lambdaQuery()
                        .eq(WorkSopLog::getSopId, sopId)
                        .orderByDesc(WorkSopLog::getStartedAt)
                        .orderByDesc(WorkSopLog::getId));

        List<SopRunVO> records = p.getRecords().stream().map(this::toRunVO).toList();
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ---- 内部：归属校验 -------------------------------------------------

    /** 取当前登录用户 id。 */
    private Long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /** 校验 SOP 存在且属于当前用户，否则 10101。 */
    private WorkSop requireOwnedSop(Long sopId, Long userId) {
        WorkSop sop = sopMapper.selectOne(Wrappers.<WorkSop>lambdaQuery()
                .eq(WorkSop::getId, sopId)
                .eq(WorkSop::getUserId, userId));
        if (sop == null) {
            throw SopErrorCode.SOP_NOT_FOUND.toException();
        }
        return sop;
    }

    // ---- 内部：步骤 -----------------------------------------------------

    /**
     * 覆盖式全量替换步骤：删旧插新，step_no 重排为 1..N。
     *
     * <p>规则：步骤列表为空（无步骤）或某步标题为空 → 10102。</p>
     *
     * @param sopId SOP id
     * @param steps 入参步骤
     */
    private void replaceSteps(Long sopId, List<SopStepDTO> steps) {
        // 规则：无步骤 → 10102
        if (steps == null || steps.isEmpty()) {
            throw SopErrorCode.STEP_INVALID.toException("至少需要一个步骤");
        }

        // 先删旧步骤（work_sop_step 无逻辑删除字段）
        stepMapper.delete(Wrappers.<WorkSopStep>lambdaQuery().eq(WorkSopStep::getSopId, sopId));

        int no = 1;
        for (SopStepDTO dto : steps) {
            // 规则：步骤标题为空 → 10102
            if (dto == null || !StringUtils.hasText(dto.getTitle())) {
                throw SopErrorCode.STEP_INVALID.toException("步骤标题不能为空");
            }
            WorkSopStep step = new WorkSopStep();
            step.setSopId(sopId);
            step.setStepNo(no++);
            step.setTitle(dto.getTitle().trim());
            step.setDetail(dto.getDetail());
            step.setTip(dto.getTip());
            step.setEstimateMin(dto.getEstimateMin());
            stepMapper.insert(step);
        }
    }

    /** 查询某 SOP 的步骤（按 step_no 升序）。 */
    private List<WorkSopStep> listSteps(Long sopId) {
        return stepMapper.selectList(Wrappers.<WorkSopStep>lambdaQuery()
                .eq(WorkSopStep::getSopId, sopId)
                .orderByAsc(WorkSopStep::getStepNo));
    }

    /** 批量统计各 SOP 的步骤数。 */
    private Map<Long, Integer> countSteps(List<Long> sopIds) {
        Map<Long, Integer> result = new HashMap<>();
        if (sopIds == null || sopIds.isEmpty()) {
            return result;
        }
        List<WorkSopStep> all = stepMapper.selectList(Wrappers.<WorkSopStep>lambdaQuery()
                .in(WorkSopStep::getSopId, sopIds));
        for (WorkSopStep step : all) {
            result.merge(step.getSopId(), 1, Integer::sum);
        }
        return result;
    }

    // ---- 内部：版本号 ---------------------------------------------------

    /**
     * 版本号 +0.1（BigDecimal 语义，保留 1 位小数）。
     *
     * <p>入参可能带 {@code v} 前缀（如 {@code v1.0}）或裸数字（如 {@code 1.0}），
     * 亦可能非法；非法或为空时回退到 {@link #INIT_VERSION} 再递增，避免产生 {@code v1.10} 这类错误。</p>
     *
     * @param current 当前版本号
     * @return 递增后的版本号（带 v 前缀）
     */
    private String bumpVersion(String current) {
        BigDecimal base = INIT_VERSION_FALLBACK;
        if (StringUtils.hasText(current)) {
            String raw = current.trim();
            if (raw.startsWith(VERSION_PREFIX)) {
                raw = raw.substring(VERSION_PREFIX.length());
            }
            try {
                base = new BigDecimal(raw);
            } catch (NumberFormatException ex) {
                log.warn("版本号非法，回退基线: current={}", current);
                base = INIT_VERSION_FALLBACK;
            }
        }
        BigDecimal next = base.add(VERSION_STEP).setScale(1, RoundingMode.HALF_UP);
        return VERSION_PREFIX + next.toPlainString();
    }

    // ---- 内部：聚合计算 -------------------------------------------------

    /**
     * 计算自动耗时：started_at → finished_at 的分钟数，向上取整，最小 1。
     *
     * @param startedAt  开始时间
     * @param finishedAt 结束时间
     * @return 分钟数（≥1）
     */
    private int calcCostMinutes(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return 1;
        }
        long seconds = Duration.between(startedAt, finishedAt).getSeconds();
        if (seconds <= 0) {
            return 1;
        }
        long minutes = (seconds + 59) / 60;
        return (int) Math.max(minutes, 1);
    }

    /**
     * 重算 avg_minutes：该 SOP 全部已完成 run 的 cost_min 平均值（四舍五入取整）。
     *
     * @param sopId SOP id
     * @return 平均分钟；无已完成 run 时返回 0
     */
    private int calcAvgMinutes(Long sopId) {
        List<WorkSopLog> finished = logMapper.selectList(Wrappers.<WorkSopLog>lambdaQuery()
                .eq(WorkSopLog::getSopId, sopId)
                .isNotNull(WorkSopLog::getFinishedAt));
        List<Integer> costs = new ArrayList<>();
        for (WorkSopLog runLog : finished) {
            if (runLog.getCostMin() != null) {
                costs.add(runLog.getCostMin());
            }
        }
        if (costs.isEmpty()) {
            return 0;
        }
        double avg = costs.stream().mapToInt(Integer::intValue).average().orElse(0d);
        return (int) Math.round(avg);
    }

    /**
     * 构建执行统计。
     *
     * @param sop SOP 主表记录
     * @return 统计 VO
     */
    private SopStatsVO buildStats(WorkSop sop) {
        SopStatsVO stats = new SopStatsVO();
        List<WorkSopLog> finished = logMapper.selectList(Wrappers.<WorkSopLog>lambdaQuery()
                .eq(WorkSopLog::getSopId, sop.getId())
                .isNotNull(WorkSopLog::getFinishedAt));
        stats.setRunCount(finished.size());
        stats.setAvgMinutes(sop.getAvgMinutes());
        stats.setLastUsedAt(sop.getLastUsedAt());
        stats.setTopStuckStep(topStuckStep(finished));
        return stats;
    }

    /**
     * 统计出现次数最多的 stuck_step。
     *
     * @param finished 已完成的 run 列表
     * @return 最常卡住的步骤序号；无则 null
     */
    private Integer topStuckStep(List<WorkSopLog> finished) {
        Map<Integer, Integer> counter = new HashMap<>();
        for (WorkSopLog runLog : finished) {
            if (runLog.getStuckStep() != null) {
                counter.merge(runLog.getStuckStep(), 1, Integer::sum);
            }
        }
        return counter.entrySet().stream()
                .max(Comparator
                        .comparingInt((Map.Entry<Integer, Integer> e) -> e.getValue())
                        .thenComparingInt(e -> -e.getKey()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // ---- 内部：快照 JSON ------------------------------------------------

    /**
     * 构建 SOP 完整快照 JSON（含步骤）。
     *
     * @param sop SOP 主表记录
     * @return JSON 字符串
     */
    private String buildSnapshotJson(WorkSop sop) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("title", sop.getTitle());
        root.put("category", sop.getCategory());
        root.put("triggerScene", sop.getTriggerScene());
        root.put("goal", sop.getGoal());
        root.put("version", sop.getVersion());
        root.put("status", sop.getStatus());
        ArrayNode stepsNode = root.putArray("steps");
        for (WorkSopStep step : listSteps(sop.getId())) {
            ObjectNode node = stepsNode.addObject();
            node.put("stepNo", step.getStepNo());
            node.put("title", step.getTitle());
            node.put("detail", step.getDetail());
            node.put("tip", step.getTip());
            if (step.getEstimateMin() == null) {
                node.putNull("estimateMin");
            } else {
                node.put("estimateMin", step.getEstimateMin());
            }
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            log.warn("序列化 SOP 快照失败: sopId={}, err={}", sop.getId(), ex.getMessage());
            return "{}";
        }
    }

    /**
     * 将快照 JSON 解析回结构化 VO。
     *
     * @param contentJson 快照 JSON 字符串
     * @param vo          目标 VO
     */
    private void parseSnapshotInto(String contentJson, SopVersionDetailVO vo) {
        if (!StringUtils.hasText(contentJson)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            vo.setTitle(textOrNull(root, "title"));
            vo.setCategory(textOrNull(root, "category"));
            vo.setTriggerScene(textOrNull(root, "triggerScene"));
            vo.setGoal(textOrNull(root, "goal"));
            vo.setStatus(textOrNull(root, "status"));
            JsonNode stepsNode = root.path("steps");
            if (stepsNode.isArray()) {
                List<SopStepVO> steps = new ArrayList<>();
                for (JsonNode node : stepsNode) {
                    SopStepVO step = new SopStepVO();
                    step.setStepNo(node.path("stepNo").isMissingNode() ? null : node.path("stepNo").asInt());
                    step.setTitle(textOrNull(node, "title"));
                    step.setDetail(textOrNull(node, "detail"));
                    step.setTip(textOrNull(node, "tip"));
                    step.setEstimateMin(node.path("estimateMin").isNumber()
                            ? node.path("estimateMin").asInt() : null);
                    steps.add(step);
                }
                vo.setSteps(steps);
            }
        } catch (Exception ex) {
            log.warn("解析 SOP 快照失败: err={}", ex.getMessage());
        }
    }

    /** 读取 JSON 文本字段，缺省返回 null。 */
    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    // ---- 内部：Entity → VO ----------------------------------------------

    /** Entity → 列表卡 VO。 */
    private SopVO toVO(WorkSop sop, int stepCount) {
        SopVO vo = new SopVO();
        vo.setId(sop.getId());
        vo.setTitle(sop.getTitle());
        vo.setCategory(sop.getCategory());
        vo.setTriggerScene(sop.getTriggerScene());
        vo.setGoal(sop.getGoal());
        vo.setVersion(sop.getVersion());
        vo.setUseCount(sop.getUseCount());
        vo.setAvgMinutes(sop.getAvgMinutes());
        vo.setLastUsedAt(sop.getLastUsedAt());
        vo.setStatus(sop.getStatus());
        vo.setPinned(sop.getPinned());
        vo.setStepCount(stepCount);
        vo.setCreateTime(sop.getCreateTime());
        vo.setUpdateTime(sop.getUpdateTime());
        return vo;
    }

    /** Entity → 步骤 VO。 */
    private SopStepVO toStepVO(WorkSopStep step) {
        SopStepVO vo = new SopStepVO();
        vo.setStepNo(step.getStepNo());
        vo.setTitle(step.getTitle());
        vo.setDetail(step.getDetail());
        vo.setTip(step.getTip());
        vo.setEstimateMin(step.getEstimateMin());
        return vo;
    }

    /** Entity → 版本摘要 VO。 */
    private SopVersionVO toVersionVO(WorkSopVersion version) {
        SopVersionVO vo = new SopVersionVO();
        vo.setId(version.getId());
        vo.setVersion(version.getVersion());
        vo.setChangeNote(version.getChangeNote());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }

    /** Entity → 执行记录 VO。 */
    private SopRunVO toRunVO(WorkSopLog runLog) {
        SopRunVO vo = new SopRunVO();
        vo.setId(runLog.getId());
        vo.setSopId(runLog.getSopId());
        vo.setStartedAt(runLog.getStartedAt());
        vo.setFinishedAt(runLog.getFinishedAt());
        vo.setCostMin(runLog.getCostMin());
        vo.setStuckStep(runLog.getStuckStep());
        vo.setDeviation(runLog.getDeviation());
        return vo;
    }

    /** 步骤列表 size，null 安全。 */
    private int sizeOf(List<SopStepDTO> steps) {
        return steps == null ? 0 : steps.size();
    }
}
