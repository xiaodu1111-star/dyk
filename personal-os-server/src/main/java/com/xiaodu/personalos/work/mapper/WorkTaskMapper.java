package com.xiaodu.personalos.work.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.work.entity.WorkTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务 Mapper（表 {@code work_task}）。
 *
 * @author Kou
 */
@Mapper
public interface WorkTaskMapper extends BaseMapper<WorkTask> {
}
