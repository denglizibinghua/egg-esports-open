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
     * 供后台/seed/管理端使用，返回全部启用游戏
     */
    public List<EggGame> listActive() {
        LambdaQueryWrapper<EggGame> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EggGame::getStatus, 1)
                .orderByAsc(EggGame::getSortOrder);
        return gameMapper.selectList(wrapper);
    }

    /**
     * 瓦聚焦：前台专用 — 只返回瓦洛兰特（gameId=1）
     * <p>SELECT * WHERE status=1 AND id=1 ORDER BY sortOrder
     * <p>后台/seed 仍调用 {@link #listActive()} 获取全量游戏，保留多游戏能力
     */
    public List<EggGame> listActiveForFrontend() {
        LambdaQueryWrapper<EggGame> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EggGame::getStatus, 1)
                .eq(EggGame::getId, 1L)
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