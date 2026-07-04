package com.egg.esports.module.loan;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.config.LoanConfig;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanStipendScheduler {

    private final EggUserMapper userMapper;
    private final LoanService loanService;
    private final LoanConfig loanConfig;

    /**
     * 每天 00:01 执行：为符合条件的用户发放每日补贴
     */
    @Scheduled(cron = "0 1 0 * * ?")
    public void processDailyStipends() {
        LocalDate today = LocalDate.now();
        int stipend = loanConfig.getDailyStipend();

        // 查找所有符合条件的用户：积分 <= 0 且 |loan_amount| >= loan_limit 且今天未领取
        List<EggUser> eligibleUsers = userMapper.selectList(
                new LambdaQueryWrapper<EggUser>()
                        .le(EggUser::getPoints, 0)
                        .lt(EggUser::getLoanAmount, 0)
        );

        int count = 0;
        for (EggUser user : eligibleUsers) {
            int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
            int loanLimit = user.getLoanLimit() != null ? user.getLoanLimit() : loanConfig.getMaxLoan();

            // 检查 |loan| >= loan_limit
            if (Math.abs(loanAmount) < loanLimit) {
                continue;
            }

            // 检查今天是否已领取
            if (user.getLastStipendDate() != null && user.getLastStipendDate().equals(today)) {
                continue;
            }

            // 原子更新：加积分 + 设置日期
            // 乐观锁：只有 last_stipend_date != today（含 NULL）时才能领取
            // 防止与用户主动领取（LoanController.stipend）并发时同一天重复发放
            int updated = userMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, user.getId())
                            .and(w -> w.isNull(EggUser::getLastStipendDate)
                                    .or().ne(EggUser::getLastStipendDate, today))
                            .setSql("points = points + " + stipend)
                            .set(EggUser::getLastStipendDate, today)
            );

            if (updated > 0) {
                count++;
            }
        }

        if (count > 0) {
            log.info("每日补贴自动发放完成: 共 {} 名用户", count);
        }
    }
}
