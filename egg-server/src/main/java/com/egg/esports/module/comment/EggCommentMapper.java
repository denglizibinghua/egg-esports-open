package com.egg.esports.module.comment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EggCommentMapper extends BaseMapper<EggComment> {

    @Select("SELECT c.*, u.nickname AS userNickname, u.avatar AS userAvatar " +
            "FROM egg_comment c LEFT JOIN egg_user u ON c.user_id = u.id " +
            "WHERE c.match_id = #{matchId} AND c.parent_id IS NULL AND c.status = 1 " +
            "ORDER BY c.create_time DESC")
    List<CommentVO> selectTopLevel(@Param("matchId") Long matchId);

    @Select("SELECT c.*, u.nickname AS userNickname, u.avatar AS userAvatar " +
            "FROM egg_comment c LEFT JOIN egg_user u ON c.user_id = u.id " +
            "WHERE c.parent_id = #{parentId} AND c.status = 1 " +
            "ORDER BY c.create_time ASC")
    List<CommentVO> selectReplies(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM egg_comment WHERE match_id = #{matchId} AND status = 1")
    int countByMatchId(@Param("matchId") Long matchId);
}
