package com.egg.esports.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限校验注解
 * <p>标注在 Controller 方法上，由 {@link com.egg.esports.common.aspect.AdminAspect} 切面拦截，
 * 校验 request attribute 中的 role 是否为 1（管理员）。
 * <p>与 LoginInterceptor 的 admin 路径白名单形成两层防护，互不冲突。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
