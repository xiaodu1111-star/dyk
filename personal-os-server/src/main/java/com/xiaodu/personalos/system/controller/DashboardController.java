package com.xiaodu.personalos.system.controller;

import com.xiaodu.personalos.common.result.R;
import com.xiaodu.personalos.system.service.HomeAggService;
import com.xiaodu.personalos.system.vo.HomeAggVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页聚合接口（M0 骨架，契约对齐《m0-infra.md》§3）。
 *
 * @author Kou
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "驾驶舱", description = "首页聚合")
public class DashboardController {

    private final HomeAggService homeAggService;

    /** 首页聚合数据。 */
    @GetMapping("/home")
    @Operation(summary = "首页聚合数据")
    public R<HomeAggVO> home() {
        return R.ok(homeAggService.aggregate());
    }
}
