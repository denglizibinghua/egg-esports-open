package com.egg.esports.module.match;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单张地图比分（瓦罗兰特 BO3/BO5）
 * 与前端 map-chips 共用结构，Haven 13-4 形式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapScore {
    /** 地图名：Haven / Bind / Ascent / Lotus / Split / Sunset / Abyss */
    private String map;
    /** A 队该地图得分 */
    private Integer scoreA;
    /** B 队该地图得分 */
    private Integer scoreB;
    /** 胜者：0=A, 1=B, null=未决 */
    private Integer winner;

    public boolean isWinA() {
        return winner != null && winner == 0;
    }

    public boolean isWinB() {
        return winner != null && winner == 1;
    }
}
