package com.egg.esports.module.dev;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.common.Result;
import com.egg.esports.common.annotation.RequireAdmin;
import com.egg.esports.module.bet.BetSettleService;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.bot.BotBetService;
import com.egg.esports.module.bot.BotUserService;
import com.egg.esports.module.comment.EggComment;
import com.egg.esports.module.comment.EggCommentMapper;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 演示数据一键灌入（C2/C3/D1）
 * RequireAdmin 保护，生产环境不暴露 profile 开关
 */
@Slf4j
@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeedController {

    private final EggMatchMapper matchMapper;
    private final EggUserMapper userMapper;
    private final EggCommentMapper commentMapper;
    private final EggBetMapper betMapper;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final BotUserService botUserService;
    private final BotBetService botBetService;
    private final BetSettleService betSettleService;

    // --- 瓦真队名池 ---
    private static final String[] TEAMS = {
            "EDG", "FPX", "TE", "JDG", "BLG", "WOL", "DRG", "NOVA", "TYLOO", "AG"
    };
    private static final String[] TOURNAMENTS = {
            "VCT CN 演示赛", "EGG杯·瓦洛兰特邀请赛", "VCT CN 演示赛·季后赛", "EGG杯·校园冠军赛"
    };

    // 瓦话术评论池
    private static final String[] COMMENT_POOL = {
            "这把EDG让一追二？Haven已经13-4了太稳了",
            "FPX这枪法今天在线，爆头率离谱",
            "TE的烟位太顶了，每次封点都恰到好处",
            "JDG这残局处理666，1v2都能翻",
            "BLG今天状态拉满，感觉要2-0带走",
            "WOL的捷风切枪太快了，根本反应不过来",
            "DRG这波配合行云流水，直接平推了",
            "Haven 13-4 太狠了，EDG这图池深啊",
            "Bind这张图FPX是真会玩，战术拉满",
            "Ascent这局拉扯太精彩了，双方都不给机会",
            "Lotus这图TE选得妙，直接把对手整不会了",
            "Split这张图就是枪法图，谁枪硬谁赢",
            "Sunset这局太焦灼了，比分咬得死死的",
            "这赔率有点香，我押EDG了兄弟们跟不跟",
            "已押TE，感觉今天TE要爆冷",
            "JDG这赔率1.82有点低了，庄家看好啊",
            "这场我看好BLG，数据面占优",
            "WOL让一追二剧本？我先押一手",
            " DRG今天首发换人了，观望一下",
            "NOVA这队新人枪是真猛，敢打敢拼",
            "TYLOO老牌劲旅了，经验丰富",
            "AG这支队伍韧性强，经常翻盘",
            "这场BO3感觉要打满三图，刺激",
            "已梭哈，赢了会所嫩模输了下海干活",
            "理性押注，小赌怡情兄弟们",
            "这场我研究过了，数据支持EDG",
            "FPX最近三场2胜1负，状态起伏",
            "TE这赛季黑马，值得关注",
            "JDG临场换阵了，有点东西",
            "看了VCT CN，感觉国内瓦水平越来越高了"
    };

    private static final String[] REPLY_POOL = {
            "同感，这场确实精彩",
            "哈哈哈确实，枪法太离谱了",
            "有道理，战术层面确实顶",
            "我也这么觉得，押了同队",
            "别奶别奶，立flag必倒",
            "确实，这波操作我给满分",
            "兄弟眼光可以，跟了",
            "这分析到位，学到了"
    };

    @RequireAdmin
    @PostMapping("/demo")
    public Result<Map<String, Object>> seedDemo() {
        Map<String, Object> result = new HashMap<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();

        // 1. 清演示比赛（tournament LIKE 'EGG演示%' OR 'VCT CN 演示赛%' OR 'EGG杯%'）
        int deletedMatches = 0;
        try {
            deletedMatches += jdbcTemplate.update("DELETE FROM egg_comment WHERE match_id IN (SELECT id FROM egg_match WHERE tournament LIKE 'EGG演示%' OR tournament LIKE 'VCT CN 演示赛%' OR tournament LIKE 'EGG杯%')");
        } catch (Exception ignored) {}
        try {
            deletedMatches += jdbcTemplate.update("DELETE FROM egg_bet WHERE match_id IN (SELECT id FROM egg_match WHERE tournament LIKE 'EGG演示%' OR tournament LIKE 'VCT CN 演示赛%' OR tournament LIKE 'EGG杯%')");
        } catch (Exception ignored) {}
        try {
            int d = jdbcTemplate.update("DELETE FROM egg_match WHERE tournament LIKE 'EGG演示%' OR tournament LIKE 'VCT CN 演示赛%' OR tournament LIKE 'EGG杯%'");
            deletedMatches = d;
        } catch (Exception e) {
            log.warn("delete demo matches failed: {}", e.getMessage());
        }
        result.put("deletedMatches", deletedMatches);

        // 2. 确保机器人存在（托底用）
        botUserService.ensureBotUsers();

        // 3. 确保演示账号存在（D1）
        ensureDemoAccounts();

        // 4. 插入18场瓦比赛：6已结束 + 6进行中 + 6未开始(未来24h)
        List<EggMatch> inserted = new ArrayList<>();
        List<Long> notStartedIds = new ArrayList<>();
        List<Long> finishedIds = new ArrayList<>();

        // 6已结束：过去7天内
        for (int i = 0; i < 6; i++) {
            EggMatch m = buildMatch(rnd, now, -7 * 24 + i * 20, 2);
            int r = rnd.nextInt(0, 2); // 0=A胜 1=B胜
            m.setResult(r);
            // score 2-0 or 2-1 style: use 2/1 or 2/0 map logic via MatchQueryService
            if (r == 0) { m.setScoreA(2); m.setScoreB(rnd.nextInt(0, 2)); }
            else { m.setScoreA(rnd.nextInt(0, 2)); m.setScoreB(2); }
            m.setSettledAt(null); // 待 settle 流程写入
            matchMapper.insert(m);
            inserted.add(m);
            finishedIds.add(m.getId());
        }
        // 6进行中：过去3小时内开始
        for (int i = 0; i < 6; i++) {
            EggMatch m = buildMatch(rnd, now, -3 + i, 1);
            m.setScoreA(rnd.nextInt(0, 10));
            m.setScoreB(rnd.nextInt(0, 10));
            matchMapper.insert(m);
            inserted.add(m);
        }
        // 6未开始：未来24h内
        for (int i = 0; i < 6; i++) {
            int hoursAhead = 1 + rnd.nextInt(0, 24); // 1-24h
            int minuteJitter = rnd.nextInt(0, 60);
            EggMatch m = buildMatch(rnd, now, hoursAhead, 0);
            // 微调分钟
            m.setMatchTime(m.getMatchTime().plusMinutes(minuteJitter));
            m.setBetDeadline(m.getMatchTime().minusMinutes(5));
            matchMapper.insert(m);
            inserted.add(m);
            notStartedIds.add(m.getId());
        }
        result.put("insertedMatches", inserted.size());
        result.put("finishedIds", finishedIds.size());
        result.put("notStartedIds", notStartedIds.size());

        // 5. 对每场未开始调 BotBetService
        int botBets = 0;
        for (Long mid : notStartedIds) {
            try {
                botBets += botBetService.placeBotBetsForMatch(mid);
            } catch (Exception e) {
                log.warn("bot bet failed for match {}: {}", mid, e.getMessage());
            }
        }
        result.put("botBetsPlaced", botBets);

        // 6. 对已结束随机result并settle
        int settled = 0;
        for (Long mid : finishedIds) {
            try {
                EggMatch m = matchMapper.selectById(mid);
                if (m != null && m.getResult() != null) {
                    betSettleService.settleBet(mid, m.getResult());
                    // 标记已结算
                    jdbcTemplate.update("UPDATE egg_match SET settled_at = NOW() WHERE id = ?", mid);
                    settled++;
                }
            } catch (Exception e) {
                log.warn("settle failed for match {}: {}", mid, e.getMessage());
            }
        }
        result.put("settledMatches", settled);

        // 7. 种子排行榜 25人
        int leaderboardCount = seedLeaderboard();
        result.put("leaderboardUsers", leaderboardCount);

        // 8. 种子评论
        int commentCount = seedComments(inserted);
        result.put("comments", commentCount);

        log.info("Seed demo done: {}", result);
        return Result.success(result);
    }

    private EggMatch buildMatch(ThreadLocalRandom rnd, LocalDateTime now, int hoursOffset, int status) {
        String teamA = TEAMS[rnd.nextInt(TEAMS.length)];
        String teamB;
        do { teamB = TEAMS[rnd.nextInt(TEAMS.length)]; } while (teamB.equals(teamA));
        String tournament = TOURNAMENTS[rnd.nextInt(TOURNAMENTS.length)];
        // EGG演示前缀用于清理
        if (!tournament.startsWith("EGG") && !tournament.startsWith("VCT")) {
            tournament = "EGG演示-" + tournament;
        }
        // 确保 tournament 含 EGG 或 VCT 演示关键字以便清理
        if (tournament.equals("VCT CN 演示赛") || tournament.equals("VCT CN 演示赛·季后赛")) {
            // ok
        } else if (!tournament.contains("EGG")) {
            tournament = "EGG演示-" + tournament;
        }

        LocalDateTime matchTime = now.plusHours(hoursOffset);
        EggMatch m = new EggMatch();
        m.setExternalId("demo-" + System.nanoTime() + "-" + rnd.nextInt(100000));
        m.setGameId(1L);
        m.setTitle(teamA + " vs " + teamB);
        m.setTeamA(teamA);
        m.setTeamB(teamB);
        m.setTeamALogo("/uploads/team-" + teamA.toLowerCase() + ".png");
        m.setTeamBLogo("/uploads/team-" + teamB.toLowerCase() + ".png");
        m.setTournament(tournament);
        m.setMatchTime(matchTime);
        m.setStatus(status);
        m.setScoreA(status == 2 ? rnd.nextInt(0, 3) : (status == 1 ? rnd.nextInt(0, 13) : 0));
        m.setScoreB(status == 2 ? rnd.nextInt(0, 3) : (status == 1 ? rnd.nextInt(0, 13) : 0));
        m.setCreateTime(now);
        m.setBetDeadline(matchTime.minusMinutes(5));
        m.setPoolA(0L);
        m.setPoolB(0L);
        return m;
    }

    private void ensureDemoAccounts() {
        createDemoUserIfNotExists("demo_admin", "等离子蛋花", 1, 10000);
        createDemoUserIfNotExists("demo_player", "预言家001", 0, 5000);
        createDemoUserIfNotExists("demo_new", "新用户", 0, 100);
    }

    private void createDemoUserIfNotExists(String username, String nickname, int role, int points) {
        EggUser exist = userMapper.selectOne(new LambdaQueryWrapper<EggUser>().eq(EggUser::getUsername, username));
        if (exist != null) {
            // 更新密码为 demo123，保证可登录
            exist.setPassword(passwordEncoder.encode("demo123"));
            exist.setNickname(nickname);
            exist.setRole(role);
            if (exist.getPoints() == null || exist.getPoints() < points) {
                exist.setPoints(points);
            }
            userMapper.updateById(exist);
            return;
        }
        EggUser u = new EggUser();
        u.setUsername(username);
        u.setNickname(nickname);
        u.setPassword(passwordEncoder.encode("demo123"));
        u.setAvatar("/uploads/avatar-demo.png");
        u.setPoints(points);
        u.setNetProfit(0);
        u.setTotalBets(0);
        u.setCorrectBets(0);
        u.setWinStreak(0);
        u.setMaxWinStreak(0);
        u.setRole(role);
        u.setLoanAmount(0);
        u.setLoanLimit(2000);
        u.setCreateTime(LocalDateTime.now());
        userMapper.insert(u);
    }

    private int seedLeaderboard() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int created = 0;
        // 25个 demo_01..25，points 800-3200 阶梯
        for (int i = 1; i <= 25; i++) {
            String username = String.format("demo_%02d", i);
            EggUser exist = userMapper.selectOne(new LambdaQueryWrapper<EggUser>().eq(EggUser::getUsername, username));
            if (exist != null) {
                // 更新阶梯分数（800 + (25-i)*~100 阶梯，头名3200左右）
                int points = 800 + (25 - i) * 96 + rnd.nextInt(0, 40); // 800..~3160
                exist.setPoints(points);
                // 按比例生成战绩
                int totalBets = 10 + rnd.nextInt(0, 40);
                int correctBets = (int) (totalBets * (0.45 + rnd.nextDouble() * 0.25));
                if (correctBets > totalBets) correctBets = totalBets;
                exist.setTotalBets(totalBets);
                exist.setCorrectBets(correctBets);
                exist.setWinStreak(rnd.nextInt(0, 5));
                exist.setMaxWinStreak(rnd.nextInt(2, 8));
                exist.setNetProfit(points - 1000 + rnd.nextInt(-200, 200));
                userMapper.updateById(exist);
                created++;
                continue;
            }
            int points = 800 + (25 - i) * 96 + rnd.nextInt(0, 40);
            int totalBets = 10 + rnd.nextInt(0, 40);
            int correctBets = (int) (totalBets * (0.45 + rnd.nextDouble() * 0.25));
            if (correctBets > totalBets) correctBets = totalBets;
            EggUser u = new EggUser();
            u.setUsername(username);
            u.setNickname("演示玩家" + String.format("%02d", i));
            u.setPassword(passwordEncoder.encode("demo123"));
            u.setAvatar("/uploads/avatar-demo-" + String.format("%02d", i) + ".png");
            u.setPoints(points);
            u.setNetProfit(points - 1000 + rnd.nextInt(-200, 200));
            u.setTotalBets(totalBets);
            u.setCorrectBets(correctBets);
            u.setWinStreak(rnd.nextInt(0, 5));
            u.setMaxWinStreak(rnd.nextInt(2, 8));
            u.setRole(0);
            u.setLoanAmount(0);
            u.setLoanLimit(2000);
            u.setCreateTime(LocalDateTime.now().minusDays(rnd.nextInt(0, 30)));
            userMapper.insert(u);
            created++;
        }
        return created;
    }

    private int seedComments(List<EggMatch> matches) {
        if (matches == null || matches.isEmpty()) return 0;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // 收集可用 userIds：demo_01..25 + demo_player + bot
        List<Long> userIds = new ArrayList<>();
        List<EggUser> demoUsers = userMapper.selectList(new LambdaQueryWrapper<EggUser>().likeRight(EggUser::getUsername, "demo_"));
        for (EggUser u : demoUsers) userIds.add(u.getId());
        List<EggUser> bots = userMapper.selectList(new LambdaQueryWrapper<EggUser>().likeRight(EggUser::getUsername, "bot_"));
        for (EggUser b : bots) userIds.add(b.getId());
        if (userIds.isEmpty()) {
            // fallback: any user
            List<EggUser> any = userMapper.selectList(new LambdaQueryWrapper<EggUser>().last("LIMIT 5"));
            for (EggUser u : any) userIds.add(u.getId());
        }
        if (userIds.isEmpty()) return 0;

        int total = 0;
        LocalDateTime now = LocalDateTime.now();
        for (EggMatch m : matches) {
            int count = rnd.nextInt(8, 13); // 8-12 条顶级
            List<Long> topIds = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Long uid = userIds.get(rnd.nextInt(userIds.size()));
                String content = COMMENT_POOL[rnd.nextInt(COMMENT_POOL.length)];
                EggComment c = new EggComment();
                c.setUserId(uid);
                c.setMatchId(m.getId());
                c.setParentId(null);
                c.setContent(content);
                c.setStatus(1);
                c.setCreateTime(now.minusDays(rnd.nextInt(0, 7)).minusHours(rnd.nextInt(0, 24)).minusMinutes(rnd.nextInt(0, 60)));
                commentMapper.insert(c);
                topIds.add(c.getId());
                total++;

                // 2-3 条回复：20-30% 的顶级评论带回复
                if (rnd.nextDouble() < 0.28 && topIds.size() > 0) {
                    int replyCount = rnd.nextInt(2, 4); // 2-3
                    for (int r = 0; r < replyCount; r++) {
                        Long replyUid = userIds.get(rnd.nextInt(userIds.size()));
                        if (replyUid.equals(uid)) continue;
                        EggComment reply = new EggComment();
                        reply.setUserId(replyUid);
                        reply.setMatchId(m.getId());
                        reply.setParentId(c.getId());
                        reply.setReplyToId(c.getId());
                        reply.setReplyToUid(uid);
                        reply.setContent(REPLY_POOL[rnd.nextInt(REPLY_POOL.length)]);
                        reply.setStatus(1);
                        reply.setCreateTime(c.getCreateTime().plusMinutes(rnd.nextInt(1, 120)));
                        commentMapper.insert(reply);
                        total++;
                    }
                }
            }
        }
        return total;
    }
}
