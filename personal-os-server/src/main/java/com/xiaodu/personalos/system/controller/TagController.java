package com.xiaodu.personalos.system.controller;

import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.system.dto.TagCreateDTO;
import com.xiaodu.personalos.system.dto.TagRelDTO;
import com.xiaodu.personalos.system.service.TagService;
import com.xiaodu.personalos.system.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签引擎接口（M0 基础设施）。
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/system/tags")
@RequiredArgsConstructor
@Tag(name = "标签引擎", description = "统一标签 / 多态关联")
public class TagController {

    private final TagService tagService;

    /** 新建标签。 */
    @PostMapping
    @Operation(summary = "新建标签")
    public R<TagVO> create(@Valid @RequestBody TagCreateDTO dto) {
        return R.ok(tagService.create(dto));
    }

    /** 查询标签列表。 */
    @GetMapping
    @Operation(summary = "查询标签列表")
    public R<List<TagVO>> list(@RequestParam(required = false) String scope,
                               @RequestParam(required = false) String keyword) {
        return R.ok(tagService.list(scope, keyword));
    }

    /** 绑定标签到业务实体（幂等）。 */
    @PostMapping("/{tagId}/rels")
    @Operation(summary = "绑定标签到业务实体")
    public R<Long> bind(@PathVariable Long tagId, @Valid @RequestBody TagRelDTO dto) {
        return R.ok(tagService.bind(tagId, dto.getBizType(), dto.getBizId()));
    }

    /** 解绑（幂等）。 */
    @DeleteMapping("/{tagId}/rels")
    @Operation(summary = "解绑标签")
    public R<Void> unbind(@PathVariable Long tagId,
                          @RequestParam String bizType,
                          @RequestParam Long bizId) {
        tagService.unbind(tagId, bizType, bizId);
        return R.ok();
    }
}
