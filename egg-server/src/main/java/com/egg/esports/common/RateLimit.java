package com.egg.esports.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * 用于防止接口被恶意刷请求
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 时间窗口（秒） */
    int window() default 10;

    /** 窗口内最大请求数 */
    int maxRequests() default 5;

    /** 限流提示消息 */
    String message() default "操作过于频繁，请稍后再试";
}
