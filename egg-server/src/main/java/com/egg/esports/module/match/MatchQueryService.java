package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.bet.EggBet;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.odds.OddsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 赛事查询服务（拆分自 EggMatchService）
 * <p>负责赛事列表、详情、热门、按联赛分组等查询逻辑。
 */
@Service
@RequiredArgsConstructor
public class MatchQueryService {

    private final EggMatchMapper matchMapper;

    private final EggBetMapper betMapper;

    private final OddsService oddsService;

    /**
     * 分页查询赛事列表
     *
     * @param status 赛事状态（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    public IPage<EggMatch> getMatchList(Integer status, Long gameId, Integer page, Integer size) {
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(EggMatch::getStatus, status);
        }
        if (gameId != null) {
            wrapper.eq(EggMatch::getGameId, gameId);
        }

        Page<EggMatch> pageParam = new Page<>(page, size);
        if (status == null) {
            // 全部：按联赛+时间排序，前端按赛事分组折叠
            wrapper.orderByAsc(EggMatch::getTournament).orderByDesc(EggMatch::getMatchTime);
        } else if (status == 0) {
            // 未开始：按比赛时间升序
            wrapper.orderByAsc(EggMatch::getMatchTime);
        } else {
            // 已结束/进行中：按比赛时间降序
            wrapper.orderByDesc(EggMatch::getMatchTime);
        }

        IPage<EggMatch> pageResult = matchMapper.selectPage(pageParam, wrapper);
        // 填充赔率
        for (EggMatch m : pageResult.getRecords()) {
            long pa = m.getPoolA() != null ? m.getPoolA() : 0L;
            long pb = m.getPoolB() != null ? m.getPoolB() : 0L;
            Map<String, BigDecimal> odds = oddsService.calculate(pa, pb);
            m.setOddsA(odds.get("oddsA"));
            m.setOddsB(odds.get("oddsB"));
        }
        return pageResult;
    }

    /**
     * 根据ID查询赛事
     */
    public EggMatch getById(Long id) {
        EggMatch match = matchMapper.selectById(id);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }
        // 填充赔率
        long poolA = match.getPoolA() != null ? match.getPoolA() : 0L;
        long poolB = match.getPoolB() != null ? match.getPoolB() : 0L;
        Map<String, BigDecimal> odds = oddsService.calculate(poolA, poolB);
        match.setOddsA(odds.get("oddsA"));
        match.setOddsB(odds.get("oddsB"));
        return match;
    }

    /**
     * 获取热门赛事（status 0/1/2，按押注人数排序，最多返回 limit 条）
     */
    public List<EggMatch> getHotMatches(int limit, Long gameId) {
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EggMatch::getStatus, 0, 1, 2);
        wrapper.ne(EggMatch::getTeamA, "TBD");
        wrapper.ne(EggMatch::getTeamB, "TBD");
        if (gameId != null) {
            wrapper.eq(EggMatch::getGameId, gameId);
        }
        wrapper.orderByDesc(EggMatch::getMatchTime);
        List<EggMatch> matches = matchMapper.selectList(wrapper);

        // 填充赔率
        for (EggMatch m : matches) {
            long pa = m.getPoolA() != null ? m.getPoolA() : 0L;
            long pb = m.getPoolB() != null ? m.getPoolB() : 0L;
            Map<String, BigDecimal> odds = oddsService.calculate(pa, pb);
            m.setOddsA(odds.get("oddsA"));
            m.setOddsB(odds.get("oddsB"));
        }

        // 批量查询各赛事押注人数（一次 GROUP BY 取代逐条 selectCount，避免 N+1）
        List<Long> matchIds = matches.stream().map(EggMatch::getId).toList();
        if (!matchIds.isEmpty()) {
            List<Map<String, Object>> counts = betMapper.selectMaps(
                    new QueryWrapper<EggBet>()
                            .select("match_id, COUNT(*) as cnt")
                            .in("match_id", matchIds)
                            .groupBy("match_id")
            );
            // 组装 matchId -> 押注人数 映射
            Map<Long, Long> countMap = counts.stream()
                    .collect(Collectors.toMap(
                            row -> ((Number) row.get("match_id")).longValue(),
                            row -> ((Number) row.get("cnt")).longValue()));
            matches.forEach(m -> m.setBetCount(countMap.getOrDefault(m.getId(), 0L).intValue()));
        }

        // 排序：status ASC，同状态 betCount DESC
        matches.sort((a, b) -> {
            int sa = a.getStatus() != null ? a.getStatus() : 2;
            int sb = b.getStatus() != null ? b.getStatus() : 2;
            if (sa != sb) return Integer.compare(sa, sb);
            int ca = a.getBetCount() != null ? a.getBetCount() : 0;
            int cb = b.getBetCount() != null ? b.getBetCount() : 0;
            return Integer.compare(cb, ca); // DESC
        });

        return matches.size() > limit ? matches.subList(0, limit) : matches;
    }

    /**
     * 按联赛分组查询赛事（按联赛名+比赛时间排序）
     *
     * @return 联赛名 -> 赛事列表
     */
    public Map<String, List<EggMatch>> getMatchesByTournament() {
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(EggMatch::getTournament, EggMatch::getMatchTime);
        List<EggMatch> allMatches = matchMapper.selectList(wrapper);

        // 填充赔率
        for (EggMatch m : allMatches) {
            long pa = m.getPoolA() != null ? m.getPoolA() : 0L;
            long pb = m.getPoolB() != null ? m.getPoolB() : 0L;
            Map<String, BigDecimal> odds = oddsService.calculate(pa, pb);
            m.setOddsA(odds.get("oddsA"));
            m.setOddsB(odds.get("oddsB"));
        }

        Map<String, List<EggMatch>> grouped = new LinkedHashMap<>();
        for (EggMatch match : allMatches) {
            String tournamentName = match.getTournament() != null ? match.getTournament() : "未分组";
            grouped.computeIfAbsent(tournamentName, k -> new java.util.ArrayList<>()).add(match);
        }
        return grouped;
    }
}
