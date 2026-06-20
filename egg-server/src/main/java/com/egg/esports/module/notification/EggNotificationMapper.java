package com.egg.esports.module.notification;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface EggNotificationMapper extends BaseMapper<EggNotification> {

    @Select("SELECT * FROM egg_notification WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<EggNotification> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM egg_notification WHERE user_id = #{userId} AND is_read = 0")
    int selectUnreadCount(@Param("userId") Long userId);

    @Update("UPDATE egg_notification SET is_read = 1 WHERE id = #{id} AND user_id = #{userId}")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE egg_notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM egg_notification WHERE user_id = #{userId} AND type = #{type} AND related_id = #{relatedId}")
    int countByUserAndTypeAndRelated(@Param("userId") Long userId, @Param("type") Integer type, @Param("relatedId") Long relatedId);
}