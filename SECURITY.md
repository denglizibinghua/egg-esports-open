# Security Policy

## 支持范围

本项目为演示 / 学习用途的全栈示例，按 **MIT License** 以「现状」提供。安全问题会在能力范围内修复，但不提供任何形式的 SLA 或商业支持。

## 报告安全问题

**请不要通过公开 Issue 报告安全漏洞。**

请使用 GitHub 的私密报告渠道：

1. 打开仓库的 **Security** 标签页
2. 点击 **Report a vulnerability**（GitHub Security Advisories）
3. 说明：受影响的版本 / 提交、复现步骤、影响范围、可能的修复建议

我们会尽快确认并回复。请在修复发布前避免公开披露细节。

## 部署安全须知

本仓库不包含任何真实密钥。所有敏感配置均通过环境变量注入：

| 变量 | 要求 |
|------|------|
| `JWT_SECRET` | 至少 64 字符的随机字符串，务必自行生成（`openssl rand -base64 48`），不要使用示例值 |
| `DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` | 使用强密码，不要沿用 `.env.example` 中的占位值 |
| `REDIS_PASSWORD` | 公网可达时必须设置；建议仅内网访问 Redis |
| `RIOT_API_KEY` / `GRID_API_KEY` | 仅存放在部署环境的 `.env` 或密钥管理服务中，不要提交到仓库 |
| `EGG_CORS_ORIGINS` | 生产环境必须收敛为具体域名，不要使用 `*` |

其他建议：

- 生产环境以 `--spring.profiles.active=prod` 启动，`application-prod.yml` 中密码与密钥均为必填、无默认值
- 通过 Nginx 终止 HTTPS，后端仅监听 `127.0.0.1`
- 定期轮换 JWT 密钥与第三方 API Key
- `.env`、上传目录、数据库备份均已加入 `.gitignore`，请勿手动提交

## 已知风险

- 演示数据接口 `POST /api/seed/demo` 受 `@RequireAdmin` 保护；请勿在生产环境随意开放管理员权限
- 项目定位为积分制竞技演示，不涉及真实资金，请勿改造成任何形式的现金博彩系统
