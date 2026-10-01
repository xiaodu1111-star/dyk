package com.xiaodu.personalos.system.service;

import com.xiaodu.personalos.system.vo.HomeAggVO;

/**
 * 首页聚合服务（M0 只出骨架，P1 集成阶段各模块数据接线）。
 *
 * @author Kou
 */
public interface HomeAggService {

    /**
     * 聚合首页数据。契约对齐《m0-infra.md》§3。
     *
     * @return 首页聚合 VO
     */
    HomeAggVO aggregate();
}
