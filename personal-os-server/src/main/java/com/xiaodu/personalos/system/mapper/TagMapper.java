package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.SysTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 统一标签 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface TagMapper extends BaseMapper<SysTag> {
}
