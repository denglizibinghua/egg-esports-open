package com.egg.esports.sync;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同步结果统计
 */
@Data
public class SyncResult {

    /** 总获取数 */
    private int totalFetched;
    /** 总新增数 */
    private int totalCreated;
    /** 总更新数 */
    private int totalUpdated;
    /** 跳过数（startTime早于2026-01-01等） */
    private int totalSkipped;
    /** 自动结算数 */
    private int autoSettled;
    /** 错误信息列表 */
    private List<String> errors = new ArrayList<>();
    /** 同步时间 */
    private LocalDateTime syncTime = LocalDateTime.now();
    /** 每个游戏的统计：key="game-{gameId}"，value=[created, updated, skipped] */
    private Map<String, int[]> perGameStats = new LinkedHashMap<>();
}
