package com.egg.esports.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liquipedia Adapter（王者荣耀 KPL / 第五人格 IVL·IJL）
 * <p>数据源：Liquipedia MediaWiki API（免费，无 Key，限频约 60 次/小时）
 * <p>端点：GET /{game}/api.php?action=parse&page={tournament}&format=json
 * <p>特性：
 * <ul>
 *   <li>自定义 User-Agent（Liquipedia 要求标识爬虫身份）</li>
 *   <li>gzip 支持：Liquipedia 强制要求 Accept-Encoding: gzip（缺失返回 406），
 *       由 RestTemplateConfig 的 GzipSupportInterceptor 统一处理</li>
 *   <li>action=parse 返回 parse.text.* (HTML)，用 JSoup 解析 div.brkts-match 比赛块</li>
 *   <li>请求间隔 ≥ 2 秒，遵守 Liquipedia 限频</li>
 *   <li>容错解析：单场解析失败跳过，不阻塞整体</li>
 * </ul>
 * <p>注意：brkts-match 比赛块通过 span.timer-object 的 data-timestamp 提取比赛时间；
 * 缺失时使用赛事页 infobox 的 Start Date 作为 matchTime 兜底；
 * 解析带比分的已结束比赛（status=2）与未开始的 upcoming 比赛（status=0）。
 */
@Slf4j
public class LiquipediaAdapter implements GameSourceAdapter {

    /** 请求间隔（毫秒），Liquipedia 限频约 60 次/小时 → 间隔 2 秒 */
    private static final long REQUEST_INTERVAL_MS = 2000L;

    /** ISO 日期正则：如 "2025-01-15"（用于提取赛事开始日期兜底） */
    private static final Pattern ISO_DATE_PATTERN =
            Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");

    /** 匹配 infobox 中 "Start Date" 后紧跟的 ISO 日期 */
    private static final Pattern START_DATE_PATTERN = Pattern.compile(
            "Start Date:?\\s*</div>\\s*<div[^>]*>\\s*(\\d{4}-\\d{2}-\\d{2})",
            Pattern.CASE_INSENSITIVE);

    private final Long gameId;
    private final String gameName;       // honorofkings / identityv
    private final String displayName;    // 王者荣耀 / 第五人格（日志展示）
    private final String baseUrl;        // https://liquipedia.net
    private final List<String> tournaments;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param gameId       游戏ID（王者荣耀=5, 第五人格=6）
     * @param gameName     Liquipedia 子域名段（honorofkings / identityv）
     * @param displayName  中文展示名（日志用）
     * @param baseUrl      Liquipedia 根地址（https://liquipedia.net）
     * @param tournaments  赛事页面列表（逗号分隔，如 "KPL/2025/Spring,IJL/2026/Summer"）
     * @param restTemplate HTTP 客户端（已注册 gzip 拦截器）
     * @param objectMapper JSON 解析
     */
    public LiquipediaAdapter(Long gameId, String gameName, String displayName,
                             String baseUrl, String tournaments,
                             RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.gameName = gameName;
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        // 解析赛事列表（逗号分隔）
        this.tournaments = new ArrayList<>();
        if (tournaments != null && !tournaments.trim().isEmpty()) {
            for (String t : tournaments.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) {
                    this.tournaments.add(trimmed);
                }
            }
        }
    }

    @Override
    public Long getGameId() {
        return gameId;
    }

    @Override
    public List<RawMatch> fetchMatches() {
        List<RawMatch> result = new ArrayList<>();
        if (tournaments.isEmpty()) {
            log.warn("Liquipedia ({}) 未配置 tournaments，跳过同步", displayName);
            return result;
        }

        log.info("Liquipedia ({}) 开始同步，待解析赛事页: {}", displayName, tournaments);

        for (String tournament : tournaments) {
            try {
                List<RawMatch> pageMatches = fetchTournamentPage(tournament);
                result.addAll(pageMatches);
                log.info("Liquipedia ({}) 页面 {} 解析完成，本页 {} 场，累计 {} 场",
                        displayName, tournament, pageMatches.size(), result.size());
            } catch (Exception e) {
                log.error("Liquipedia ({}) 解析赛事页 {} 失败: {}", displayName, tournament, e.getMessage(), e);
            }
            // 赛事页间间隔 2 秒
            sleep(REQUEST_INTERVAL_MS);
        }

        log.info("Liquipedia ({}) 同步完成，共 {} 场赛事", displayName, result.size());
        return result;
    }

    /**
     * 拉取并解析单个赛事页面
     *
     * @param tournament 赛事页面名（如 King_Pro_League/2025/Spring）
     * @return 该页面的 RawMatch 列表
     */
    private List<RawMatch> fetchTournamentPage(String tournament) {
        List<RawMatch> pageResult = new ArrayList<>();

        // 构造 MediaWiki API URL：action=parse&page=...&format=json
        String encodedPage = URLEncoder.encode(tournament, StandardCharsets.UTF_8)
                .replace("%2F", "/");
        String url = String.format("%s/%s/api.php?action=parse&page=%s&format=json",
                baseUrl, gameName, encodedPage);
        // 排查用：打印实际请求 URL（debug 级别，生产关闭）
        log.debug("Liquipedia ({}) 请求 URL: {}", displayName, url);

        JsonNode parseNode = requestJson(url);
        if (parseNode == null) {
            log.warn("Liquipedia ({}) 页面 {} 无响应数据", displayName, tournament);
            return pageResult;
        }

        // action=parse 响应结构：{ parse: { title, pageid, text: { "*": "<html>" } } }
        JsonNode parse = parseNode.path("parse");
        if (parse.isMissingNode()) {
            // 可能是错误响应（如页面不存在）：{ error: { code, info } }
            JsonNode err = parseNode.path("error");
            if (!err.isMissingNode()) {
                log.warn("Liquipedia ({}) 页面 {} API 错误: {} - {}",
                        displayName, tournament, err.path("code").asText(), err.path("info").asText());
            } else {
                log.warn("Liquipedia ({}) 页面 {} 响应缺少 parse 节点", displayName, tournament);
            }
            return pageResult;
        }

        long pageId = parse.path("pageid").asLong(0);
        String pageTitle = parse.path("title").asText(tournament);
        String html = parse.path("text").path("*").asText("");
        if (html.isEmpty()) {
            log.warn("Liquipedia ({}) 页面 {} 解析内容为空", displayName, tournament);
            return pageResult;
        }

        // 用 JSoup 解析 HTML，提取 brkts-match 比赛块
        Document doc = Jsoup.parse(html);
        // brkts-match 块通常自带 timer-object 时间戳；infobox Start Date 仅作兜底
        LocalDateTime fallbackDate = extractStartDate(html);
        List<RawMatch> rows = parseMatchInfoBlocks(doc, pageTitle, pageId, tournament, fallbackDate);
        pageResult.addAll(rows);

        return pageResult;
    }

    /**
     * 从 HTML 文档中解析比赛块
     * <p>策略：选择 div.brkts-match 元素（Liquipedia bracket 比赛容器），
     * 从中提取队伍名（div.team-name-dynamic 的 data-team-name）、
     * 比分（div.brkts-opponent-score-inner）、胜者（brkts-opponent-win）、
     * 时间戳（span.timer-object 的 data-timestamp）。
     * <p>解析带比分的已结束比赛与未开始的 upcoming 比赛；fallbackDate 仅在比赛块自身无时间戳时兜底。
     * <p>容错：单块解析失败跳过，不阻塞整体
     */
    private List<RawMatch> parseMatchInfoBlocks(Document doc, String pageTitle, long pageId,
                                                String tournamentPage, LocalDateTime fallbackDate) {
        List<RawMatch> rows = new ArrayList<>();

        int matchIndex = 0;
        // Liquipedia bracket 比赛容器：div.brkts-match
        for (Element match : doc.select("div.brkts-match")) {
            try {
                RawMatch raw = mapMatchInfoToRawMatch(match, pageTitle, pageId,
                        matchIndex, fallbackDate);
                if (raw != null) {
                    rows.add(raw);
                    matchIndex++;
                }
            } catch (Exception e) {
                log.debug("Liquipedia ({}) 单场比赛块解析失败: {}", displayName, e.getMessage());
            }
        }

        if (rows.isEmpty()) {
            log.info("Liquipedia ({}) 页面 {} 未解析到有效比赛块", displayName, tournamentPage);
        } else {
            log.info("Liquipedia ({}) 页面 {} 解析到 {} 场比赛（兜底日期 {}）",
                    displayName, tournamentPage, rows.size(),
                    fallbackDate != null ? fallbackDate.toLocalDate() : "无");
        }
        return rows;
    }

    /**
     * 将 brkts-match 元素映射为 RawMatch
     * <p>结构：
     * <ul>
     *   <li>队伍：div.team-name-dynamic 的 data-team-name 属性（取前两个作为 A/B）</li>
     *   <li>比分：div.brkts-opponent-score-inner 的 text()（两个，顺序对应 A/B）；
     *       被父级 brkts-opponent-win 标记的一方为胜者</li>
     *   <li>胜者：含 brkts-opponent-win 类的 brkts-opponent 行（0=A 胜，1=B 胜）</li>
     *   <li>时间戳：span.timer-object 的 data-timestamp（Unix 秒，Asia/Shanghai）；
     *       缺失时用 fallbackDate 兜底</li>
     * </ul>
     *
     * @param match         brkts-match 元素
     * @param fallbackDate  赛事开始日期（比赛块无 timer-object 时的兜底 matchTime）
     * @return RawMatch；无效（缺队伍/缺比分）返回 null
     */
    private RawMatch mapMatchInfoToRawMatch(Element match, String pageTitle, long pageId,
                                            int matchIndex, LocalDateTime fallbackDate) {
        // 队伍名：从 brkts-popup 的 match-info-header-opponent > span.name 提取
        // 2026 KPL 结构：div.match-info-header-opponent > div.block-team > span.name > a
        // span.name 的 text() 含 a 的文本（如 "Dragon Ranger"）；TBD 无 a 标签直接是 "TBD"（会被 cleanTeamName 过滤）
        Elements nameEls = match.select("div.match-info-header-opponent span.name");
        if (nameEls.size() < 2) {
            // 排查用：队伍元素不足，可能页面结构变化（debug 级别）
            log.debug("Liquipedia ({}) 跳过比赛块: 队伍元素不足({})", displayName, nameEls.size());
            return null;
        }
        String teamA = cleanTeamName(nameEls.get(0).text());
        String teamB = cleanTeamName(nameEls.get(1).text());
        if (teamA.isEmpty() || teamB.isEmpty()) {
            // 排查用：队名为空（TBD/TBA 占位也会被清洗为空）
            log.debug("Liquipedia ({}) 跳过比赛块: 队名为空 teamA={}, teamB={}", displayName, teamA, teamB);
            return null;
        }

        // 比分：span.match-info-header-scoreholder-score 的 text()，顺序对应 A/B
        // 2026 结构：两个 span.match-info-header-scoreholder-score（A 比分、B 比分），
        // 中间有 match-info-header-scoreholder-divider（":"）不会被选中
        // 无比分（TBD vs TBD 显示 "vs"）视为未开始比赛（status=0 upcoming），仍解析入库以支持押注
        Elements scoreEls = match.select("span.match-info-header-scoreholder-score");
        Integer scoreA = null;
        Integer scoreB = null;
        boolean finished = false;
        if (scoreEls.size() >= 2) {
            scoreA = parseScore(scoreEls.get(0).text());
            scoreB = parseScore(scoreEls.get(1).text());
            // 双方比分均为有效整数才视为已结束
            if (scoreA != null && scoreB != null) {
                finished = true;
            } else {
                scoreA = null;
                scoreB = null;
            }
        }

        // 胜者判定：仅已结束比赛有胜者；未开始比赛 result=null
        // 2026 结构：div.match-info-header-opponent 含 match-info-header-winner 类为胜者
        Integer result = null;
        if (finished) {
            Elements opponentRows = match.select("div.match-info-header-opponent");
            if (opponentRows.size() >= 2) {
                boolean aWin = opponentRows.get(0).classNames().contains("match-info-header-winner");
                boolean bWin = opponentRows.get(1).classNames().contains("match-info-header-winner");
                if (aWin && !bWin) {
                    result = 0;  // A 胜
                } else if (bWin && !aWin) {
                    result = 1;  // B 胜
                } else if (scoreA > scoreB) {
                    // win 类缺失时用比分兜底
                    result = 0;
                } else if (scoreB > scoreA) {
                    result = 1;
                }
            } else if (scoreA > scoreB) {
                result = 0;
            } else if (scoreB > scoreA) {
                result = 1;
            }
        }

        // 时间戳：span.timer-object 的 data-timestamp（Unix 秒 → Asia/Shanghai）
        LocalDateTime matchTime = extractMatchTime(match);
        if (matchTime == null) {
            matchTime = fallbackDate;
        }
        if (matchTime == null) {
            // 既无 timer-object 也无兜底日期，无法入库 matchTime，跳过
            log.debug("Liquipedia ({}) 跳过比赛块: 无比赛时间 teamA={}", displayName, teamA);
            return null;
        }

        // externalId = "lp-" + gameName + "-" + pageId + "-" + matchIndex
        String externalId = "lp-" + gameName + "-" + pageId + "-" + matchIndex;

        // 队标：从 match-info-header 的 team-template-image-icon img 提取
        // 2026 结构：span.team-template-image-icon > a > img（无 lightmode/darkmode 双图）
        // 限定 div.match-info-header 避免误选 brkts-popup-body 里的选手队伍图标
        // TBD 队伍用 <i class="far fa-users"> 图标代替，无 img，返回 null
        Elements logoEls = match.select("div.match-info-header span.team-template-image-icon img");
        String logoA = null, logoB = null;
        if (logoEls.size() >= 2) {
            logoA = fixLogoUrl(logoEls.get(0).attr("src"));
            logoB = fixLogoUrl(logoEls.get(1).attr("src"));
        }

        RawMatch raw = new RawMatch();
        raw.setExternalId(externalId);
        raw.setGameId(gameId);
        raw.setTitle(teamA + " vs " + teamB);
        raw.setTeamA(teamA);
        raw.setTeamB(teamB);
        raw.setTeamALogo(logoA);
        raw.setTeamBLogo(logoB);
        raw.setTournament(pageTitle);
        raw.setMatchTime(matchTime);
        raw.setScoreA(scoreA);
        raw.setScoreB(scoreB);
        raw.setStatus(finished ? 2 : 0);  // 带比分视为已结束，否则为未开始(upcoming)
        raw.setResult(result);
        return raw;
    }

    /**
     * 从 brkts-match 块的 span.timer-object 提取比赛时间
     * <p>data-timestamp 为 Unix epoch 秒，转换为 Asia/Shanghai 时区 LocalDateTime
     *
     * @param match brkts-match 元素
     * @return 比赛时间；无 timer-object 或时间戳非法返回 null
     */
    private LocalDateTime extractMatchTime(Element match) {
        Element timer = match.selectFirst("span.timer-object");
        if (timer == null) {
            return null;
        }
        String ts = timer.attr("data-timestamp");
        if (ts == null || ts.isEmpty()) {
            return null;
        }
        try {
            long epochSeconds = Long.parseLong(ts.trim());
            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSeconds),
                    ZoneId.of("Asia/Shanghai"));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 opponent 元素提取队伍名称
     * <p>优先 .name a 文本，回退到带 title 属性的 a 链接文本
     */
    private String extractTeamName(Element opponentEl) {
        Element nameLink = opponentEl.selectFirst(".name a");
        if (nameLink == null) {
            nameLink = opponentEl.selectFirst("a[title]");
        }
        if (nameLink == null) {
            return "";
        }
        return cleanTeamName(nameLink.text());
    }

    /**
     * 解析比分文本为整数（容错：非数字返回 null）
     */
    private Integer parseScore(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 清洗队名：折叠空白，过滤 TBD/TBA 占位
     */
    private String cleanTeamName(String name) {
        if (name == null) return "";
        String s = name.replaceAll("\\s+", " ").trim();
        if (s.equalsIgnoreCase("tbd") || s.equalsIgnoreCase("tba")) {
            return "";
        }
        return s;
    }

    /**
     * 从赛事页 HTML 的 infobox 提取 Start Date 作为兜底比赛时间
     * <p>infobox 结构：...Start Date:</div><div ...>2025-02-12</div>
     * <p>找不到时回退到页面首个 ISO 日期
     *
     * @return 北京时间 00:00 时刻；无日期返回 null
     */
    private LocalDateTime extractStartDate(String html) {
        if (html == null || html.isEmpty()) return null;
        String dateStr = null;
        // 1. 优先匹配 "Start Date" 后紧跟的 ISO 日期
        Matcher m = START_DATE_PATTERN.matcher(html);
        if (m.find()) {
            dateStr = m.group(1);
        } else {
            // 2. 回退：页面首个 ISO 日期
            Matcher iso = ISO_DATE_PATTERN.matcher(html);
            if (iso.find()) {
                dateStr = iso.group(0);
            }
        }
        if (dateStr == null) return null;
        try {
            return LocalDate.parse(dateStr)
                    .atStartOfDay(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 发送带自定义 User-Agent 的 GET 请求并返回 JSON 根节点
     * <p>gzip 由 RestTemplateConfig 的 GzipSupportInterceptor 统一处理
     * （Liquipedia 强制要求 Accept-Encoding: gzip，缺失返回 406）
     */
    private JsonNode requestJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        // Liquipedia 要求标识爬虫身份，附带联系方式
        headers.set("User-Agent", "egg-esports/1.0 (contact@example.com)");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            // 打印完整堆栈，便于排查 gzip 解压/SSL/超时等具体异常类型
            log.error("Liquipedia 请求异常: {} - {}", url, e.getMessage(), e);
            return null;
        }
    }

    /** 将 Liquipedia 相对路径补全为完整 URL */
    private String fixLogoUrl(String src) {
        if (src == null || src.isEmpty()) return null;
        if (src.startsWith("http")) return src;
        return "https://liquipedia.net" + src;
    }

    /** 线程睡眠工具，处理中断 */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("LiquipediaAdapter ({}) 同步被中断", displayName);
        }
    }
}
