package com.egg.esports.module.match;

import com.egg.esports.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛事管理服务（拆分自 EggMatchService）
 * <p>负责赛事创建、更新、批量导入、upsert 等管理逻辑。
 */
@Service
@RequiredArgsConstructor
public class MatchManageService {

    private final EggMatchMapper matchMapper;

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
        // 通过校验的赛事收集到此列表，循环结束后一次性批量插入，避免 N+1
        List<EggMatch> toInsert = new java.util.ArrayList<>();

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
                toInsert.add(match);
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

        // 批量插入通过校验的赛事（一条 SQL 插入多条，取代逐条 insert）
        if (!toInsert.isEmpty()) {
            matchMapper.batchInsert(toInsert);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("fail", fail);
        result.put("total", matches.size());
        result.put("errors", errors);
        return result;
    }

    /**
     * 根据externalId新增或更新赛事（同步用）
     * <p>同步时只 upsert 不结算，结算由 autoSettlePending 定时完成。
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
                // 🔒 检测 Riot API 队伍是否翻转：如果新数据的 teamA 与已有记录不同，result 需要重新映射
                boolean teamFlipped = match.getTeamA() != null && existing.getTeamA() != null
                        && !match.getTeamA().equals(existing.getTeamA());
                if (teamFlipped && match.getResult() != 2) {
                    // 队伍被 Riot API 翻转了，result 映射取反（0↔1）
                    existing.setResult(match.getResult() == 0 ? 1 : 0);
                } else {
                    existing.setResult(match.getResult());
                }
                changed = true;
            }
            if (match.getMatchTime() != null && !match.getMatchTime().equals(existing.getMatchTime())) {
                existing.setMatchTime(match.getMatchTime());
                changed = true;
            }
            // 🔒 队伍名/标题首次创建后不覆盖，防止 Riot API teams[] 顺序翻转导致押注历史错乱
            // 例外：TBD/TBA/? 占位符允许被真队名覆盖（否则线上永远待定）
            if (isPlaceholder(existing.getTeamA()) && !isPlaceholder(match.getTeamA())
                    && match.getTeamA() != null && !match.getTeamA().equals(existing.getTeamA())) {
                existing.setTeamA(match.getTeamA());
                changed = true;
            }
            if (isPlaceholder(existing.getTeamB()) && !isPlaceholder(match.getTeamB())
                    && match.getTeamB() != null && !match.getTeamB().equals(existing.getTeamB())) {
                existing.setTeamB(match.getTeamB());
                changed = true;
            }
            if ((isPlaceholder(existing.getTeamA()) || isPlaceholder(existing.getTeamB()))
                    && match.getTitle() != null && !match.getTitle().equals(existing.getTitle())) {
                existing.setTitle(match.getTitle());
                changed = true;
            }
            // 队标可以安全更新——图片 URL 不存在翻转问题
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

    private boolean isPlaceholder(String s) {
        if (s == null) return true;
        String t = s.trim();
        return t.isEmpty() || t.equalsIgnoreCase("TBD") || t.equalsIgnoreCase("TBA") || t.equals("?");
    }
}
