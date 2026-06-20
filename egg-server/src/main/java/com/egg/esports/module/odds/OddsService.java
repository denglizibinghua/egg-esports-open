package com.egg.esports.module.odds;

import com.egg.esports.config.OddsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 赔率计算服务
 * 公式: odds = (poolA + poolB) * (1 - commissionRate) / pool_i
 */
@Service
@RequiredArgsConstructor
public class OddsService {

    private final OddsConfig oddsConfig;

    /**
     * 根据奖池计算双方赔率
     *
     * @param poolA A队奖池
     * @param poolB B队奖池
     * @return Map with keys "oddsA" and "oddsB"
     */
    /**
     * 虚拟底池：空方至少视为有 1 积分，防止空池赔率爆表。
     * 好处：无人下注时赔率 1.80/1.80（均衡），一方空时给对方合理赔率。
     */
    public Map<String, BigDecimal> calculate(long poolA, long poolB) {
        long effectiveA = Math.max(poolA, 1L);
        long effectiveB = Math.max(poolB, 1L);

        BigDecimal totalPool = BigDecimal.valueOf(effectiveA + effectiveB);
        BigDecimal netPool = totalPool.multiply(BigDecimal.ONE.subtract(oddsConfig.getCommissionRate()));

        BigDecimal rawA = netPool.divide(BigDecimal.valueOf(effectiveA), 4, RoundingMode.DOWN)
                .setScale(2, RoundingMode.DOWN);
        BigDecimal rawB = netPool.divide(BigDecimal.valueOf(effectiveB), 4, RoundingMode.DOWN)
                .setScale(2, RoundingMode.DOWN);

        BigDecimal oddsA = rawA.max(oddsConfig.getMinOdds()).min(oddsConfig.getMaxOdds());
        BigDecimal oddsB = rawB.max(oddsConfig.getMinOdds()).min(oddsConfig.getMaxOdds());

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("oddsA", oddsA);
        result.put("oddsB", oddsB);
        return result;
    }

    /**
     * 估算返还金额
     *
     * @param betAmount 下注金额
     * @param odds      锁定赔率
     * @return 返还金额（向下取整）
     */
    public long estimateReturn(int betAmount, BigDecimal odds) {
        return BigDecimal.valueOf(betAmount).multiply(odds).setScale(0, RoundingMode.DOWN).longValue();
    }
}