# 🥚 Egg Esports

> 非盈利电竞赛事预测平台 — 纯彩池制，0% 抽水，猜中全分。

**线上地址**：[eggesports.com](https://eggesports.com)

---

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端 | Spring Boot 3.0.6 + Java 17 | REST API + WebSocket |
| ORM | MyBatis-Plus 3.5.3 | 注解式 SQL，Lambda 查询 |
| 数据库 | MySQL 8.0 | InnoDB，utf8mb4 |
| 缓存 | Redis 7 | Lua 限流 + 内存降级 |
| 鉴权 | JJWT 0.11.5 | HS256，7 天过期 |
| 前端 | Vue 3.4 + Element Plus 2.7 | 暗黑电竞风 |
| 构建 | Vite 5 | 按需引入，Gzip |
| 部署 | Nginx + systemd | HTTPS，限流，反向代理 |

## 支持游戏

| 游戏 | 数据源 | 类型 |
|------|--------|------|
| 无畏契约 (Valorant) | Riot Esports API | 官方 |
| 英雄联盟 (LoL) | Riot Esports API | 官方 |
| Dota 2 | OpenDota | 免费 |
| CS2 | GRID Open Access | 二线赛事 |

## 快速开始

### 环境要求

- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8.0
- Redis 7

### 1. 克隆项目

```bash
git clone git@github.com:denglizibinghua/egg-esports.git
cd egg-esports
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env，填入 MySQL/Redis 密码和 API Key
```

### 3. 启动后端

```bash
cd egg-server
mvn spring-boot:run
# 后端运行在 http://localhost:8088
# Swagger UI: http://localhost:8088/swagger-ui.html
```

### 4. 启动前端

```bash
cd egg-web
npm install
npm run dev
# 前端运行在 http://localhost:8080
```

### 5. 登录

默认管理员账号：`等离子蛋花`，密码见 `.env`。

---

## 项目结构

```
egg-esports/
├── egg-server/                # Spring Boot 后端
│   └── src/main/java/com/egg/esports/
│       ├── common/            # 公共组件（JWT、拦截器、限流、异常处理）
│       ├── config/            # 配置类（Redis、WebSocket、CORS、Swagger）
│       ├── module/            # 业务模块（12 个）
│       │   ├── user/          # 用户（注册、登录、资料、头像）
│       │   ├── match/         # 比赛（列表、详情、创建、结算）
│       │   ├── bet/           # 押注（下注、修改、查询、结算）
│       │   ├── game/          # 游戏目录
│       │   ├── comment/       # 评论（WebSocket 实时推送）
│       │   ├── announcement/  # 公告
│       │   ├── notification/  # 通知
│       │   ├── leaderboard/   # 排行榜
│       │   ├── loan/          # 贷款（救济、补贴、自动还款）
│       │   ├── audit/         # 操作日志（AOP 审计）
│       │   ├── odds/          # 动态赔率
│       │   └── sync/          # 外部数据同步（Adapter 模式）
│       └── sync/              # 数据源适配器（Riot、OpenDota、GRID）
├── egg-web/                   # Vue 3 前端
│   └── src/
│       ├── views/             # 页面（首页、赛程、排行、用户中心等）
│       ├── components/        # 组件（押注面板、评论区、管理后台等）
│       ├── router/            # 路由配置
│       ├── stores/            # Pinia 状态管理
│       ├── utils/             # 工具（Axios、WebSocket、格式化）
│       └── styles/            # 全局样式（暗黑主题）
├── deploy/                    # 部署文件（systemd、nginx、备份脚本）
├── docs/                      # 设计文档
│   ├── 设计分析文档.md         # 架构、数据库、API、类图
│   ├── 需求说明文档.md         # 用户故事、功能需求
│   └── 上线计划书_v2.md        # 版本路线图
└── .github/workflows/         # CI/CD
```

---

## 架构亮点

- **Adapter 模式**：数据源统一为 `GameSourceAdapter` 接口，新增游戏只需实现一个 Adapter
- **纯彩池制**：赔率 = 总彩池 / 单边彩池，0% 抽水
- **三层鉴权**：JWT Token → LoginInterceptor → @RequireAdmin AOP
- **乐观锁**：彩池更新、积分变更、贷款操作全部带版本/条件校验
- **Redis + Lua 限流**：API 限流，Redis 不可用时自动降级为内存限流
- **自动重试**：外部 API 调用 `@Retryable` + HttpClient5 连接池

## 部署

详见 `deploy/` 目录和 `docs/上线计划书_v2.md`。

```bash
# 后端重启
sudo systemctl restart egg-esports

# 前端更新
cd egg-web && npm run build
scp -r dist ubuntu@your-server:~/
ssh ubuntu@your-server "sudo cp -r ~/dist/* /var/www/html/egg-esports/"
```

## 文档

- [设计分析文档](docs/设计分析文档.md) — 架构、ER 图、API 列表、类图、时序图
- [需求说明文档](docs/需求说明文档.md) — 用户故事、功能需求、非功能需求
- [上线计划书](docs/上线计划书_v2.md) — 版本路线图、P0-P3 优先级

## License

Private — All rights reserved.
