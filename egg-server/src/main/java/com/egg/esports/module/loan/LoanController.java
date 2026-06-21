package com.egg.esports.module.loan;

import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/loan")
public class LoanController {

    @Autowired
    private EggUserMapper eggUserMapper;

    @Autowired
    private LoanService loanService;

    /**
     * 获取当前贷款状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = eggUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
        int loanLimit = user.getLoanLimit() != null ? user.getLoanLimit() : 2000;
        int rankingScore = loanService.getRankingScore(user);
        int activeBets = loanService.getActiveBetAmount(userId);

        // 判断是否可领取每日补贴
        boolean canClaimStipend = user.getPoints() <= 0
                && Math.abs(loanAmount) >= loanLimit
                && (user.getLastStipendDate() == null || !user.getLastStipendDate().equals(LocalDate.now()));

        // 判断是否可申请救济贷款（积分 < 500 且贷款未满）
        boolean canClaimLoan = user.getPoints() < 500 && Math.abs(loanAmount) < loanLimit;

        Map<String, Object> data = new HashMap<>();
        data.put("loanAmount", loanAmount);
        data.put("loanLimit", loanLimit);
        data.put("rankingScore", rankingScore);
        data.put("activeBets", activeBets);
        data.put("canClaimStipend", canClaimStipend);
        data.put("canClaimLoan", canClaimLoan);

        return Result.success(data);
    }

    /**
     * 领取每日补贴
     */
    @RateLimit(window = 60, maxRequests = 3, message = "操作过于频繁，请1分钟后再试")
    @PostMapping("/stipend")
    public Result<Map<String, Object>> claimStipend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = eggUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        int amount = loanService.tryDailyStipend(user);
        if (amount == 0) {
            return Result.error("不符合领取条件");
        }

        // 重新读取用户数据
        user = eggUserMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("stipendAmount", amount);
        data.put("points", user.getPoints());
        data.put("lastStipendDate", user.getLastStipendDate());

        return Result.success(data);
    }

    /**
     * 申请救济贷款（手动）
     */
    @RateLimit(window = 60, maxRequests = 3, message = "操作过于频繁，请1分钟后再试")
    @PostMapping("/relief")
    public Result<Map<String, Object>> claimRelief(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = eggUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 贷款触发条件：积分 < 500（救济金额）且未达贷款上限
        if (user.getPoints() >= 500) {
            return Result.error("积分充足，无需借款");
        }
        int loanAmount = user.getLoanAmount() != null ? user.getLoanAmount() : 0;
        int loanLimit = user.getLoanLimit() != null ? user.getLoanLimit() : 2000;
        if (Math.abs(loanAmount) >= loanLimit) {
            return Result.error("贷款已达上限");
        }

        boolean ok = loanService.tryReliefLoan(user, 500);
        if (!ok) {
            return Result.error("借款失败，可能已被其他操作抢先");
        }

        user = eggUserMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("points", user.getPoints());
        data.put("loanAmount", user.getLoanAmount());

        return Result.success(data);
    }

    /**
     * 主动还款
     */
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请1分钟后再试")
    @PostMapping("/repay")
    public Result<Map<String, Object>> repay(@RequestBody Map<String, Integer> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Integer amount = body.get("amount");
        if (amount == null || amount <= 0) {
            return Result.error("还款金额必须大于0");
        }

        int actualRepay = loanService.manualRepay(userId, amount);
        if (actualRepay == 0) {
            return Result.error("还款失败：积分不足或无贷款");
        }

        // Re-read user for updated data
        EggUser user = eggUserMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("repaidAmount", actualRepay);
        data.put("remainingDebt", user.getLoanAmount());
        data.put("points", user.getPoints());
        return Result.success(data);
    }
}
