package com.egg.esports.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EggUserMapper extends BaseMapper<EggUser> {

    @Select("SELECT * FROM egg_user WHERE username = #{username}")
    EggUser selectByUsername(String username);

    @Select("SELECT * FROM egg_user WHERE email = #{email}")
    EggUser selectByEmail(@Param("email") String email);

    /** 登录失败次数 +1 */
    @Update("UPDATE egg_user SET login_attempts = login_attempts + 1 WHERE id = #{id}")
    int incrementLoginAttempts(@Param("id") Long id);

    /** 登录成功：重置失败次数和锁定时间 */
    @Update("UPDATE egg_user SET login_attempts = 0, locked_until = NULL WHERE id = #{id}")
    int resetLoginAttempts(@Param("id") Long id);
}