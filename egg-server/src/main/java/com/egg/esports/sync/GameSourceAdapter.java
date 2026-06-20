package com.egg.esports.sync;

import java.util.List;

/**
 * 游戏数据源适配器接口
 * <p>每个游戏一个 Adapter 实现，归一化外部数据为 RawMatch 列表
 * <p>注意：本接口不是 MyBatis Mapper，放在 module 包外避免被 @MapperScan 扫描
 */
public interface GameSourceAdapter {

    /**
     * 获取该 Adapter 对应的游戏ID
     */
    Long getGameId();

    /**
     * 拉取赛事数据，归一化为 RawMatch 列表
     */
    List<RawMatch> fetchMatches();
}
