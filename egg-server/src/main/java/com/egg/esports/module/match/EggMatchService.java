package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.bet.EggBetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 赛事服务
 */
@Service
@RequiredArgsConstructor
public class EggMatchService {

    private final EggMatchMapper matchMapper;

    private final EggBetService betService;

    /**
     * 分页查询赛事列表
     *
     * @param status 赛事状态（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    public IPage<EggMatch> getMatchList(Integer status, Integer page, Integer size) {
        Page<EggMatch> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(EggMatch::getStatus, status);
        }

        // 未开始(0)按比赛时间升序，已结束(2)按比赛时间降序，其他默认降序
        if (status != null && status == 0) {
            wrapper.orderByAsc(EggMatch::getMatchTime);
        } else {
            wrapper.orderByDesc(EggMatch::getMatchTime);
        }

        return matchMapper.selectPage(pageParam, wrapper);
    }

    /**
     * 根据ID查询赛事
     */
    public EggMatch getById(Long id) {
        EggMatch match = matchMapper.selectById(id);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }
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
     * 获取热门赛事（即将开始 + 进行中，按时间排序，最多返回 limit 条）
     */
    public List<EggMatch> getHotMatches(int limit) {
        LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EggMatch::getStatus, 0, 1)
                .orderByAsc(EggMatch::getMatchTime)
                .last("LIMIT " + limit);
        return matchMapper.selectList(wrapper);
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

        EggMatch existing = matchMapper.selectByExternalId(match.getExternalId());
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
}