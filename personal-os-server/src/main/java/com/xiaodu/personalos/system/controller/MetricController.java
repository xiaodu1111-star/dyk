package com.xiaodu.personalos.system.controller;

import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.system.dto.MetricDefDTO;
import com.xiaodu.personalos.system.dto.MetricRecordDTO;
import com.xiaodu.personalos.system.service.MetricService;
import com.xiaodu.personalos.system.vo.MetricDefVO;
import com.xiaodu.personalos.system.vo.MetricRecordVO;
import com.xiaodu.personalos.system.vo.MetricSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 指标引擎接口（M0 基础设施）。
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/system/metrics")
@RequiredArgsConstructor
@Tag(name = "指标引擎", description = "可配置量化追踪")
public class MetricController {

    private final MetricService metricService;

    /** 新增指标定义。 */
    @PostMapping
    @Operation(summary = "新增指标定义")
    public R<MetricDefVO> create(@Valid @RequestBody MetricDefDTO dto) {
        return R.ok(metricService.create(dto));
    }

    /** 查询指标定义。 */
    @GetMapping
    @Operation(summary = "查询指标定义")
    public R<List<MetricDefVO>> list(@RequestParam(required = false) String dimension) {
        return R.ok(metricService.list(dimension));
    }

    /** 写入一条记录（同日 upsert）。 */
    @PostMapping("/{code}/records")
    @Operation(summary = "写入指标记录")
    public R<MetricRecordVO> record(@PathVariable String code,
                                    @Valid @RequestBody MetricRecordDTO dto) {
        return R.ok(metricService.record(code, dto));
    }

    /** 查询记录（日期闭区间）。 */
    @GetMapping("/{code}/records")
    @Operation(summary = "查询指标记录")
    public R<List<MetricRecordVO>> records(@PathVariable String code,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(metricService.records(code, start, end));
    }

    /** 周期汇总（period 为周 key 2026-W40 或月 key 2026-10）。 */
    @GetMapping("/{code}/summary")
    @Operation(summary = "指标周期汇总")
    public R<MetricSummaryVO> summary(@PathVariable String code,
                                      @RequestParam String period) {
        return R.ok(metricService.summary(code, period));
    }
}
