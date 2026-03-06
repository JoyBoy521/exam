package com.exam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
