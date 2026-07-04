package com.egg.esports.module.match;

import com.egg.esports.BaseH2Test;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.bet.EggBet;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * EggMatchService 测试（结算 + 自动结算幂等）
 * <p>覆盖任务书用例 4.1-4.3，其中 4.3 是 BUG #1 回归测试。
 */
@DisplayName("EggMatchService 赛事结算")
class EggMatchServiceTest extends BaseH2Test {

    @Autowired
    private EggMatchService matchService;

    @Autowired
    private EggMatchMapper matchMapper;

    @Autowired
    private EggBetMapper betMapper;

    @Autowired
    private EggUserMapper userMapper;

    /** 创建并插入赛事 */
    private EggMatch createMatch(long id, int status, Integer result, LocalDateTime settledAt, long poolA, long poolB) {
        EggMatch match = new EggMatch();
        match.setId(id);
        match.setGameId(1L);
        match.setTitle("测试赛事" + id);
        match.setTeamA("A队");
        match.setTeamB("B队");
        match.setTournament("测试联赛");
        match.setMatchTime(LocalDateTime.now().plusDays(1));
        match.setStatus(status);
        match.setResult(result);
        match.setPoolA(poolA);
        match.setPoolB(poolB);
        match.setSettledAt(settledAt);
        matchMapper.insert(match);
        return match;
    }

    /** 创建并插入用户 */
    private EggUser createUser(long id, int points) {
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
        user.setLoanAmount(0);
        user.setLoanLimit(2000);
        userMapper.insert(user);
        return user;
    }

    /** 手动插入一条待结算 bet */
    private EggBet insertBet(long id, long userId, long matchId, int betSide, int amount) {
        EggBet bet = new EggBet();
        bet.setId(id);
        bet.setUserId(userId);
        bet.setMatchId(matchId);
        bet.setBetSide(betSide);
        bet.setBetAmount(amount);
        bet.setStatus(0);
        bet.setTeamASnapshot("A队");
        bet.setTeamBSnapshot("B队");
        bet.setCreateTime(LocalDateTime.now());
        betMapper.insert(bet);
        return bet;
    }

    @Test
    @DisplayName("4.1 settle 手动结算：设置 result、settledAt，联动结算押注")
    void testSettle_manual() {
        // Given: match status=2, result=null, 有 1 笔待结算 bet（押 A 队）
        createUser(1L, 1000);
        createMatch(1L, 2, null, null, 300L, 100L);
        insertBet(1L, 1L, 1L, 0, 200); // 押 A 队 200

        // When: settle(matchId, 0) A 胜
        EggMatch result = matchService.settle(1L, 0);

        // Then
        assertEquals(0, result.getResult(), "match.result 应为 0（A 胜）");
        // 注意：settle() 返回的对象在最后用单独 UPDATE 设置 settledAt，未回写到返回对象，
        // 所以 result.getSettledAt() 可能为 null，需从 DB 查询验证
        EggMatch updated = matchMapper.selectById(1L);
        assertNotNull(updated.getSettledAt(), "DB 中 settledAt 应非空");

        EggBet bet = betMapper.selectById(1L);
        assertEquals(1, bet.getStatus(), "bet 应被结算为猜中状态（押 A 队，A 胜）");
    }

    @Test
    @DisplayName("4.2 settle 重复结算被拒绝")
    void testSettle_duplicateRejected() {
        // Given: match result=0（已结算）
        createMatch(1L, 2, 0, LocalDateTime.now(), 300L, 100L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                matchService.settle(1L, 0));
        assertEquals("该赛事已结算，不可重复结算", ex.getMessage());
    }

    @Test
    @DisplayName("4.3 autoSettlePending 幂等性（BUG #1 回归）：二次调用不重复结算")
    void testAutoSettlePending_idempotent() {
        // Given: match status=2, result=0, settledAt=null, 有 1 笔待结算 bet
        createUser(1L, 1000);
        createMatch(1L, 2, 0, null, 300L, 100L);
        insertBet(1L, 1L, 1L, 0, 200); // 押 A 队 200

        // When: 第一次 autoSettlePending()
        int count1 = matchService.autoSettlePending();

        // Then: bet 被结算，match.settledAt 被设置，count=1
        assertEquals(1, count1, "第一次应结算 1 场赛事");

        EggBet betAfter1 = betMapper.selectById(1L);
        assertEquals(1, betAfter1.getStatus(), "bet 应被结算");
        EggMatch matchAfter1 = matchMapper.selectById(1L);
        assertNotNull(matchAfter1.getSettledAt(), "settledAt 应被设置");

        // 记录第一次结算后的积分
        EggUser userAfter1 = userMapper.selectById(1L);
        int pointsAfter1 = userAfter1.getPoints();

        // When: 第二次 autoSettlePending()（模拟定时器再次触发）
        int count2 = matchService.autoSettlePending();

        // Then: selectFinishedWithResult 返回空（settledAt != null），count=0
        assertEquals(0, count2, "第二次应无赛事可结算（settledAt 已设置）");

        // bet 没有被重复结算（status 不变）
        EggBet betAfter2 = betMapper.selectById(1L);
        assertEquals(1, betAfter2.getStatus(), "bet 状态不应变化");

        // 积分没有再次增加
        EggUser userAfter2 = userMapper.selectById(1L);
        assertEquals(pointsAfter1, userAfter2.getPoints(), "积分不应二次增加");
    }
}
