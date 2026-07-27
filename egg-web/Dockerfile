# ============================================================
# Egg Esports Frontend Dockerfile
# Multi-stage Node build -> Nginx serve
# ============================================================

# ---------- Stage 1: Builder ----------
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制依赖清单，利用 Docker 层缓存
COPY package.json package-lock.json ./

# 安装依赖（npm ci 在有 lock 文件时更快、更可重现）
RUN npm ci

# 复制源码与构建配置
COPY src ./src
COPY vite.config.ts tsconfig.json index.html ./

# 生产构建，输出到 /app/dist
RUN npm run build

# ---------- Stage 2: Runtime ----------
FROM nginx:alpine AS runtime

# 移除默认配置，使用自定义 nginx 配置
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/egg-esports.conf

# 从 builder 复制构建产物到 Nginx 静态目录
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Nginx 自带健康检查能力，使用 curl 探测
RUN apk add --no-cache curl

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]