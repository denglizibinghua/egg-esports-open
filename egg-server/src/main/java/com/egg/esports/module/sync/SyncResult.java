package com.egg.esports.module.sync;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 同步结果统计
 */
@Data
public class SyncResult {

    /** CN赛区新增数 */
    private int cnCreated;
    /** CN赛区更新数 */
    private int cnUpdated;
    /** 国际赛区新增数 */
    private int intlCreated;
    /** 国际赛区更新数 */
    private int intlUpdated;
    /** 总获取数 */
    private int totalFetched;
    /** 跳过数（startTime早于2026-01-01等） */
    private int totalSkipped;
    /** 错误信息列表 */
    private List<String> errors = new ArrayList<>();
    /** 同步时间 */
    private LocalDateTime syncTime = LocalDateTime.now();
}