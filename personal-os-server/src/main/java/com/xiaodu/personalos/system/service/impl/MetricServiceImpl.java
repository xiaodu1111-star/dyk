package com.xiaodu.personalos.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xiaodu.personalos.common.exception.BizException;
import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.system.dto.MetricDefDTO;
import com.xiaodu.personalos.system.dto.MetricRecordDTO;
import com.xiaodu.personalos.system.entity.SysMetricDef;
import com.xiaodu.personalos.system.entity.SysMetricRecord;
import com.xiaodu.personalos.system.mapper.MetricDefMapper;
import com.xiaodu.personalos.system.mapper.MetricRecordMapper;
import com.xiaodu.personalos.system.result.SystemErrorCode;
import com.xiaodu.personalos.system.service.MetricService;
import com.xiaodu.personalos.system.vo.MetricDefVO;
import com.xiaodu.personalos.system.vo.MetricRecordVO;
import com.xiaodu.personalos.system.vo.MetricSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标引擎服务实现。
 *
 * <p>周期口径（周 key / 月 key / 归属日）一律走 {@link PeriodUtil}，本类不自算边界。</p>
 *
 * @author Kou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricServiceImpl implements MetricService {

    private final MetricDefMapper defMapper;
    private final MetricRecordMapper recordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetricDefVO create(MetricDefDTO dto) {
        Long exists = defMapper.selectCount(Wrappers.<SysMetricDef>lambdaQuery()
                .eq(SysMetricDef::getCode, dto.getCode()));
        if (exists != null && exists > 0) {
            throw SystemErrorCode.METRIC_CODE_EXISTS.toException();
        }

        SysMetricDef def = new SysMetricDef();
        def.setCode(dto.getCode());
        def.setName(dto.getName());
        def.setDimension(dto.getDimension());
        def.setValueType(StringUtils.hasText(dto.getValueType()) ? dto.getValueType() : "number");
        def.setUnit(dto.getUnit());
        def.setOptionsJson(dto.getOptionsJson());
        def.setAggType(StringUtils.hasText(dto.getAggType()) ? dto.getAggType() : "last");
        def.setTargetValue(dto.getTargetValue());
        def.setShowOnHome(dto.getShowOnHome() != null ? dto.getShowOnHome() : 1);
        def.setSortNo(dto.getSortNo() != null ? dto.getSortNo() : 0);
        defMapper.insert(def);

        log.info("新建指标定义: id={}, code={}, dimension={}", def.getId(), def.getCode(), def.getDimension());
        return toDefVO(def);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricDefVO> list(String dimension) {
        List<SysMetricDef> defs = defMapper.selectList(Wrappers.<SysMetricDef>lambdaQuery()
                .eq(StringUtils.hasText(dimension), SysMetricDef::getDimension, dimension)
                .orderByAsc(SysMetricDef::getSortNo)
                .orderByAsc(SysMetricDef::getId));
        return defs.stream().map(this::toDefVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MetricRecordVO record(String code, MetricRecordDTO dto) {
        SysMetricDef def = requireDef(code);

        // 值与类型匹配校验（number/duration 必须有数值；enum/bool 必须有文本值）
        validateValue(def, dto);

        LocalDate recordDate = dto.getRecordDate() != null ? dto.getRecordDate() : PeriodUtil.today();

        // upsert：同指标同归属日已有记录则更新值，否则插入
        SysMetricRecord existing = recordMapper.selectOne(Wrappers.<SysMetricRecord>lambdaQuery()
                .eq(SysMetricRecord::getMetricId, def.getId())
                .eq(SysMetricRecord::getRecordDate, recordDate)
                .orderByDesc(SysMetricRecord::getId)
                .last("LIMIT 1"));

        SysMetricRecord record;
        if (existing != null) {
            record = existing;
            record.setValueNum(dto.getValueNum());
            record.setValueText(dto.getValueText());
            record.setRefType(dto.getRefType());
            record.setRefId(dto.getRefId());
            record.setRemark(dto.getRemark());
            record.setRecordTime(LocalDateTime.now());
            recordMapper.updateById(record);
        } else {
            record = new SysMetricRecord();
            record.setMetricId(def.getId());
            record.setRecordDate(recordDate);
            record.setRecordTime(LocalDateTime.now());
            record.setValueNum(dto.getValueNum());
            record.setValueText(dto.getValueText());
            record.setRefType(dto.getRefType());
            record.setRefId(dto.getRefId());
            record.setRemark(dto.getRemark());
            recordMapper.insert(record);
        }

        log.info("指标记录写入: code={}, date={}, valueNum={}, valueText={}, id={}",
                code, recordDate, dto.getValueNum(), dto.getValueText(), record.getId());
        return toRecordVO(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MetricRecordVO> records(String code, LocalDate start, LocalDate end) {
        SysMetricDef def = requireDef(code);
        List<SysMetricRecord> records = recordMapper.selectList(Wrappers.<SysMetricRecord>lambdaQuery()
                .eq(SysMetricRecord::getMetricId, def.getId())
                .ge(start != null, SysMetricRecord::getRecordDate, start)
                .le(end != null, SysMetricRecord::getRecordDate, end)
                .orderByAsc(SysMetricRecord::getRecordDate)
                .orderByAsc(SysMetricRecord::getId));
        return records.stream().map(this::toRecordVO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MetricSummaryVO summary(String code, String periodKey) {
        SysMetricDef def = requireDef(code);

        LocalDate[] range;
        try {
            // 周期边界只走 PeriodUtil
            range = PeriodUtil.periodRange(periodKey);
        } catch (IllegalArgumentException e) {
            throw SystemErrorCode.METRIC_PERIOD_INVALID.toException();
        }

        List<SysMetricRecord> records = recordMapper.selectList(Wrappers.<SysMetricRecord>lambdaQuery()
                .eq(SysMetricRecord::getMetricId, def.getId())
                .ge(SysMetricRecord::getRecordDate, range[0])
                .le(SysMetricRecord::getRecordDate, range[1])
                .orderByAsc(SysMetricRecord::getRecordDate)
                .orderByAsc(SysMetricRecord::getId));

        MetricSummaryVO vo = new MetricSummaryVO();
        vo.setCode(def.getCode());
        vo.setName(def.getName());
        vo.setPeriodKey(periodKey);
        vo.setAggType(def.getAggType());
        vo.setCount(records.size());
        vo.setValue(aggregate(records, def.getAggType()));
        return vo;
    }

    /** 按编码查定义，不存在抛 15201。 */
    private SysMetricDef requireDef(String code) {
        SysMetricDef def = defMapper.selectOne(Wrappers.<SysMetricDef>lambdaQuery()
                .eq(SysMetricDef::getCode, code));
        if (def == null) {
            throw SystemErrorCode.METRIC_NOT_FOUND.toException();
        }
        return def;
    }

    /** 值与 value_type 匹配校验。 */
    private void validateValue(SysMetricDef def, MetricRecordDTO dto) {
        String valueType = def.getValueType();
        boolean needNum = "number".equals(valueType) || "duration".equals(valueType);
        boolean needText = "enum".equals(valueType) || "bool".equals(valueType);
        if (needNum && dto.getValueNum() == null) {
            throw SystemErrorCode.METRIC_VALUE_INVALID.toException();
        }
        if (needText && !StringUtils.hasText(dto.getValueText())) {
            throw SystemErrorCode.METRIC_VALUE_INVALID.toException();
        }
    }

    /** 按聚合方式计算周期值。 */
    private BigDecimal aggregate(List<SysMetricRecord> records, String aggType) {
        List<BigDecimal> values = records.stream()
                .map(SysMetricRecord::getValueNum)
                .filter(v -> v != null)
                .toList();
        if (values.isEmpty()) {
            return null;
        }
        String type = aggType != null ? aggType : "last";
        return switch (type) {
            case "sum" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "avg" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
            case "max" -> values.stream().reduce(BigDecimal::max).orElse(null);
            case "min" -> values.stream().reduce(BigDecimal::min).orElse(null);
            // 默认 last：取最后一条记录的值（列表已按日期、id 升序）
            default -> values.get(values.size() - 1);
        };
    }

    /** Def Entity → VO。 */
    private MetricDefVO toDefVO(SysMetricDef def) {
        MetricDefVO vo = new MetricDefVO();
        vo.setId(def.getId());
        vo.setCode(def.getCode());
        vo.setName(def.getName());
        vo.setDimension(def.getDimension());
        vo.setValueType(def.getValueType());
        vo.setUnit(def.getUnit());
        vo.setOptionsJson(def.getOptionsJson());
        vo.setAggType(def.getAggType());
        vo.setTargetValue(def.getTargetValue());
        vo.setShowOnHome(def.getShowOnHome());
        vo.setSortNo(def.getSortNo());
        vo.setCreateTime(def.getCreateTime());
        return vo;
    }

    /** Record Entity → VO。 */
    private MetricRecordVO toRecordVO(SysMetricRecord record) {
        MetricRecordVO vo = new MetricRecordVO();
        vo.setId(record.getId());
        vo.setMetricId(record.getMetricId());
        vo.setRecordDate(record.getRecordDate());
        vo.setRecordTime(record.getRecordTime());
        vo.setValueNum(record.getValueNum());
        vo.setValueText(record.getValueText());
        vo.setRefType(record.getRefType());
        vo.setRefId(record.getRefId());
        vo.setRemark(record.getRemark());
        return vo;
    }
}
