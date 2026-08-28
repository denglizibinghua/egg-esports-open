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
     * 排序：积分 DESC，猜中次数 DESC（与全局榜一致，积分是主排序）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "ORDER BY u.points DESC, correct_bets DESC " +
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
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%'")
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
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "ORDER BY u.points DESC, correct_bets DESC " +
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
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "ORDER BY u.points DESC, correct_bets DESC")
    List<LeaderboardVO> selectGameLeaderboardAll(@Param("gameId") Long gameId);

    // ==================== 胜率排行榜（≥10 场） ====================

    /**
     * 按游戏查询胜率排行榜（分页，仅 total_bets >= 10）
     * 排序：胜率 DESC，猜中次数 DESC
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak, " +
            "ROUND(COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END), 0), 2) as accuracy " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 10 " +
            "ORDER BY accuracy DESC, correct_bets DESC " +
            "LIMIT #{offset}, #{n}")
    List<LeaderboardVO> selectAccuracyLeaderboardLimit(@Param("gameId") Long gameId,
                                                        @Param("offset") int offset,
                                                        @Param("n") int n);

    /**
     * 按游戏查询胜率排行榜总人数（仅 total_bets >= 10）
     */
    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT u.id FROM egg_user u " +
            "  INNER JOIN egg_bet b ON u.id = b.user_id " +
            "  INNER JOIN egg_match m ON b.match_id = m.id " +
            "  WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "  GROUP BY u.id " +
            "  HAVING COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) >= 10" +
            ") t")
    int countAccuracyLeaderboard(@Param("gameId") Long gameId);

    /**
     * 按游戏查询胜率排行榜 Top N（仅 total_bets >= 10）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak, " +
            "ROUND(COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END), 0), 2) as accuracy " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 10 " +
            "ORDER BY accuracy DESC, correct_bets DESC " +
            "LIMIT #{n}")
    List<LeaderboardVO> selectAccuracyLeaderboardTop(@Param("gameId") Long gameId,
                                                      @Param("n") int n);

    /**
     * 按游戏查询胜率排行榜（全部，用于 my-rank 定位）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak, " +
            "ROUND(COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END), 0), 2) as accuracy " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 10 " +
            "ORDER BY accuracy DESC, correct_bets DESC")
    List<LeaderboardVO> selectAccuracyLeaderboardAll(@Param("gameId") Long gameId);

    // ==================== 全部游戏胜率榜（不按 gameId 过滤） ====================

    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak, " +
            "ROUND(COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END), 0), 2) as accuracy " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 10 " +
            "ORDER BY accuracy DESC, correct_bets DESC " +
            "LIMIT #{offset}, #{n}")
    List<LeaderboardVO> selectAccuracyLeaderboardLimitAll(@Param("offset") int offset, @Param("n") int n);

    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT u.id FROM egg_user u " +
            "  INNER JOIN egg_bet b ON u.id = b.user_id " +
            "  INNER JOIN egg_match m ON b.match_id = m.id " +
            "  WHERE u.username NOT LIKE 'bot_%' " +
            "  GROUP BY u.id " +
            "  HAVING COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) >= 10" +
            ") t")
    int countAccuracyLeaderboardAll();

    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak, " +
            "ROUND(COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) * 100.0 / NULLIF(COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END), 0), 2) as accuracy " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 10 " +
            "ORDER BY accuracy DESC, correct_bets DESC " +
            "LIMIT #{n}")
    List<LeaderboardVO> selectAccuracyLeaderboardTopAll(@Param("n") int n);

    // ==================== 全部游戏净收益最低榜（不按 gameId 过滤） ====================

    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 1 " +
            "ORDER BY u.net_profit ASC " +
            "LIMIT #{offset}, #{n}")
    List<LeaderboardVO> selectWorstProfitLeaderboardLimitAll(@Param("offset") int offset, @Param("n") int n);

    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT u.id FROM egg_user u " +
            "  INNER JOIN egg_bet b ON u.id = b.user_id " +
            "  INNER JOIN egg_match m ON b.match_id = m.id " +
            "  WHERE u.username NOT LIKE 'bot_%' " +
            "  GROUP BY u.id " +
            "  HAVING COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) >= 1" +
            ") t")
    int countWorstProfitLeaderboardAll();

    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 1 " +
            "ORDER BY u.net_profit ASC " +
            "LIMIT #{n}")
    List<LeaderboardVO> selectWorstProfitLeaderboardTopAll(@Param("n") int n);

    /**
     * 按游戏查询净收益排行榜（分页，≥1 场，ASC = 亏损最多在前）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 1 " +
            "ORDER BY u.net_profit ASC " +
            "LIMIT #{offset}, #{n}")
    List<LeaderboardVO> selectWorstProfitLeaderboardLimit(@Param("gameId") Long gameId,
                                                           @Param("offset") int offset,
                                                           @Param("n") int n);

    /**
     * 按游戏查询净收益排行榜总人数（≥1 场）
     */
    @Select("SELECT COUNT(*) FROM (" +
            "  SELECT u.id FROM egg_user u " +
            "  INNER JOIN egg_bet b ON u.id = b.user_id " +
            "  INNER JOIN egg_match m ON b.match_id = m.id " +
            "  WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "  GROUP BY u.id " +
            "  HAVING COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) >= 1" +
            ") t")
    int countWorstProfitLeaderboard(@Param("gameId") Long gameId);

    /**
     * 按游戏查询净收益排行榜 Top N（≥1 场，ASC = 亏损最多在前）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 1 " +
            "ORDER BY u.net_profit ASC " +
            "LIMIT #{n}")
    List<LeaderboardVO> selectWorstProfitLeaderboardTop(@Param("gameId") Long gameId,
                                                         @Param("n") int n);

    /**
     * 按游戏查询净收益排行榜（全部，用于 my-rank 定位）
     */
    @Select("SELECT u.id as user_id, u.nickname, u.avatar, u.points, u.net_profit, " +
            "COUNT(CASE WHEN b.status = 1 AND m.result != 2 THEN 1 END) as correct_bets, " +
            "COUNT(CASE WHEN b.status IN (1, 2) THEN 1 END) as total_bets, " +
            "u.win_streak, u.max_win_streak " +
            "FROM egg_user u " +
            "INNER JOIN egg_bet b ON u.id = b.user_id " +
            "INNER JOIN egg_match m ON b.match_id = m.id " +
            "WHERE m.game_id = #{gameId} AND u.username NOT LIKE 'bot_%' " +
            "GROUP BY u.id " +
            "HAVING total_bets >= 1 " +
            "ORDER BY u.net_profit ASC")
    List<LeaderboardVO> selectWorstProfitLeaderboardAll(@Param("gameId") Long gameId);
}
