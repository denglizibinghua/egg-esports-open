package com.egg.esports.module.loan;

import com.egg.esports.BaseH2Test;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LoanService 测试（还款 + 补贴 + 救济贷款）
 * <p>覆盖任务书用例 3.1-3.5，其中 3.3/3.4 是并发 bug 回归测试。
 */
@DisplayName("LoanService 贷款与补贴")
class LoanServiceTest extends BaseH2Test {

    @Autowired
    private LoanService loanService;

    @Autowired
    private EggUserMapper userMapper;

    /** 创建并插入用户，可指定 points/loanAmount/loanLimit/lastStipendDate */
    private EggUser createUser(long id, int points, int loanAmount, int loanLimit, LocalDate lastStipend) {
        EggUser user = new EggUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPassword("pwd");
        user.setPoints(points);
        user.setNetProfit(0);
        user.setTotalBets(0);
        user.setCorrectBets(0);
        user.setWinStreak(0);
        user.setMaxWinStreak(0);
        user.setRole(0);
        user.setLoanAmount(loanAmount);
        user.setLoanLimit(loanLimit);
        user.setLastStipendDate(lastStipend);
        userMapper.insert(user);
        return user;
    }

    @Test
    @DisplayName("3.1 autoRepay 基本流程：50% 利润还贷，部分还款")
    void testAutoRepay_partial() {
        // Given: user loanAmount=-200, profit=100
        createUser(1L, 1000, -200, 2000, null);

        // When: autoRepay(userId, profit=100)
        int[] result = loanService.autoRepay(1L, 100);

        // Then: repayAmount = min(100*0.5, 200) = 50, profitAfterRepay = 50
        assertEquals(50, result[0], "profitAfterRepay 应为 50");
        assertEquals(50, result[1], "repayAmount 应为 50");

        EggUser updated = userMapper.selectById(1L);
        assertEquals(-150, updated.getLoanAmount(), "loanAmount 应从 -200 变为 -150");
    }

    @Test
    @DisplayName("3.2 autoRepay 全部还清：利润超过债务，清零")
    void testAutoRepay_fullRepaid() {
        // Given: user loanAmount=-200, profit=500
        createUser(1L, 1000, -200, 2000, null);

        // When
        int[] result = loanService.autoRepay(1L, 500);

        // Then: repayAmount = min(500*0.5, 200) = 200, profitAfterRepay = 300
        assertEquals(300, result[0], "profitAfterRepay 应为 300");
        assertEquals(200, result[1], "repayAmount 应为 200（清零债务）");

        EggUser updated = userMapper.selectById(1L);
        assertEquals(0, updated.getLoanAmount(), "loanAmount 应清零");
    }

    @Test
    @DisplayName("3.3 并发 autoRepay（BUG #3 回归）：不会超额还贷")
    void testConcurrentAutoRepay() throws Exception {
        // Given: user loanAmount=-200
        createUser(1L, 1000, -200, 2000, null);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<int[]>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                return loanService.autoRepay(1L, 300); // 每个线程 profit=300
            }));
        }

        startLatch.countDown();
        int totalRepaid = 0;
        int zeroReturnCount = 0;
        for (Future<int[]> f : futures) {
            int[] r = f.get(10, TimeUnit.SECONDS);
            totalRepaid += r[1];
            if (r[1] == 0) {
                zeroReturnCount++;
            }
        }
        executor.shutdown();

        // Then: 总还贷金额 ≤ 200（不会超额）
        assertTrue(totalRepaid <= 200, "总还贷金额应 ≤ 200，实际 " + totalRepaid);

        // 至少一个线程的 updated==0 触发了 return [profit, 0]
        assertTrue(zeroReturnCount >= 1, "至少一个线程应返回 repayAmount=0");

        EggUser updated = userMapper.selectById(1L);
        // 预期：一个线程还 150，另一个被拒。loanAmount = -200 + 150 = -50
        // 或极端情况：两个都部分还款成功（不可能，因为乐观锁）。所以必为 -50
        assertEquals(-50, updated.getLoanAmount(),
                "loanAmount 应为 -50（仅一个线程还了 150）");
    }

    @Test
    @DisplayName("3.4 并发 tryDailyStipend（BUG #4 回归）：同日只发一次")
    void testConcurrentTryDailyStipend() throws Exception {
        // Given: user points=0, loanAmount=-2000(达上限), lastStipendDate=null
        createUser(1L, 0, -2000, 2000, null);

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                EggUser u = userMapper.selectById(1L);
                return loanService.tryDailyStipend(u);
            }));
        }

        startLatch.countDown();
        int totalGiven = 0;
        int zeroReturnCount = 0;
        for (Future<Integer> f : futures) {
            int given = f.get(10, TimeUnit.SECONDS);
            totalGiven += given;
            if (given == 0) {
                zeroReturnCount++;
            }
        }
        executor.shutdown();

        // Then: points 只增加 100（配置的 dailyStipend）
        assertEquals(100, totalGiven, "总发放金额应只为一次补贴 100");
        assertTrue(zeroReturnCount >= 1, "至少一个线程应返回 0（updated==0）");

        EggUser updated = userMapper.selectById(1L);
        assertEquals(100, updated.getPoints(), "points 只应增加 100");
        assertEquals(LocalDate.now(), updated.getLastStipendDate(), "lastStipendDate 应为今天");
    }

    @Test
    @DisplayName("3.5 救济贷款：积分归零时自动发放，扣增贷款余额")
    void testTryReliefLoan() {
        // Given: user points=0, loanAmount=0, loanLimit=2000
        EggUser user = createUser(1L, 0, 0, 2000, null);

        // When
        boolean result = loanService.tryReliefLoan(user);

        // Then
        assertTrue(result, "应成功发放救济贷款");

        EggUser updated = userMapper.selectById(1L);
        assertEquals(500, updated.getPoints(), "points 应增加 reliefAmount=500");
        assertEquals(-500, updated.getLoanAmount(), "loanAmount 应变为 -500");
    }

    @Test
    @DisplayName("3.5b 救济贷款：积分 > 0 时不发放")
    void testTryReliefLoan_hasPoints() {
        EggUser user = createUser(1L, 100, 0, 2000, null);

        boolean result = loanService.tryReliefLoan(user);

        assertFalse(result, "积分 > 0 时不应发放救济贷款");
    }
}
