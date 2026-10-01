package com.xiaodu.personalos.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaodu.personalos.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 *
 * @author Kou
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
}
