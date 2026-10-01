package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.SysTagRel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签多态关联 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface TagRelMapper extends BaseMapper<SysTagRel> {
}
