package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.SysMetricDef;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标定义 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface MetricDefMapper extends BaseMapper<SysMetricDef> {
}
