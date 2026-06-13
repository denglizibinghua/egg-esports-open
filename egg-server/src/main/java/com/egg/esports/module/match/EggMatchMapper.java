package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 赛事 Mapper
 */
@Mapper
public interface EggMatchMapper extends BaseMapper<EggMatch> {

    @Select("SELECT * FROM egg_match WHERE external_id = #{externalId}")
    EggMatch selectByExternalId(@Param("externalId") String externalId);
}