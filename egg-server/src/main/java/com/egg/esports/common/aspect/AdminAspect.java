package com.egg.esports.common.aspect;

import com.egg.esports.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 管理员权限校验切面
 * <p>拦截所有标注 {@link com.egg.esports.common.annotation.RequireAdmin} 的方法，
 * 校验当前登录用户 role 是否为 1（管理员），非管理员抛 BusinessException(403)。
 * <p>在 LoginInterceptor 之后执行，两层防护不冲突。
 */
@Aspect
@Component
@Slf4j
public class AdminAspect {

    @Around("@annotation(com.egg.esports.common.annotation.RequireAdmin)")
    public Object checkAdmin(ProceedingJoinPoint pjp) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            throw new BusinessException(403, "无管理员权限");
        }
        return pjp.proceed();
    }
}
