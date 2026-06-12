package com.egg.esports.module.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EggUserMapper extends BaseMapper<EggUser> {

    @Select("SELECT * FROM egg_user WHERE username = #{username}")
    EggUser selectByUsername(String username);
}