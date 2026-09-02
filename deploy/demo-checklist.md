# Demo 站部署追平 Checklist — feat/homepage-v2-slim 51c3aab → your-server.example.com

> 生成时间：2026-09-02  
> 本地分支：`feat/homepage-v2-slim`  
> 本地 HEAD：`51c3aab9913ba739bf0f342ef0b89662aba5a3d9` (含 `b090195` 翻转修复 + `51c3aab` 标题自愈)  
> 生产当前：`v33`（需一次部署追平至 `51c3aab`）  
> 目标库：`egg_esports.egg_match` 3451/3452

---

## 0. 本地校验（已完成）

```bash
git -C D:\egg-esports log --oneline -6
# 51c3aab fix(sync): 标题与队名一致性自愈
# b090195 fix(sync): 修复占位符翻转导致队名重复 LOUD vs LOUD

git -C D:\egg-esports rev-parse HEAD
# 51c3aab9913ba739bf0f342ef0b89662aba5a3d9

git -C D:\egg-esports status
# feat/homepage-v2-slim，领先 origin 需部署

# 验证服务代码已含自愈逻辑
# MatchManageService.upsertByExternalId: 占位翻转 + 已脏自愈(260) + logo联动(283) + 标题一致性(322)

# SQL 已合并
# deploy/sql/fix-3451-3452-dup.sql — 按 externalId，非 id

# 构建
mvn -DskipTests package 或 mvn.cmd -DskipTests package（见 §1 验证）
```

---

## 1. 生产 SQL 热修（P0-1）— 手动在 your-server.example.com 执行

> ⚠️ 环境无直连 SSH，以下为手动执行清单，需登录 your-server.example.com 后逐条执行

### 1.1 上传 SQL

```bash
# 本地
scp deploy/sql/fix-3451-3452-dup.sql ubuntu@your-server.example.com:/tmp/

# 生产
ssh ubuntu@your-server.example.com
cat /tmp/fix-3451-3452-dup.sql
```

### 1.2 执行前快照

```bash
source /opt/egg-esports/.env
mysql -h 127.0.0.1 -u egg -p"$DB_PASSWORD" egg_esports -e "SELECT id,external_id,team_a,team_b,title,status FROM egg_match WHERE external_id IN ('115581239610515780','115581239610515784');"
```

### 1.3 执行修复

```bash
mysql -h 127.0.0.1 -u egg -p"$DB_PASSWORD" egg_esports < /tmp/fix-3451-3452-dup.sql
```

预期输出：
- 两行分别 `MIBR vs LOUD` / `LEV vs G2`
- `dup_cnt` 为 `0`
- 标题 TBD 残留 `0` 行

### 1.4 验证（必须为 0 行）

```sql
-- 队名重复全量
SELECT COUNT(*) FROM egg_match WHERE team_a = team_b AND team_a NOT IN ('TBD','TBA','?');
-- 期望：0

-- 标题残留
SELECT id,team_a,team_b,title FROM egg_match WHERE title LIKE '%TBD vs%' AND team_a != 'TBD' AND team_b != 'TBD';
-- 期望：0 行
```

### 1.5 缓存清理（可选）

```bash
redis-cli -a "$REDIS_PASSWORD" DEL "match:hot" "match:list" || echo "等待 10min 定时同步自动覆盖"
```

---

## 2. 生产部署追平（P0-2）— 手动在 your-server.example.com 执行

### 2.1 本地构建产物

```bash
# 后端
cd D:\egg-esports\egg-server
mvn -DskipTests package
# 产物：target/egg-server-*.jar

# 前端
cd D:\egg-esports\egg-web
npm run build
# 产物：dist/
```

### 2.2 上传并部署

```bash
# 本地上传
scp egg-server/target/egg-server-*.jar ubuntu@your-server.example.com:/tmp/egg-server.jar
scp -r egg-web/dist ubuntu@your-server.example.com:/tmp/dist

# 生产部署
ssh ubuntu@your-server.example.com
sudo cp /tmp/egg-server.jar /opt/egg-esports/egg-server.jar
sudo cp -r /tmp/dist/* /var/www/html/egg-esports/
sudo systemctl restart egg-esports
sudo systemctl status egg-esports --no-pager
journalctl -u egg-esports -n 50 --no-pager
```

### 2.3 版本校验

```bash
# 生产 HEAD 应与本地一致
ssh ubuntu@your-server.example.com "cd /opt/egg-esports && git rev-parse HEAD"
# 期望：51c3aab9913ba739bf0f342ef0b89662aba5a3d9

# 接口与本地同构
curl -s https://eggesports.com/api/match/hot?gameId=1&limit=6 | jq '.data[] | {title, teamA, teamB}'
curl -s "https://eggesports.com/api/match/list?gameId=1&page=1&size=20" | jq '.data.records[] | select(.teamA==.teamB) | {id, teamA, teamB}'
# 期望：第二条无输出（无重复队名）

# 浏览器检查：无 Mixed Content，fixLogoUrl 已处理 http→https
```

---

## 3. 演示数据回归（P0-3）

```bash
# 用 demo_admin/demo123 登录获取 TOKEN
TOKEN=$(curl -s -X POST https://eggesports.com/api/auth/login -H "Content-Type: application/json" -d '{"username":"demo_admin","password":"demo123"}' | jq -r '.data.token')

curl -X POST https://eggesports.com/api/seed/demo -H "Authorization: Bearer $TOKEN"
# 期望：200，Home 焦点6 + 赛程6 + 榜单 podium3 均非空
```

---

## 4. 未直连 SSH 说明

> 本清单执行环境无直连 your-server.example.com 的 SSH 网络通路（`ssh: connect to host your-server.example.com port 22: Connection timed out / Network unreachable`），故未自动推送。  
> 请按 §1-§3 手动在生产执行，执行后在本文档勾选并附 `SELECT` 与 `curl` 输出截图即闭环。  
> 后续 `upsertByExternalId` 已含自愈，脏数据不会复发；建议每周 `SELECT team_a=team_b` 巡检一次。

---

## 5. 回滚预案

- SQL 回滚：按 `external_id` 重新 UPDATE 回错误值仅用于演练，正常无需回滚（自愈逻辑已覆盖）。
- 服务回滚：`sudo systemctl restart egg-esports` 前备份 `/opt/egg-esports/egg-server.jar.bak`。
