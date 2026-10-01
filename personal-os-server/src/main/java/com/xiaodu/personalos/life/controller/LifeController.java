package com.xiaodu.personalos.life.controller;

import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.life.dto.HabitCheckinDTO;
import com.xiaodu.personalos.life.dto.HabitCreateDTO;
import com.xiaodu.personalos.life.dto.QuickRecordDTO;
import com.xiaodu.personalos.life.dto.RecordConfirmDTO;
import com.xiaodu.personalos.life.service.HabitService;
import com.xiaodu.personalos.life.vo.HabitVO;
import com.xiaodu.personalos.life.vo.QuickRecordVO;
import com.xiaodu.personalos.life.vo.RecordVO;
import com.xiaodu.personalos.life.vo.StreakVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 生活域接口（M3）：习惯打卡 + 快捷记录。
 *
 * <p>路径前缀 {@code /api/life}（见 dev-workflow.md §3.1）。Controller 只做参数绑定与委托，
 * 不碰 Entity、不 try/catch 业务异常（由 GlobalExceptionHandler 统一兜底）。</p>
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/life")
@RequiredArgsConstructor
@Tag(name = "生活域", description = "习惯打卡 + 快捷记录")
public class LifeController {

    private final HabitService habitService;

    /** 新建习惯。 */
    @PostMapping("/habits")
    @Operation(summary = "新建习惯")
    public R<HabitVO> createHabit(@Valid @RequestBody HabitCreateDTO dto) {
        return R.ok(habitService.create(dto));
    }

    /** 习惯列表（含今日完成状态）。 */
    @GetMapping("/habits")
    @Operation(summary = "习惯列表")
    public R<List<HabitVO>> listHabits() {
        return R.ok(habitService.list());
    }

    /** 修改习惯（改名 / 目标 / 单位）。 */
    @PutMapping("/habits/{id}")
    @Operation(summary = "修改习惯")
    public R<HabitVO> updateHabit(@PathVariable Long id, @Valid @RequestBody HabitCreateDTO dto) {
        return R.ok(habitService.update(id, dto));
    }

    /** 删除习惯（逻辑删）。 */
    @DeleteMapping("/habits/{id}")
    @Operation(summary = "删除习惯")
    public R<Void> deleteHabit(@PathVariable Long id) {
        habitService.delete(id);
        return R.ok();
    }

    /** 打卡（打卡型幂等；计数型增量）。 */
    @PostMapping("/habits/{id}/checkin")
    @Operation(summary = "打卡")
    public R<RecordVO> checkin(@PathVariable Long id,
                               @Valid @RequestBody(required = false) HabitCheckinDTO dto) {
        HabitCheckinDTO body = dto != null ? dto : new HabitCheckinDTO();
        return R.ok(habitService.checkin(id, body));
    }

    /** 连续打卡天数。 */
    @GetMapping("/habits/{id}/streak")
    @Operation(summary = "连续打卡天数")
    public R<StreakVO> streak(@PathVariable Long id) {
        return R.ok(habitService.streak(id));
    }

    /** 快捷记录解析（只解析不落库）。 */
    @PostMapping("/quick-record")
    @Operation(summary = "快捷记录解析")
    public R<QuickRecordVO> quickRecord(@Valid @RequestBody QuickRecordDTO dto) {
        return R.ok(habitService.quickRecord(dto));
    }

    /** 记录确认落库（快捷记录与手动统一入口）。 */
    @PostMapping("/records")
    @Operation(summary = "记录确认落库")
    public R<RecordVO> confirmRecord(@Valid @RequestBody RecordConfirmDTO dto) {
        return R.ok(habitService.confirmRecord(dto));
    }
}
