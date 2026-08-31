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
            // 🔒 队伍名/标题智能占位填充 — 修复 Riot teams[] 顺序翻转导致 LOUD vs LOUD 重复
            // 场景：existing=TBD vs LOUD, 新数据翻转为 LOUD vs MIBR 时，旧逻辑会把 TBD 填成 LOUD 造成重复
            // 策略：
            //  1) 双占位 → 直接用新队名覆盖（需判重防 LOUD vs LOUD 源数据异常）
            //  2) 单占位 → 检查新队名是否与已知队重复，若重复则取另一侧队名
            //  3) 队标与队名联动更新，避免 logo 已翻转但队名仍旧导致 MIBR logo 配 LOUD 队名
            boolean phA = isPlaceholder(existing.getTeamA());
            boolean phB = isPlaceholder(existing.getTeamB());
            boolean newPhA = isPlaceholder(match.getTeamA());
            boolean newPhB = isPlaceholder(match.getTeamB());

            if (phA || phB) {
                String newA = match.getTeamA();
                String newB = match.getTeamB();
                String existA = existing.getTeamA();
                String existB = existing.getTeamB();

                if (phA && phB) {
                    // 双占位：新数据双真实且不重复 → 直接覆盖
                    if (!newPhA && !newPhB && newA != null && newB != null && !newA.equals(newB)) {
                        if (!newA.equals(existA)) { existing.setTeamA(newA); changed = true; }
                        if (!newB.equals(existB)) { existing.setTeamB(newB); changed = true; }
                    } else if (!newPhA && newPhB && newA != null) {
                        // 仅一侧真实（如 TBD vs LOUD）
                        if (!newA.equals(existA) && !newA.equals(existB)) { existing.setTeamA(newA); changed = true; }
                    } else if (newPhA && !newPhB && newB != null) {
                        if (!newB.equals(existA) && !newB.equals(existB)) { existing.setTeamB(newB); changed = true; }
                    }
                } else if (phA && !phB) {
                    // A 占位 B 已知（如 TBD vs LOUD）
                    String knownB = existB;
                    if (!newPhA && newA != null && !newA.equals(knownB) && !newA.equals(existA)) {
                        existing.setTeamA(newA); changed = true;
                    } else if (!newPhB && newB != null && !newB.equals(knownB) && !newB.equals(existA)) {
                        // 翻转：newA==knownB，新 B 才是真实对手（LOUD vs MIBR → MIBR）
                        existing.setTeamA(newB); changed = true;
                    } else if (!newPhA && newA != null && newA.equals(knownB) && !newPhB && newB != null) {
                        // 明确翻转场景，直接取对手
                        if (!newB.equals(knownB)) { existing.setTeamA(newB); changed = true; }
                    }
                } else if (!phA && phB) {
                    // B 占位 A 已知（如 LEV vs TBD）
                    String knownA = existA;
                    if (!newPhB && newB != null && !newB.equals(knownA) && !newB.equals(existB)) {
                        existing.setTeamB(newB); changed = true;
                    } else if (!newPhA && newA != null && !newA.equals(knownA) && !newA.equals(existB)) {
                        existing.setTeamB(newA); changed = true;
                    } else if (!newPhB && newB != null && newB.equals(knownA) && !newPhA && newA != null) {
                        if (!newA.equals(knownA)) { existing.setTeamB(newA); changed = true; }
                    }
                }

                // 标题随队名联动重算（避免 TBD vs LOUD 残留）
                if (changed) {
                    String correctedA = existing.getTeamA();
                    String correctedB = existing.getTeamB();
                    if (correctedA != null && correctedB != null && !correctedA.equals(correctedB)) {
                        // 保留联赛前缀：若原标题含 " — " 则保留前缀，否则直接拼接
                        String newTitle = correctedA + " vs " + correctedB;
                        String oldTitle = existing.getTitle();
                        if (oldTitle != null && oldTitle.contains(" — ")) {
                            String prefix = oldTitle.substring(0, oldTitle.indexOf(" — ") + 3);
                            newTitle = prefix + newTitle;
                        } else if (match.getTitle() != null && match.getTitle().contains(" — ")) {
                            String prefix = match.getTitle().substring(0, match.getTitle().indexOf(" — ") + 3);
                            newTitle = prefix + newTitle;
                        }
                        if (!newTitle.equals(existing.getTitle())) {
                            existing.setTitle(newTitle);
                        }
                    }
                }
            }
            // 自愈：已脏数据 LOUD vs LOUD / LEV vs LEV（两队非占位但重复），即使非占位也强制用新数据纠正
            // 生产 3451/3452 已命中：logo 已正确但队名重复，下次同步自动自愈
            if (!phA && !phB && existing.getTeamA() != null && existing.getTeamA().equals(existing.getTeamB())) {
                String dup = existing.getTeamA();
                String newA = match.getTeamA();
                String newB = match.getTeamB();
                if (!isPlaceholder(newA) && !isPlaceholder(newB) && newA != null && newB != null && !newA.equals(newB)) {
                    // 新数据双真实且不重复 → 直接覆盖双侧（即使顺序翻转，也比重复正确；score/result 翻转由上方 teamFlipped 逻辑处理）
                    if (!newA.equals(existing.getTeamA()) || !newB.equals(existing.getTeamB())) {
                        existing.setTeamA(newA);
                        existing.setTeamB(newB);
                        changed = true;
                        // 标题重算
                        String oldTitle = existing.getTitle();
                        String prefix = "";
                        if (oldTitle != null && oldTitle.contains(" — ")) {
                            prefix = oldTitle.substring(0, oldTitle.indexOf(" — ") + 3);
                        } else if (match.getTitle() != null && match.getTitle().contains(" — ")) {
                            prefix = match.getTitle().substring(0, match.getTitle().indexOf(" — ") + 3);
                        }
                        String newTitle = prefix + newA + " vs " + newB;
                        if (!newTitle.equals(existing.getTitle())) existing.setTitle(newTitle);
                    }
                }
            }
            // 队标与队名联动更新 — 仅当队名不重复时才更新，避免 MIBR logo 配 LOUD 名
            // 若检测到队名翻转，logo 需同步翻转
            {
                String newLogoA = match.getTeamALogo();
                String newLogoB = match.getTeamBLogo();
                String existLogoA = existing.getTeamALogo();
                String existLogoB = existing.getTeamBLogo();
                // 常规更新：logo 不等即更新，但若队名已判定翻转则同步对调
                boolean flipped = false;
                if ((phA || phB) && match.getTeamA() != null && match.getTeamB() != null) {
                    // 简单翻转检测：新 A 等于旧 B 且新 B 不等于旧 B
                    if (!isPlaceholder(existing.getTeamB()) && match.getTeamA() != null
                            && match.getTeamA().equals(existing.getTeamB())
                            && !match.getTeamB().equals(existing.getTeamB())) {
                        flipped = true;
                    }
                    if (!isPlaceholder(existing.getTeamA()) && match.getTeamB() != null
                            && match.getTeamB().equals(existing.getTeamA())
                            && !match.getTeamA().equals(existing.getTeamA())) {
                        flipped = true;
                    }
                }
                if (flipped) {
                    // 翻转时对调 logo
                    if (newLogoB != null && !newLogoB.equals(existLogoA)) {
                        existing.setTeamALogo(newLogoB); changed = true;
                    }
                    if (newLogoA != null && !newLogoA.equals(existLogoB)) {
                        existing.setTeamBLogo(newLogoA); changed = true;
                    }
                } else {
                    if (newLogoA != null && !newLogoA.equals(existLogoA)) {
                        existing.setTeamALogo(newLogoA); changed = true;
                    }
                    if (newLogoB != null && !newLogoB.equals(existLogoB)) {
                        existing.setTeamBLogo(newLogoB); changed = true;
                    }
                }
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
