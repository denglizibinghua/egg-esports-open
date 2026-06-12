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