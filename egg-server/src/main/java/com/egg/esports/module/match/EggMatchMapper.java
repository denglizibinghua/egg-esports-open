package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 赛事 Mapper
 */
@Mapper
public interface EggMatchMapper extends BaseMapper<EggMatch> {

    @Select("SELECT * FROM egg_match WHERE external_id = #{externalId} AND game_id = #{gameId}")
    EggMatch selectByExternalIdAndGameId(@Param("externalId") String externalId, @Param("gameId") Long gameId);

    /** @deprecated 用 selectByExternalIdAndGameId 替代，多游戏下 externalId 需配合 gameId 唯一 */
    @Select("SELECT * FROM egg_match WHERE external_id = #{externalId}")
    EggMatch selectByExternalId(@Param("externalId") String externalId);

    /**
     * 查询已结束且有结果但可能未结算押注的赛事
     * 用于自动结算定时任务
     */
    @Select("SELECT * FROM egg_match WHERE status = 2 AND result IS NOT NULL")
    List<EggMatch> selectFinishedWithResult();

    /**
     * 原子更新奖池（乐观锁）
     * 只有当 pool_a 和 pool_b 等于预期值时才更新，防止并发冲突
     */
    @Update("UPDATE egg_match SET pool_a = pool_a + #{deltaA}, pool_b = pool_b + #{deltaB} WHERE id = #{id} AND pool_a = #{expectedA} AND pool_b = #{expectedB}")
    int updatePool(@Param("id") Long id, @Param("deltaA") Long deltaA, @Param("deltaB") Long deltaB, @Param("expectedA") Long expectedA, @Param("expectedB") Long expectedB);
}