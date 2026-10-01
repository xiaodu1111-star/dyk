package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.SysMetricRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标记录 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface MetricRecordMapper extends BaseMapper<SysMetricRecord> {
}
