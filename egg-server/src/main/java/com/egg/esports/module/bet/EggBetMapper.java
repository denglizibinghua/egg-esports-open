package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EggBetMapper extends BaseMapper<EggBet> {

    /**
     * 查询指定时间后已结算的下注记录（status IN 1,2 且 update_time > since）
     */
    @Select("SELECT * FROM egg_bet WHERE user_id = #{userId} AND status IN (1, 2) AND update_time > #{since} ORDER BY update_time DESC")
    List<EggBet> selectSettledSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(SUM(bet_amount), 0) FROM egg_bet WHERE user_id = #{userId} AND status = 0")
    int getActiveSumByUserId(@Param("userId") Long userId);

    /** 统计赛事各方的下注人数 */
    @Select("SELECT COALESCE(SUM(CASE WHEN bet_side = 0 THEN 1 ELSE 0 END), 0) FROM egg_bet WHERE match_id = #{matchId}")
    int countBySideA(@Param("matchId") Long matchId);

    @Select("SELECT COALESCE(SUM(CASE WHEN bet_side = 1 THEN 1 ELSE 0 END), 0) FROM egg_bet WHERE match_id = #{matchId}")
    int countBySideB(@Param("matchId") Long matchId);
}