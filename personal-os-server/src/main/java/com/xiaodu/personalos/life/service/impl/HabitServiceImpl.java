package com.xiaodu.personalos.life.service.impl;

import cn.hutool.extra.pinyin.PinyinUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.life.dto.HabitCheckinDTO;
import com.xiaodu.personalos.life.dto.HabitCreateDTO;
import com.xiaodu.personalos.life.dto.QuickRecordDTO;
import com.xiaodu.personalos.life.dto.RecordConfirmDTO;
import com.xiaodu.personalos.life.result.LifeErrorCode;
import com.xiaodu.personalos.life.service.HabitService;
import com.xiaodu.personalos.life.vo.HabitVO;
import com.xiaodu.personalos.life.vo.QuickRecordVO;
import com.xiaodu.personalos.life.vo.RecordVO;
import com.xiaodu.personalos.life.vo.StreakVO;
import com.xiaodu.personalos.system.entity.SysMetricDef;
import com.xiaodu.personalos.system.entity.SysMetricRecord;
import com.xiaodu.personalos.system.mapper.MetricDefMapper;
import com.xiaodu.personalos.system.mapper.MetricRecordMapper;
import com.xiaodu.personalos.system.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import cn.dev33.satoken.stp.StpUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 生活域习惯服务实现（M3）。
 *
 * <p>习惯映射到 {@code sys_metric_def}，打卡记录写入 {@code sys_metric_record}，
 * <b>零建表</b>。本类不复用 {@code MetricService.record()}（其为同日 upsert 语义），
 * 而是自实现业务记录逻辑：</p>
 * <ul>
 *   <li>打卡型（value_type=bool, agg_type=last）：当日已存在记录 → 14002 幂等拒绝；否则插 value_num=1/value_text="1"</li>
 *   <li>计数型（value_type=number, agg_type=sum）：当日已有记录 → value_num += 增量；否则插 value_num=增量</li>
 * </ul>
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HabitServiceImpl implements HabitService {

    /** 生活域维度标识。 */
    private static final String DIMENSION_LIFE = "life";

    /** 打卡型标识。 */
    private static final String TYPE_CHECKIN = "checkin";

    /** 计数型标识。 */
    private static final String TYPE_COUNT = "count";

    /** 指标 value_type：布尔（打卡型）。 */
    private static final String VALUE_TYPE_BOOL = "bool";

    /** 指标 value_type：数值（计数型）。 */
    private static final String VALUE_TYPE_NUMBER = "number";

    /** 聚合方式：sum（计数型）。 */
    private static final String AGG_SUM = "sum";

    /** 聚合方式：last（打卡型）。 */
    private static final String AGG_LAST = "last";

    /** 习惯 code 前缀。 */
    private static final String CODE_PREFIX = "life_habit_";

    /** code 字段上限（VARCHAR(50)）。 */
    private static final int CODE_MAX_LEN = 50;

    /** code 生成/查重最大尝试次数。 */
    private static final int CODE_MAX_TRY = 1000;

    /** streak 回溯窗口天数（一次查询）。 */
    private static final int STREAK_WINDOW_DAYS = 90;

    /** 快捷记录解析正则：首词为习惯名前缀，可选尾随数字。 */
    private static final Pattern QUICK_PATTERN = Pattern.compile("^(\\S+?)\\s*([-+]?\\d+(?:\\.\\d+)?)?$");

    private final MetricDefMapper defMapper;
    private final MetricRecordMapper recordMapper;
    private final ActivityLogService activityLogService;

    // ---- 习惯 CRUD ------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HabitVO create(HabitCreateDTO dto) {
        String type = normalizeType(dto.getType());

        SysMetricDef def = new SysMetricDef();
        def.setCode(generateUniqueCode(dto.getName()));
        def.setName(dto.getName());
        def.setDimension(DIMENSION_LIFE);
        applyTypeMapping(def, type, dto.getUnit(), dto.getTargetValue());
        def.setShowOnHome(1);
        def.setSortNo(0);
        defMapper.insert(def);

        log.info("新建习惯: id={}, code={}, type={}", def.getId(), def.getCode(), type);
        return toHabitVO(def, PeriodUtil.today());
    }

    @Override
    @Transactional(readOnly = true)
    public List<HabitVO> list() {
        List<SysMetricDef> defs = defMapper.selectList(Wrappers.<SysMetricDef>lambdaQuery()
                .eq(SysMetricDef::getDimension, DIMENSION_LIFE)
                .orderByAsc(SysMetricDef::getSortNo)
                .orderByAsc(SysMetricDef::getId));

        LocalDate today = PeriodUtil.today();
        // 一次批量取今日全部 life 习惯的记录，避免逐条查询
        Map<Long, BigDecimal> todayValues = sumTodayValues(defs, today);

        List<HabitVO> result = new ArrayList<>(defs.size());
        for (SysMetricDef def : defs) {
            result.add(toHabitVO(def, today, todayValues.getOrDefault(def.getId(), BigDecimal.ZERO)));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HabitVO update(Long id, HabitCreateDTO dto) {
        SysMetricDef def = requireHabit(id);
        String type = normalizeType(dto.getType());

        def.setName(dto.getName());
        applyTypeMapping(def, type, dto.getUnit(), dto.getTargetValue());
        defMapper.updateById(def);

        log.info("修改习惯: id={}, name={}, type={}", id, dto.getName(), type);
        return toHabitVO(def, PeriodUtil.today());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysMetricDef def = requireHabit(id);
        // @TableLogic 逻辑删
        defMapper.deleteById(def.getId());
        log.info("删除习惯: id={}, code={}", id, def.getCode());
    }

    // ---- 打卡 -----------------------------------------------------------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordVO checkin(Long id, HabitCheckinDTO dto) {
        SysMetricDef def = requireHabit(id);
        String type = resolveType(def);

        BigDecimal delta = resolveDelta(dto);
        LocalDate today = PeriodUtil.today();

        SysMetricRecord existing = findRecord(def.getId(), today);

        if (TYPE_CHECKIN.equals(type)) {
            // 打卡型幂等：今日已打 → 14002
            if (existing != null) {
                throw LifeErrorCode.ALREADY_CHECKED.toException();
            }
            SysMetricRecord record = new SysMetricRecord();
            record.setMetricId(def.getId());
            record.setRecordDate(today);
            record.setRecordTime(LocalDateTime.now());
            record.setValueNum(BigDecimal.ONE);
            record.setValueText("1");
            recordMapper.insert(record);
            log.info("打卡成功: habitId={}, code={}, date={}", id, def.getCode(), today);
            // 打卡成功才写活动流（重复被拒已提前抛异常，不会走到这）
            activityLogService.log(StpUtil.getLoginIdAsLong(), DIMENSION_LIFE, "checkin", def.getName());
            return toRecordVO(record, def.getName());
        }

        // 计数型：当日累加
        if (delta.signum() <= 0) {
            throw LifeErrorCode.PARAM_INVALID.toException("计数型增量必须大于 0");
        }
        SysMetricRecord record;
        if (existing != null) {
            record = existing;
            BigDecimal base = existing.getValueNum() != null ? existing.getValueNum() : BigDecimal.ZERO;
            record.setValueNum(base.add(delta));
            record.setRecordTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else {
            record = new SysMetricRecord();
            record.setMetricId(def.getId());
            record.setRecordDate(today);
            record.setRecordTime(LocalDateTime.now());
            record.setValueNum(delta);
            recordMapper.insert(record);
        }
        log.info("计数打卡: habitId={}, code={}, delta={}, total={}", id, def.getCode(), delta, record.getValueNum());
        activityLogService.log(StpUtil.getLoginIdAsLong(), DIMENSION_LIFE, "checkin", def.getName());
        return toRecordVO(record, def.getName());
    }

    // ---- streak ---------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public StreakVO streak(Long id) {
        SysMetricDef def = requireHabit(id);
        String type = resolveType(def);
        LocalDate today = PeriodUtil.today();

        // 一次查询取最近 90 天，内存聚合为 Map<日期, 当日值>
        LocalDate start = today.minusDays(STREAK_WINDOW_DAYS - 1L);
        List<SysMetricRecord> records = recordMapper.selectList(Wrappers.<SysMetricRecord>lambdaQuery()
                .eq(SysMetricRecord::getMetricId, def.getId())
                .ge(SysMetricRecord::getRecordDate, start)
                .le(SysMetricRecord::getRecordDate, today));

        Map<LocalDate, BigDecimal> dayValue = new LinkedHashMap<>();
        for (SysMetricRecord r : records) {
            BigDecimal v = r.getValueNum() != null ? r.getValueNum() : BigDecimal.ZERO;
            dayValue.merge(r.getRecordDate(), v, BigDecimal::add);
        }

        // 起点：今天有效则从今天算，否则从昨天算（今天未打不算断）
        LocalDate cursor = isValidDay(dayValue.get(today), type) ? today : today.minusDays(1);
        int streak = 0;
        while (isValidDay(dayValue.get(cursor), type)) {
            streak++;
            cursor = cursor.minusDays(1);
            if (cursor.isBefore(start)) {
                // 窗口边界保护
                break;
            }
        }

        StreakVO vo = new StreakVO();
        vo.setHabitId(def.getId());
        vo.setName(def.getName());
        vo.setStreak(streak);
        return vo;
    }

    // ---- 快捷记录 -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public QuickRecordVO quickRecord(QuickRecordDTO dto) {
        String text = dto.getText() == null ? "" : dto.getText().trim();
        if (!StringUtils.hasText(text)) {
            throw LifeErrorCode.PARSE_FAILED.toException();
        }

        Matcher matcher = QUICK_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw LifeErrorCode.PARSE_FAILED.toException();
        }
        String keyword = matcher.group(1);
        String numberToken = matcher.group(2);
        if (!StringUtils.hasText(keyword)) {
            throw LifeErrorCode.PARSE_FAILED.toException();
        }

        // 在 life / common 维度中找 name 以关键词开头的第一条（按 sort_no、id 升序）
        SysMetricDef hit = defMapper.selectList(Wrappers.<SysMetricDef>lambdaQuery()
                        .in(SysMetricDef::getDimension, DIMENSION_LIFE, "common")
                        .likeRight(SysMetricDef::getName, keyword)
                        .orderByAsc(SysMetricDef::getSortNo)
                        .orderByAsc(SysMetricDef::getId))
                .stream().findFirst().orElse(null);

        if (hit == null) {
            throw LifeErrorCode.PARSE_FAILED.toException();
        }

        String type = resolveType(hit);
        BigDecimal value;
        if (TYPE_CHECKIN.equals(type)) {
            // 打卡型忽略数字，恒为 1
            value = BigDecimal.ONE;
        } else if (StringUtils.hasText(numberToken)) {
            value = new BigDecimal(numberToken);
        } else {
            // 计数型无数字 → 默认 1
            value = BigDecimal.ONE;
        }

        QuickRecordVO vo = new QuickRecordVO();
        vo.setMetricId(hit.getId());
        vo.setMetricName(hit.getName());
        vo.setValue(value);
        vo.setDate(PeriodUtil.today());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecordVO confirmRecord(RecordConfirmDTO dto) {
        SysMetricDef def = defMapper.selectById(dto.getMetricId());
        if (def == null || !DIMENSION_LIFE.equals(def.getDimension())) {
            throw LifeErrorCode.HABIT_NOT_FOUND.toException();
        }
        String type = resolveType(def);
        LocalDate date = dto.getDate() != null ? dto.getDate() : PeriodUtil.today();

        SysMetricRecord existing = findRecord(def.getId(), date);

        if (TYPE_CHECKIN.equals(type)) {
            // 打卡型：幂等，已存在 → 14002
            if (existing != null) {
                throw LifeErrorCode.ALREADY_CHECKED.toException();
            }
            SysMetricRecord record = new SysMetricRecord();
            record.setMetricId(def.getId());
            record.setRecordDate(date);
            record.setRecordTime(LocalDateTime.now());
            record.setValueNum(BigDecimal.ONE);
            record.setValueText("1");
            recordMapper.insert(record);
            log.info("快捷记录落库(打卡型): habitId={}, date={}", def.getId(), date);
            activityLogService.log(StpUtil.getLoginIdAsLong(), DIMENSION_LIFE, "checkin", def.getName());
            return toRecordVO(record, def.getName());
        }

        // 计数型：当日累加，value 为空默认 1
        BigDecimal delta = dto.getValue() != null ? dto.getValue() : BigDecimal.ONE;
        if (delta.signum() <= 0) {
            throw LifeErrorCode.PARAM_INVALID.toException("计数型记录值必须大于 0");
        }
        SysMetricRecord record;
        if (existing != null) {
            record = existing;
            BigDecimal base = existing.getValueNum() != null ? existing.getValueNum() : BigDecimal.ZERO;
            record.setValueNum(base.add(delta));
            record.setRecordTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else {
            record = new SysMetricRecord();
            record.setMetricId(def.getId());
            record.setRecordDate(date);
            record.setRecordTime(LocalDateTime.now());
            record.setValueNum(delta);
            recordMapper.insert(record);
        }
        log.info("快捷记录落库(计数型): habitId={}, date={}, delta={}, total={}",
                def.getId(), date, delta, record.getValueNum());
        activityLogService.log(StpUtil.getLoginIdAsLong(), DIMENSION_LIFE, "checkin", def.getName());
        return toRecordVO(record, def.getName());
    }

    // ---- 私有辅助 -------------------------------------------------------

    /** 按 id 查习惯定义（含 life 维度校验），不存在抛 14001。 */
    private SysMetricDef requireHabit(Long id) {
        SysMetricDef def = defMapper.selectById(id);
        if (def == null || !DIMENSION_LIFE.equals(def.getDimension())) {
            throw LifeErrorCode.HABIT_NOT_FOUND.toException();
        }
        return def;
    }

    /** 归一化习惯类型并校验，非法抛 14004。 */
    private String normalizeType(String type) {
        if (TYPE_CHECKIN.equals(type) || TYPE_COUNT.equals(type)) {
            return type;
        }
        throw LifeErrorCode.PARAM_INVALID.toException("习惯类型只能是 checkin 或 count");
    }

    /** 由指标定义反推习惯类型（bool → checkin，其余 → count）。 */
    private String resolveType(SysMetricDef def) {
        return VALUE_TYPE_BOOL.equals(def.getValueType()) ? TYPE_CHECKIN : TYPE_COUNT;
    }

    /** 解析计数型增量：为空默认 1。 */
    private BigDecimal resolveDelta(HabitCheckinDTO dto) {
        if (dto == null || dto.getValue() == null) {
            return BigDecimal.ONE;
        }
        return dto.getValue();
    }

    /** 按类型把习惯语义折算写入指标定义字段。 */
    private void applyTypeMapping(SysMetricDef def, String type, String unit, BigDecimal targetValue) {
        if (TYPE_CHECKIN.equals(type)) {
            def.setValueType(VALUE_TYPE_BOOL);
            def.setAggType(AGG_LAST);
            def.setTargetValue(BigDecimal.ONE);
            def.setUnit(null);
        } else {
            def.setValueType(VALUE_TYPE_NUMBER);
            def.setAggType(AGG_SUM);
            def.setUnit(StringUtils.hasText(unit) ? unit : null);
            def.setTargetValue(targetValue != null ? targetValue : null);
        }
    }

    /** 生成唯一 code：life_habit_<拼音/英文>，查重后追加 _2、_3…，并截断到 50。 */
    private String generateUniqueCode(String name) {
        String base = CODE_PREFIX + toAsciiSlug(name);
        base = truncate(base, CODE_MAX_LEN);
        String candidate = base;
        int suffix = 2;
        int tries = 0;
        while (codeExists(candidate)) {
            String tag = "_" + suffix;
            candidate = truncate(base, CODE_MAX_LEN - tag.length()) + tag;
            suffix++;
            tries++;
            if (tries > CODE_MAX_TRY) {
                // 理论不可达；兜底用时间戳后缀
                candidate = truncate(base, CODE_MAX_LEN - 8) + Math.abs(System.nanoTime() % 100_000_000L);
                break;
            }
        }
        return candidate;
    }

    /** name → 小写 ASCII 安全串：中文转拼音，仅保留 [a-z0-9]。 */
    private String toAsciiSlug(String name) {
        String ascii;
        try {
            ascii = PinyinUtil.getPinyin(name, "");
        } catch (Exception e) {
            log.warn("拼音转换失败，退化为原文过滤: name={}, err={}", name, e.getMessage());
            ascii = name;
        }
        if (!StringUtils.hasText(ascii)) {
            ascii = name;
        }
        String cleaned = ascii.toLowerCase().replaceAll("[^a-z0-9]", "");
        return StringUtils.hasText(cleaned) ? cleaned : "h";
    }

    /** 判断 code 是否已存在。 */
    private boolean codeExists(String code) {
        Long count = defMapper.selectCount(Wrappers.<SysMetricDef>lambdaQuery()
                .eq(SysMetricDef::getCode, code));
        return count != null && count > 0;
    }

    /** 安全截断字符串（null 安全）。 */
    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** 批量取给定定义在指定归属日的累计值（SUM），一次查询。 */
    private Map<Long, BigDecimal> sumTodayValues(List<SysMetricDef> defs, LocalDate date) {
        Map<Long, BigDecimal> result = new LinkedHashMap<>();
        if (defs.isEmpty()) {
            return result;
        }
        List<Long> ids = defs.stream().map(SysMetricDef::getId).toList();
        List<SysMetricRecord> records = recordMapper.selectList(Wrappers.<SysMetricRecord>lambdaQuery()
                .in(SysMetricRecord::getMetricId, ids)
                .eq(SysMetricRecord::getRecordDate, date));
        for (SysMetricRecord r : records) {
            BigDecimal v = r.getValueNum() != null ? r.getValueNum() : BigDecimal.ZERO;
            result.merge(r.getMetricId(), v, BigDecimal::add);
        }
        return result;
    }

    /** 查询某指标在指定归属日的记录（取最后一条）。 */
    private SysMetricRecord findRecord(Long metricId, LocalDate date) {
        return recordMapper.selectOne(Wrappers.<SysMetricRecord>lambdaQuery()
                .eq(SysMetricRecord::getMetricId, metricId)
                .eq(SysMetricRecord::getRecordDate, date)
                .orderByDesc(SysMetricRecord::getId)
                .last("LIMIT 1"));
    }

    /** 某日是否有效：打卡型 value>=1；计数型 SUM>0（value 为当日累计）。 */
    private boolean isValidDay(BigDecimal dayValue, String type) {
        if (dayValue == null) {
            return false;
        }
        if (TYPE_CHECKIN.equals(type)) {
            return dayValue.compareTo(BigDecimal.ONE) >= 0;
        }
        return dayValue.signum() > 0;
    }

    /** Entity → HabitVO（含今日状态，value 由调用方传入）。 */
    private HabitVO toHabitVO(SysMetricDef def, LocalDate today, BigDecimal todayValue) {
        String type = resolveType(def);
        BigDecimal value = todayValue != null ? todayValue : BigDecimal.ZERO;

        HabitVO vo = new HabitVO();
        vo.setId(def.getId());
        vo.setCode(def.getCode());
        vo.setName(def.getName());
        vo.setType(type);
        vo.setUnit(def.getUnit());
        vo.setTargetValue(def.getTargetValue());
        vo.setValue(value);
        vo.setDate(today);

        if (TYPE_CHECKIN.equals(type)) {
            vo.setDone(value.compareTo(BigDecimal.ONE) >= 0);
        } else {
            BigDecimal target = def.getTargetValue();
            vo.setDone(target != null && value.compareTo(target) >= 0);
        }
        return vo;
    }

    /** Entity → HabitVO（单条，自行查今日记录）。 */
    private HabitVO toHabitVO(SysMetricDef def, LocalDate today) {
        Map<Long, BigDecimal> map = sumTodayValues(List.of(def), today);
        return toHabitVO(def, today, map.getOrDefault(def.getId(), BigDecimal.ZERO));
    }

    /** Entity → RecordVO。 */
    private RecordVO toRecordVO(SysMetricRecord record, String metricName) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setMetricId(record.getMetricId());
        vo.setMetricName(metricName);
        vo.setDate(record.getRecordDate());
        vo.setRecordTime(record.getRecordTime());
        vo.setValue(record.getValueNum() != null ? record.getValueNum() : BigDecimal.ZERO);
        return vo;
    }
}
