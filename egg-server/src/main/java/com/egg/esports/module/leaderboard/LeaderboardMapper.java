package com.egg.esports.module.leaderboard;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜自定义 Mapper
 * 用于按游戏过滤的 JOIN 查询（egg_user JOIN egg_bet JOIN egg_match）
 * 全局排行榜（gameId=null）走 EggUserMapper，不走这里。
 */
@Mapper
public interface LeaderboardMapper {

    /**
     * 按游戏查询排行榜（分页）
     * 排序：猜中次数 DESC，总押注数 DESC
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} " +
            "GROUP BY u.id " +
            "ORDER BY correct_bets DESC, total_bets DESC " +
            "LIMIT #{offset}, #{n}")
    List<LeaderboardVO> selectGameLeaderboardLimit(@Param("gameId") Long gameId,
                                                    @Param("offset") int offset,
                                                    @Param("n") int n);

    /**
     * 按游戏查询排行榜总人数
     */
    @Select("SELECT COUNT(DISTINCT u.id) " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId}")
    int countGameLeaderboard(@Param("gameId") Long gameId);

    /**
     * 按游戏查询排行榜 Top N
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} " +
            "GROUP BY u.id " +
            "ORDER BY correct_bets DESC, total_bets DESC " +
            "LIMIT #{n}")
    List<LeaderboardVO> selectGameLeaderboardTop(@Param("gameId") Long gameId,
                                                  @Param("n") int n);

    /**
     * 按游戏查询完整排行榜（用于「我的排名」定位）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} " +
            "GROUP BY u.id " +
            "ORDER BY correct_bets DESC, total_bets DESC")
    List<LeaderboardVO> selectGameLeaderboardAll(@Param("gameId") Long gameId);
}
