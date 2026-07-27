# ============================================================
# Egg Esports Backend Dockerfile
# Multi-stage Maven build -> JRE runtime
# ============================================================

# ---------- Stage 1: Builder ----------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 先复制 pom.xml，利用 Docker 层缓存加速依赖解析
COPY pom.xml .
# 预下载依赖（可选，加速后续构建）
RUN mvn dependency:go-offline -B -q || true

# 复制源码
COPY src ./src

# 构建，跳过测试
RUN mvn package -DskipTests -B -q

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime

# 安装 curl 用于健康检查，并创建非 root 用户
RUN apk add --no-cache curl \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/target/egg-esports.jar app.jar

# 创建上传目录（后端文件上传功能使用）
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8088

# 健康检查（Actuator 健康端点）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-jar", "app.jar"]