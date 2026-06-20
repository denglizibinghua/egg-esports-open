package com.egg.esports.common.aspect;

import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.module.audit.EggOperationLog;
import com.egg.esports.module.audit.EggOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 操作日志切面
 * <p>拦截 @OperationLog 注解的方法，自动记录操作到数据库（异步）
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private EggOperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 构建日志实体
        EggOperationLog logEntry = new EggOperationLog();
        logEntry.setModule(operationLog.module());
        logEntry.setAction(operationLog.value());
        logEntry.setDescription(operationLog.value());

        // 方法全名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        logEntry.setMethod(method.getDeclaringClass().getName() + "." + method.getName());

        // 从 RequestContextHolder 拿请求信息（可能没有，如定时任务调用）
        HttpServletRequest request = getRequest();
        if (request != null) {
            logEntry.setRequestUrl(request.getRequestURI());
            logEntry.setIp(getIp(request));

            Object userId = request.getAttribute("userId");
            if (userId != null) {
                logEntry.setUserId((Long) userId);
            }
            Object role = request.getAttribute("role");
            if (role != null) {
                logEntry.setRole((Integer) role);
            }
            Object username = request.getAttribute("username");
            if (username != null) {
                logEntry.setUsername(username.toString());
            }
        }

        // 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
            logEntry.setStatus(1);
        } catch (Throwable e) {
            logEntry.setStatus(0);
            // 错误信息脱敏：如果异常消息里含 password 关键字，截断
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("password")) {
                msg = "[敏感信息已脱敏]";
            }
            logEntry.setErrorMsg(truncate(msg, 1000));
            // 重新抛出，不影响业务异常链
            throw e;
        } finally {
            logEntry.setCostMs(System.currentTimeMillis() - startTime);
            // 异步保存（不阻塞业务返回）
            try {
                operationLogService.saveAsync(logEntry);
            } catch (Exception e) {
                log.error("操作日志异步保存失败: {}", e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * 获取当前 HttpServletRequest（可能为 null，如定时任务场景）
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    /**
     * 获取客户端 IP，X-Forwarded-For 优先
     */
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // 多级代理取第一个
            int comma = ip.indexOf(',');
            if (comma > 0) {
                ip = ip.substring(0, comma);
            }
        }
        return ip;
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
