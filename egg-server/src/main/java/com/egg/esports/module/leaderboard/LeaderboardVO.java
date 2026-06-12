package com.egg.esports.module.leaderboard;

import lombok.Data;

@Data
public class LeaderboardVO {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer points;
    private Integer correctBets;
    private Integer totalBets;
    private Integer winStreak;
    private Integer maxWinStreak;
    private Double accuracy;
}