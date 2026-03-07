package com.exam.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("""
            SELECT id,
                   username,
                   COALESCE(real_name, username) AS display_name,
                   password,
                   role,
                   status,
                   create_time,
                   update_time
            FROM sys_user
            WHERE username = #{username}
            LIMIT 1
            """)
    SysUser selectLegacyByUsername(@Param("username") String username);
}
