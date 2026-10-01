package com.xiaodu.personalos.work.controller;

import com.xiaodu.personalos.common.result.PageResult;
import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.work.dto.TaskCreateDTO;
import com.xiaodu.personalos.work.dto.TaskStatusDTO;
import com.xiaodu.personalos.work.dto.TaskUpdateDTO;
import com.xiaodu.personalos.work.service.WorkTaskService;
import com.xiaodu.personalos.work.vo.TaskVO;
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

/**
 * 工作域任务接口（M1）。
 *
 * <p>Controller 不碰 Entity、不 try/catch 业务异常（交给 {@code GlobalExceptionHandler}）。</p>
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/work/tasks")
@RequiredArgsConstructor
@Tag(name = "工作域任务", description = "任务 CRUD / 状态流转 / 三视图 / 标签")
public class WorkTaskController {

    private final WorkTaskService workTaskService;

    /** 新建任务。 */
    @PostMapping
    @Operation(summary = "新建任务")
    public R<Long> create(@Valid @RequestBody TaskCreateDTO dto) {
        return R.ok(workTaskService.create(dto));
    }

    /** 分页查询任务（三视图）。 */
    @GetMapping
    @Operation(summary = "分页查询任务")
    public R<PageResult<TaskVO>> page(@RequestParam(required = false) String view,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(workTaskService.page(view, status, page, size));
    }

    /** 任务详情。 */
    @GetMapping("/{id}")
    @Operation(summary = "任务详情")
    public R<TaskVO> detail(@PathVariable Long id) {
        return R.ok(workTaskService.detail(id));
    }

    /** 全量更新任务。 */
    @PutMapping("/{id}")
    @Operation(summary = "更新任务")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateDTO dto) {
        workTaskService.update(id, dto);
        return R.ok();
    }

    /** 状态流转。 */
    @PutMapping("/{id}/status")
    @Operation(summary = "状态流转")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody TaskStatusDTO dto) {
        workTaskService.updateStatus(id, dto);
        return R.ok();
    }

    /** 删除任务（逻辑删）。 */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除任务")
    public R<Void> remove(@PathVariable Long id) {
        workTaskService.delete(id);
        return R.ok();
    }
}
