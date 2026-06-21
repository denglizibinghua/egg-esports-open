package com.egg.esports.config;

import com.egg.esports.common.LoginInterceptor;
import com.egg.esports.common.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Value("${egg.upload.path:uploads}")
    private String uploadPath;

    @Value("${egg.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir.getAbsolutePath() + "/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        // 认证相关
                        "/api/user/login",
                        "/api/user/register",
                        // 游戏 & 公告（浏览）
                        "/api/game/list",
                        "/api/game/{id}",
                        "/api/announcement/active",
                        // ===== v19: 未登录可浏览 =====
                        "/api/match/list",
                        "/api/match/hot",
                        "/api/match/upcoming",
                        "/api/match/live",
                        "/api/match/finished",
                        "/api/match/{id}",        // GET 赛事详情
                        "/api/leaderboard/points",
                        "/api/leaderboard/accuracy",
                        "/api/leaderboard/worst-profit",
                        "/api/leaderboard/top/{n}",
                        "/api/leaderboard/accuracy/top/{n}",
                        "/api/leaderboard/worst-profit/top/{n}",
                        "/api/leaderboard/user-rank/{userId}",  // 公开排名查询
                        "/api/comment/list",
                        "/api/comment/replies",
                        "/api/bet/count/{id}"     // 押注人数统计
                );

        // 限流拦截（扫描所有 /api/**，方法上有 @RateLimit 注解才生效）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
