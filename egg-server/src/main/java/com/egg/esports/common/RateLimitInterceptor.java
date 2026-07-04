package com.egg.esports.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 Redis + Lua 脚本的限流拦截器
 * <p>生产环境用 Redis 原子化令牌桶（INCR + EXPIRE）；
 * 测试环境无 Redis 时自动降级到内存限流（ConcurrentHashMap）。
 * <p>降级触发条件：
 * <ul>
 *   <li>StringRedisTemplate 未注入（无 Redis 自动配置时为 null）</li>
 *   <li>Redis 连接异常（网络故障等），catch 后走内存限流，不阻塞主流程</li>
 * </ul>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /**
     * Lua 脚本：原子化固定窗口限流
     * KEYS[1] = 限流 key
     * ARGV[1] = 窗口内最大请求数
     * ARGV[2] = 窗口时长（秒）
     * 返回 1 = 放行，0 = 拒绝
     */
    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local current = redis.call('INCR', key)
            if current == 1 then
                redis.call('EXPIRE', key, window)
            end
            if current > limit then return 0 end
            return 1
            """;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);

    /**
     * StringRedisTemplate 注入，required=false 保证无 Redis 时不报错。
     * 测试环境未排除 RedisAutoConfiguration（LettuceConnectionFactory 懒连接），此处非 null，
     * 但测试不经过 HTTP 拦截器，不会触发 Redis 调用；即便触发，下方 try-catch 会降级到内存限流。
     */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /** 内存限流降级方案（测试环境或 Redis 异常时使用） */
    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit annotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (annotation == null) {
            return true;
        }

        String key = buildKey(request);

        // Redis 可用时优先走 Redis Lua 限流
        if (redisTemplate != null) {
            try {
                Long allowed = redisTemplate.execute(
                        RATE_LIMIT_SCRIPT,
                        Collections.singletonList("rate_limit:" + key),
                        String.valueOf(annotation.maxRequests()),
                        String.valueOf(annotation.window()));
                if (allowed != null && allowed == 0L) {
                    log.warn("Redis 限流触发: key={}, max={}", key, annotation.maxRequests());
                    writeRateLimitResponse(response, annotation.message());
                    return false;
                }
                // allowed == 1 放行；allowed == null（异常情况）降级到内存
                if (allowed != null) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("Redis 限流异常，降级到内存限流: {}", e.getMessage());
            }
        }

        // 内存限流降级（测试环境或 Redis 异常）
        return memoryRateLimit(key, annotation, response);
    }

    /**
     * 内存限流降级逻辑（原实现）
     */
    private boolean memoryRateLimit(String key, RateLimit annotation, HttpServletResponse response) throws Exception {
        RateLimitWindow window = windows.computeIfAbsent(key, k -> new RateLimitWindow(annotation.window()));

        long now = System.currentTimeMillis();
        synchronized (window) {
            if (now - window.windowStart > window.windowMs) {
                window.reset(now);
            }
            window.count++;
            if (window.count > annotation.maxRequests()) {
                log.warn("内存限流触发: key={}, count={}, max={}", key, window.count, annotation.maxRequests());
                writeRateLimitResponse(response, annotation.message());
                return false;
            }
        }
        return true;
    }

    /**
     * 写入 429 限流响应
     */
    private void writeRateLimitResponse(HttpServletResponse response, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429);
        new ObjectMapper().writeValue(response.getWriter(), Result.error(429, message));
    }

    private String buildKey(HttpServletRequest request) {
        // 按 用户ID + URI 限流（未登录用户按 IP）
        Object userId = request.getAttribute("userId");
        String user = userId != null ? userId.toString() : request.getRemoteAddr();
        return user + ":" + request.getRequestURI();
    }

    private static class RateLimitWindow {
        final long windowMs;
        long windowStart;
        int count;

        RateLimitWindow(int windowSeconds) {
            this.windowMs = windowSeconds * 1000L;
            this.windowStart = System.currentTimeMillis();
            this.count = 0;
        }

        void reset(long now) {
            this.windowStart = now;
            this.count = 0;
        }
    }
}
