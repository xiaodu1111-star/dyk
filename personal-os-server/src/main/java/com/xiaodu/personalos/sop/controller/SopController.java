package com.xiaodu.personalos.sop.controller;

import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.sop.dto.SopRunFinishDTO;
import com.xiaodu.personalos.sop.dto.SopSaveDTO;
import com.xiaodu.personalos.sop.service.SopService;
import com.xiaodu.personalos.sop.vo.SopDetailVO;
import com.xiaodu.personalos.sop.vo.SopRunVO;
import com.xiaodu.personalos.sop.vo.SopVO;
import com.xiaodu.personalos.sop.vo.SopVersionDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * SOP 库接口（M2 核心差异化模块）。
 *
 * <p>用户边界由全局 Sa-Token 拦截器保证，{@code user_id} 在 Service 内取当前登录用户。</p>
 *
 * @author Alex
 */
@RestController
@RequestMapping("/api/sop")
@RequiredArgsConstructor
@Tag(name = "SOP 库", description = "工作流程沉淀 / 版本留底 / 按步执行")
public class SopController {

    private final SopService sopService;

    /** 创建 SOP（含步骤）。 */
    @PostMapping
    @Operation(summary = "创建 SOP")
    public R<Map<String, Long>> create(@Valid @RequestBody SopSaveDTO dto) {
        Long id = sopService.create(dto);
        return R.ok(Map.of("id", id));
    }

    /** 分页查询 SOP 列表。 */
    @GetMapping
    @Operation(summary = "分页查询 SOP 列表")
    public R<PageResult<SopVO>> list(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size) {
        return R.ok(sopService.list(keyword, category, page, size));
    }

    /** 查询 SOP 详情（含步骤 / 版本摘要 / 执行统计）。 */
    @GetMapping("/{id}")
    @Operation(summary = "查询 SOP 详情")
    public R<SopDetailVO> detail(@PathVariable Long id) {
        return R.ok(sopService.detail(id));
    }

    /** 编辑 SOP（已有执行记录时自动留版本快照）。 */
    @PutMapping("/{id}")
    @Operation(summary = "编辑 SOP")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SopSaveDTO dto) {
        sopService.update(id, dto);
        return R.ok();
    }

    /** 删除 SOP（逻辑删，级联删步骤）。 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除 SOP")
    public R<Void> delete(@PathVariable Long id) {
        sopService.delete(id);
        return R.ok();
    }

    /** 回看版本完整快照。 */
    @GetMapping("/{id}/versions/{versionId}")
    @Operation(summary = "回看版本完整快照")
    public R<SopVersionDetailVO> versionDetail(@PathVariable Long id, @PathVariable Long versionId) {
        return R.ok(sopService.versionDetail(id, versionId));
    }

    /** 发起执行。 */
    @PostMapping("/{id}/runs")
    @Operation(summary = "发起执行")
    public R<Map<String, Long>> startRun(@PathVariable Long id) {
        Long runId = sopService.startRun(id);
        return R.ok(Map.of("runId", runId));
    }

    /** 结束执行。 */
    @PutMapping("/runs/{runId}")
    @Operation(summary = "结束执行")
    public R<Void> finishRun(@PathVariable Long runId, @RequestBody SopRunFinishDTO dto) {
        sopService.finishRun(runId, dto);
        return R.ok();
    }

    /** 分页查询执行历史。 */
    @GetMapping("/{id}/runs")
    @Operation(summary = "分页查询执行历史")
    public R<PageResult<SopRunVO>> runHistory(@PathVariable Long id,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return R.ok(sopService.runHistory(id, page, size));
    }
}
