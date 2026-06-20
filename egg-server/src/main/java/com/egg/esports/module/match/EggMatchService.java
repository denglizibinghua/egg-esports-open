package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.module.bet.EggBet;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.bet.EggBetService;
import com.egg.esports.module.odds.OddsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛事服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EggMatchService {

    private final EggMatchMapper matchMapper;

    private final EggBetService betService;

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

        // 全部(status=null)：不限制条数，按 tournament + match_time 排序，前端按赛事分组折叠
        // 未开始(status=0)：按比赛时间升序
        // 已结束/进行中：按比赛时间降序
        if (status == null) {
            wrapper.orderByAsc(EggMatch::getTournament).orderByDesc(EggMatch::getMatchTime);
            List<EggMatch> allMatches = matchMapper.selectList(wrapper);
            // 填充赔率
            for (EggMatch m : allMatches) {
                long pa = m.getPoolA() != null ? m.getPoolA() : 0L;
                long pb = m.getPoolB() != null ? m.getPoolB() : 0L;
                Map<String, BigDecimal> odds = oddsService.calculate(pa, pb);
                m.setOddsA(odds.get("oddsA"));
                m.setOddsB(odds.get("oddsB"));
            }
            Page<EggMatch> fakePage = new Page<>(1, allMatches.size());
            fakePage.setRecords(allMatches);
            fakePage.setTotal(allMatches.size());
            return fakePage;
        }

        Page<EggMatch> pageParam = new Page<>(page, size);
        if (status == 0) {
            wrapper.orderByAsc(EggMatch::getMatchTime);
        } else {
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
     * 创建赛事
     */
    public EggMatch create(EggMatch match) {
        if (!StringUtils.hasText(match.getTitle())) {
            throw new BusinessException("赛事标题不能为空");
        }
        if (!StringUtils.hasText(match.getTeamA())) {
            throw new BusinessException("队伍A名称不能为空");
        }
        if (!StringUtils.hasText(match.getTeamB())) {
            throw new BusinessException("队伍B名称不能为空");
        }
        if (match.getMatchTime() == null) {
            throw new BusinessException("比赛时间不能为空");
        }

        match.setStatus(0);
        match.setResult(null);
        match.setCreateTime(LocalDateTime.now());
        matchMapper.insert(match);
        return match;
    }

    /**
     * 更新赛事信息
     */
    public EggMatch update(EggMatch match) {
        EggMatch existing = matchMapper.selectById(match.getId());
        if (existing == null) {
            throw new BusinessException("赛事不存在");
        }

        if (StringUtils.hasText(match.getTitle())) {
            existing.setTitle(match.getTitle());
        }
        if (StringUtils.hasText(match.getTeamA())) {
            existing.setTeamA(match.getTeamA());
        }
        if (StringUtils.hasText(match.getTeamB())) {
            existing.setTeamB(match.getTeamB());
        }
        if (StringUtils.hasText(match.getTeamALogo())) {
            existing.setTeamALogo(match.getTeamALogo());
        }
        if (StringUtils.hasText(match.getTeamBLogo())) {
            existing.setTeamBLogo(match.getTeamBLogo());
        }
        if (StringUtils.hasText(match.getTournament())) {
            existing.setTournament(match.getTournament());
        }
        if (match.getGameId() != null) {
            existing.setGameId(match.getGameId());
        }
        if (match.getMatchTime() != null) {
            existing.setMatchTime(match.getMatchTime());
        }
        if (match.getStatus() != null) {
            existing.setStatus(match.getStatus());
        }

        matchMapper.updateById(existing);
        return existing;
    }

    /**
     * 获取热门赛事（status 0/1/2，按押注人数排序，最多返回 limit 条）
     */
    public List<EggMatch> getHotMatches(int limit, Long gameId) {
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EggMatch::getStatus, 0, 1, 2);
        if (gameId != null) {
            wrapper.eq(EggMatch::getGameId, gameId);
        }
        wrapper.orderByDesc(EggMatch::getMatchTime);
        List<EggMatch> matches = matchMapper.selectList(wrapper);

        // 填充赔率 + 押注人数
        for (EggMatch m : matches) {
            long pa = m.getPoolA() != null ? m.getPoolA() : 0L;
            long pb = m.getPoolB() != null ? m.getPoolB() : 0L;
            Map<String, BigDecimal> odds = oddsService.calculate(pa, pb);
            m.setOddsA(odds.get("oddsA"));
            m.setOddsB(odds.get("oddsB"));
            Long count = betMapper.selectCount(new LambdaQueryWrapper<EggBet>().eq(EggBet::getMatchId, m.getId()));
            m.setBetCount(count != null ? count.intValue() : 0);
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

    /**
     * 批量导入赛事
     *
     * @param matches 赛事列表
     * @return 导入结果统计
     */
    @Transactional
    public Map<String, Object> batchImport(List<EggMatch> matches) {
        int success = 0;
        int fail = 0;
        List<Map<String, Object>> errors = new java.util.ArrayList<>();

        for (int i = 0; i < matches.size(); i++) {
            EggMatch match = matches.get(i);
            try {
                // 校验必填字段
                if (!StringUtils.hasText(match.getTitle())) {
                    throw new BusinessException("赛事标题不能为空");
                }
                if (!StringUtils.hasText(match.getTeamA())) {
                    throw new BusinessException("队伍A名称不能为空");
                }
                if (!StringUtils.hasText(match.getTeamB())) {
                    throw new BusinessException("队伍B名称不能为空");
                }
                if (match.getMatchTime() == null) {
                    throw new BusinessException("比赛时间不能为空");
                }

                match.setStatus(match.getStatus() != null ? match.getStatus() : 0);
                match.setResult(null);
                match.setCreateTime(LocalDateTime.now());
                matchMapper.insert(match);
                success++;
            } catch (BusinessException e) {
                fail++;
                Map<String, Object> err = new HashMap<>();
                err.put("index", i);
                err.put("title", match.getTitle() != null ? match.getTitle() : "未命名");
                err.put("error", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", matches.size());
        result.put("errors", errors);
        return result;
    }

    /**
     * 根据externalId新增或更新赛事（Riot API同步用）
     *
     * @param match 包含externalId的赛事数据
     * @return 新增或更新后的赛事
     */
    public EggMatch upsertByExternalId(EggMatch match) {
        if (match.getExternalId() == null || match.getExternalId().isEmpty()) {
            throw new BusinessException("externalId不能为空");
        }

        EggMatch existing = matchMapper.selectByExternalIdAndGameId(match.getExternalId(), match.getGameId());
        if (existing != null) {
            // 更新已有记录（仅更新有变化的字段）
            boolean changed = false;

            if (match.getStatus() != null && !match.getStatus().equals(existing.getStatus())) {
                existing.setStatus(match.getStatus());
                changed = true;
            }
            if (match.getResult() != null && !match.getResult().equals(existing.getResult())) {
                existing.setResult(match.getResult());
                changed = true;
                // 不再在此处自动结算——统一交由 autoSettlePending 定时任务处理
                // 避免 syncAll → settleBet 与 autoSettlePending → settleBet 双重触发导致通知重复
            }
            if (match.getMatchTime() != null && !match.getMatchTime().equals(existing.getMatchTime())) {
                existing.setMatchTime(match.getMatchTime());
                changed = true;
            }
            if (match.getTitle() != null && !match.getTitle().equals(existing.getTitle())) {
                existing.setTitle(match.getTitle());
                changed = true;
            }
            if (match.getTeamA() != null && !match.getTeamA().equals(existing.getTeamA())) {
                existing.setTeamA(match.getTeamA());
                changed = true;
            }
            if (match.getTeamB() != null && !match.getTeamB().equals(existing.getTeamB())) {
                existing.setTeamB(match.getTeamB());
                changed = true;
            }
            if (match.getTeamALogo() != null && !match.getTeamALogo().equals(existing.getTeamALogo())) {
                existing.setTeamALogo(match.getTeamALogo());
                changed = true;
            }
            if (match.getTeamBLogo() != null && !match.getTeamBLogo().equals(existing.getTeamBLogo())) {
                existing.setTeamBLogo(match.getTeamBLogo());
                changed = true;
            }
            if (match.getTournament() != null && !match.getTournament().equals(existing.getTournament())) {
                existing.setTournament(match.getTournament());
                changed = true;
            }
            if (match.getScoreA() != null && !match.getScoreA().equals(existing.getScoreA())) {
                existing.setScoreA(match.getScoreA());
                changed = true;
            }
            if (match.getScoreB() != null && !match.getScoreB().equals(existing.getScoreB())) {
                existing.setScoreB(match.getScoreB());
                changed = true;
            }

            if (changed) {
                matchMapper.updateById(existing);
            }
            return existing;
        } else {
            // 新增记录
            match.setCreateTime(LocalDateTime.now());
            matchMapper.insert(match);
            return match;
        }
    }

    /**
     * 结算赛事结果（同时结算所有押注）
     *
     * @param matchId 赛事ID
     * @param result  结果：0=A胜, 1=B胜, 2=平局
     */
    @Transactional
    public EggMatch settle(Long matchId, Integer result) {
        if (result == null || result < 0 || result > 2) {
            throw new BusinessException("结算结果无效，必须为0(A胜)、1(B胜)或2(平局)");
        }

        EggMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }
        if (match.getStatus() != 2) {
            throw new BusinessException("只有已结束的赛事才能结算");
        }
        if (match.getResult() != null) {
            throw new BusinessException("该赛事已结算，不可重复结算");
        }

        match.setResult(result);
        matchMapper.updateById(match);

        // 联动结算所有押注
        betService.settleBet(matchId, result);

        return match;
    }

    /**
     * 自动结算：扫描所有已结束且有结果但押注未结算的赛事
     * 用于定时任务调用（Riot API 已在 sync 时写入了 result，但 bets 可能未结算）
     *
     * @return 结算数量
     */
    @Transactional
    @OperationLog(module = "赛事", value = "自动结算")
    public int autoSettlePending() {
        List<EggMatch> toSettle = matchMapper.selectFinishedWithResult();
        int count = 0;
        for (EggMatch match : toSettle) {
            try {
                betService.settleBet(match.getId(), match.getResult());
                count++;
            } catch (Exception e) {
                log.warn("自动结算赛事[{}]失败: {}", match.getId(), e.getMessage());
            }
        }
        return count;
    }
}