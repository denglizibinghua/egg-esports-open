package com.egg.esports.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于滑动窗口的简易限流拦截器
 * 生产环境建议替换为 Redis + Lua 脚本方案
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

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
        RateLimitWindow window = windows.computeIfAbsent(key,
                k -> new RateLimitWindow(annotation.window()));

        long now = System.currentTimeMillis();
        synchronized (window) {
            // 清理过期计数
            if (now - window.windowStart > window.windowMs) {
                window.reset(now);
            }

            window.count++;

            if (window.count > annotation.maxRequests()) {
                log.warn("限流触发: key={}, count={}, max={}", key, window.count, annotation.maxRequests());
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(429);
                new ObjectMapper().writeValue(response.getWriter(),
                        Result.error(429, annotation.message()));
                return false;
            }
        }

        return true;
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
