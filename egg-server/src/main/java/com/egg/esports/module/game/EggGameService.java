package com.egg.esports.module.game;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 游戏服务
 */
@Service
@RequiredArgsConstructor
public class EggGameService {

    private final EggGameMapper gameMapper;

    /**
     * 查询所有启用的游戏（按排序号升序）
     */
    public List<EggGame> listActive() {
        LambdaQueryWrapper<EggGame> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EggGame::getStatus, 1)
                .orderByAsc(EggGame::getSortOrder);
        return gameMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询游戏
     */
    public EggGame getById(Long id) {
        return gameMapper.selectById(id);
    }
}