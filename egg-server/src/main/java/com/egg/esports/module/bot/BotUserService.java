package com.egg.esports.module.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 机器人用户服务：创建 12-20 个机器人账号
 * username=bot_01..bot_20, role=0, points=5000
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotUserService {

    private final EggUserMapper eggUserMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String[] NICKNAMES = {
            "瓦学长", "枪男", "爆头哥", "狙神", "烟位大师",
            "残局王", "捷风本风", "雷兹玩家", "贤者奶妈", "夜露小子",
            "霓虹疾行", "零封高手", "蝰蛇控场", "海港轰炸", "炼狱火男",
            "幽影刺客", "盖可萌新", "芙拉小粉", "钱博尔", "钢索控场"
    };

    /**
     * 确保机器人用户存在，幂等创建。创建 15 个（12-20 范围内）。
     * @return 创建或已存在的机器人数量
     */
    public int ensureBotUsers() {
        Long existing = eggUserMapper.selectCount(
                new LambdaQueryWrapper<EggUser>().likeRight(EggUser::getUsername, "bot_"));
        if (existing >= 12) {
            log.info("Bot users already exist: {}", existing);
            return existing.intValue();
        }
        int target = 15;
        int toCreate = target - existing.intValue();
        // 已有部分 bot，则从 max 序号继续
        int startIdx = 1;
        if (existing > 0) {
            List<EggUser> bots = eggUserMapper.selectList(
                    new LambdaQueryWrapper<EggUser>().likeRight(EggUser::getUsername, "bot_"));
            int maxNum = 0;
            for (EggUser u : bots) {
                try {
                    String suffix = u.getUsername().substring(4);
                    int n = Integer.parseInt(suffix);
                    if (n > maxNum) maxNum = n;
                } catch (Exception ignored) {}
            }
            startIdx = maxNum + 1;
        }
        int created = 0;
        for (int i = 0; i < toCreate; i++) {
            int botNum = startIdx + i;
            String username = String.format("bot_%02d", botNum);
            // 幂等检查
            EggUser exist = eggUserMapper.selectOne(
                    new LambdaQueryWrapper<EggUser>().eq(EggUser::getUsername, username));
            if (exist != null) continue;

            int nickIdx = (botNum - 1) % NICKNAMES.length;
            EggUser bot = new EggUser();
            bot.setUsername(username);
            bot.setNickname(NICKNAMES[nickIdx]);
            bot.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            bot.setAvatar(String.format("/uploads/bot-%02d.png", botNum));
            bot.setPoints(5000);
            bot.setNetProfit(0);
            bot.setTotalBets(0);
            bot.setCorrectBets(0);
            bot.setWinStreak(0);
            bot.setMaxWinStreak(0);
            bot.setRole(0);
            bot.setLoanAmount(0);
            bot.setLoanLimit(2000);
            bot.setCreateTime(LocalDateTime.now());
            eggUserMapper.insert(bot);
            created++;
        }
        log.info("Created {} bot users, total now {}", created, existing + created);
        return existing.intValue() + created;
    }

    /**
     * 获取所有机器人用户
     */
    public List<EggUser> getBotUsers() {
        return eggUserMapper.selectList(
                new LambdaQueryWrapper<EggUser>().likeRight(EggUser::getUsername, "bot_"));
    }
}
