package com.egg.esports.module.loan;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.config.LoanConfig;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class LoanService {

    @Autowired
    private EggUserMapper eggUserMapper;

    @Autowired
    private LoanConfig loanConfig;

    @Autowired
    private EggBetMapper eggBetMapper;

    /**
     * 发放救济贷款。当用户积分不足支付下注金额且债务未达上限时，自动发放贷款。
     * @param user 用户实体
     * @param requiredAmount 需要的积分（通常是下注金额）
     * Returns true if loan was issued.
     */
    @Transactional
    @OperationLog(module = "贷款", value = "发放救济贷款")
    public boolean tryReliefLoan(EggUser user, int requiredAmount) {
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
        int loanLimit = user.getLoanLimit() != null ? user.getLoanLimit() : loanConfig.getMaxLoan();

        // 条件：积分不够支付所需金额 且 |loan_amount| < loan_limit
        if (user.getPoints() >= requiredAmount || Math.abs(loanAmount) >= loanLimit) {
            return false;
        }

        int reliefAmount = loanConfig.getReliefAmount();

        // 使用直接 SQL 更新，避免竞态条件
        int updated = eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, user.getId())
                        .eq(EggUser::getPoints, user.getPoints())  // 乐观锁：确保积分未变
                        .setSql("loan_amount = loan_amount - " + reliefAmount)
                        .setSql("points = points + " + reliefAmount)
        );

        if (updated > 0) {
            log.info("救济贷款发放: userId={}, amount={}", user.getId(), reliefAmount);
            return true;
        }
        return false;
    }

    /**
     * 自动还款。结算赢注后，50%利润用于还贷。
     * profit = winAmount - betAmount. repay = min(profit * ratio, |loan|).
     * Returns: [profitAfterRepay, loanRepaid]
     */
    @Transactional
    public int[] autoRepay(Long userId, int profit) {
        EggUser user = eggUserMapper.selectById(userId);
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;

        if (loanAmount >= 0 || profit <= 0) {
            // 无债务或无利润，不需要还款
            return new int[]{profit, 0};
        }

        int debt = Math.abs(loanAmount);
        int repayAmount = (int) Math.min(profit * loanConfig.getAutoRepayRatio(), debt);
        int profitAfterRepay = profit - repayAmount;

        if (repayAmount <= 0) {
            return new int[]{profit, 0};
        }

        // 检查还款后是否清零
        boolean fullyRepaid = (repayAmount >= debt);

        if (fullyRepaid) {
            eggUserMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, userId)
                            .set(EggUser::getLoanAmount, 0)
            );
            log.info("贷款还清: userId={}", userId);
        } else {
            // 部分还款
            eggUserMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, userId)
                            .setSql("loan_amount = loan_amount + " + repayAmount)
            );
            log.info("部分还款: userId={}, repaid={}", userId, repayAmount);
        }

        return new int[]{profitAfterRepay, repayAmount};
    }

    /**
     * 每日补贴。当用户无积分且已达贷款上限时，发放免费积分（非贷款）。
     * Returns: amount given (0 if not eligible)
     */
    @Transactional
    public int tryDailyStipend(EggUser user) {
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
        int loanLimit = user.getLoanLimit() != null ? user.getLoanLimit() : loanConfig.getMaxLoan();
        LocalDate today = LocalDate.now();

        // 条件：积分 <= 0 且 |loan| >= loan_limit 且 今天未领取
        if (user.getPoints() > 0 || Math.abs(loanAmount) < loanLimit) {
            return 0;
        }
        if (user.getLastStipendDate() != null && user.getLastStipendDate().equals(today)) {
            return 0;
        }

        int stipend = loanConfig.getDailyStipend();

        int updated = eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, user.getId())
                        .setSql("points = points + " + stipend)
                        .set(EggUser::getLastStipendDate, today)
        );

        if (updated > 0) {
            log.info("每日补贴发放: userId={}, amount={}", user.getId(), stipend);
            return stipend;
        }
        return 0;
    }

    /**
     * 计算用户当前活跃下注总额（status=0 的下注金额之和）
     */
    public int getActiveBetAmount(Long userId) {
        Integer sum = eggBetMapper.getActiveSumByUserId(userId);
        return sum != null ? sum : 0;
    }

    /**
     * 计算用户排名分数
     * R = points + active_bets + loan_amount
     * loan_amount 为负数（债务），所以债务会降低排名
     */
    public int getRankingScore(EggUser user) {
        int points = user.getPoints() != null ? user.getPoints() : 0;
        int activeBets = getActiveBetAmount(user.getId());
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
        return points + activeBets + loanAmount;
    }

    /**
     * 主动还款。用户手动还贷。
     * @param userId 用户ID
     * @param amount 要还的金额
     * @return 实际还款金额（0表示还款失败）
     */
    @Transactional
    @OperationLog(module = "贷款", value = "还款")
    public int manualRepay(Long userId, int amount) {
        if (amount <= 0) return 0;

        EggUser user = eggUserMapper.selectById(userId);
        int points = user.getPoints() != null ? user.getPoints() : 0;
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;

        // 无债务或积分不足
        if (loanAmount >= 0) return 0;
        if (points < amount) return 0;

        int debt = Math.abs(loanAmount);
        int actualRepay = Math.min(amount, debt);

        eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, userId)
                        .setSql("points = points - " + actualRepay)
                        .setSql("loan_amount = loan_amount + " + actualRepay)
        );

        log.info("手动还款: userId={}, amount={}, 剩余债务={}", userId, actualRepay, -(loanAmount + actualRepay));
        return actualRepay;
    }
}
