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
        // 登录拦截 — 未登录可浏览，放开所有公开读接口
        // 注意：/api/match/** 不再全局放行，改为细粒度放行 list/hot/tournaments，detail 由 LoginInterceptor 按 GET 放行，sync 需管理员
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/announcement/active",
                        "/api/game/**",
                        "/api/leaderboard/**",
                        "/api/match/list",
                        "/api/match/hot",
                        "/api/match/tournaments",
                        "/api/matches/**",
                        "/api/bet/count/**",
                        "/api/bet/odds/**",
                        "/api/comment/list",
                        "/api/comment/replies",
                        "/api/comment/count/**",
                        "/api/health"
                );

        // 限流拦截（扫描所有 /api/**，方法上有 @RateLimit 注解才生效）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
