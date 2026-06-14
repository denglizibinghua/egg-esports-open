package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 赛事 Mapper
 */
@Mapper
public interface EggMatchMapper extends BaseMapper<EggMatch> {

    @Select("SELECT * FROM egg_match WHERE external_id = #{externalId}")
    EggMatch selectByExternalId(@Param("externalId") String externalId);

    /**
     * 查询已结束且有结果但可能未结算押注的赛事
     * 用于自动结算定时任务
     */
    @Select("SELECT * FROM egg_match WHERE status = 2 AND result IS NOT NULL")
    List<EggMatch> selectFinishedWithResult();
}