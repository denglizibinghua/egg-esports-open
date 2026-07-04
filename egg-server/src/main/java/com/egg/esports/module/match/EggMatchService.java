package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.module.bet.EggBetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 赛事服务（门面）
 * <p>拆分为 MatchQueryService（查询）、MatchManageService（管理）两个子服务，
 * 本类作为门面保留所有原 public 方法签名：查询/管理方法委托给子服务，
 * 结算相关方法（settle / autoSettlePending）保留在本类，保证 Controller 与其他调用方无需改动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EggMatchService {

    private final EggMatchMapper matchMapper;

    private final EggBetService betService;

    private final MatchQueryService matchQueryService;

    private final MatchManageService matchManageService;

    /**
     * 分页查询赛事列表
     *
     * @param status 赛事状态（可选）
     * @param page   页码
     * @param size   每页条数
     * @return 分页结果
     */
    public IPage<EggMatch> getMatchList(Integer status, Long gameId, Integer page, Integer size) {
        return matchQueryService.getMatchList(status, gameId, page, size);
    }

    /**
     * 根据ID查询赛事
     */
    public EggMatch getById(Long id) {
        return matchQueryService.getById(id);
    }

    /**
     * 创建赛事
     */
    public EggMatch create(EggMatch match) {
        return matchManageService.create(match);
    }

    /**
     * 更新赛事信息
     */
    public EggMatch update(EggMatch match) {
        return matchManageService.update(match);
    }

    /**
     * 获取热门赛事（status 0/1/2，按押注人数排序，最多返回 limit 条）
     */
    public List<EggMatch> getHotMatches(int limit, Long gameId) {
        return matchQueryService.getHotMatches(limit, gameId);
    }

    /**
     * 按联赛分组查询赛事（按联赛名+比赛时间排序）
     *
     * @return 联赛名 -> 赛事列表
     */
    public Map<String, List<EggMatch>> getMatchesByTournament() {
        return matchQueryService.getMatchesByTournament();
    }

    /**
     * 批量导入赛事
     *
     * @param matches 赛事列表
     * @return 导入结果统计
     */
    public Map<String, Object> batchImport(List<EggMatch> matches) {
        return matchManageService.batchImport(matches);
    }

    /**
     * 根据externalId新增或更新赛事（同步用）
     * <p>同步时只 upsert 不结算，结算由 autoSettlePending 定时完成。
     *
     * @param match 包含externalId的赛事数据
     * @return 新增或更新后的赛事
     */
    public EggMatch upsertByExternalId(EggMatch match) {
        return matchManageService.upsertByExternalId(match);
    }

    /**
     * 结算赛事结果（同时结算所有押注）
     *
     * @param matchId 赛事ID
     * @param result  结果：0=A胜, 1=B胜, 2=平局
     */
    @Transactional
    @OperationLog(module = "赛事", value = "手动结算")
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

        // 乐观锁：result IS NULL 才能写入，防止手动 settle 与 autoSettlePending 并发用不同 result 派彩
        // 两个线程都通过上方 selectById 检查后，只有一个能成功 update，另一个 updated=0 抛异常
        int updated = matchMapper.update(null,
                new LambdaUpdateWrapper<EggMatch>()
                        .eq(EggMatch::getId, matchId)
                        .isNull(EggMatch::getResult)
                        .set(EggMatch::getResult, result)
        );
        if (updated == 0) {
            throw new BusinessException("该赛事已结算，不可重复结算");
        }

        // 联动结算所有押注
        betService.settleBet(matchId, result);

        // 确保 settled_at 被设置（即使无待结算押注）
        // 加 isNull(settledAt) 守卫：手动 settle 与自动 settle 并发时不会双写
        matchMapper.update(null,
                new LambdaUpdateWrapper<EggMatch>()
                        .eq(EggMatch::getId, matchId)
                        .isNull(EggMatch::getSettledAt)
                        .set(EggMatch::getSettledAt, LocalDateTime.now())
        );

        // 回写 result 到内存对象（DB 已通过上方 update 写入，但 match 对象未回写）
        match.setResult(result);
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
                // 关键：结算完成后必须设置 settled_at，否则下次定时任务还会选中此赛事
                // 配合 selectFinishedWithResult 的 settled_at IS NULL 过滤实现幂等
                matchMapper.update(null,
                        new LambdaUpdateWrapper<EggMatch>()
                                .eq(EggMatch::getId, match.getId())
                                .isNull(EggMatch::getSettledAt)
                                .set(EggMatch::getSettledAt, LocalDateTime.now())
                );
                count++;
            } catch (Exception e) {
                log.warn("自动结算赛事[{}]失败: {}", match.getId(), e.getMessage());
            }
        }
        return count;
    }
}
