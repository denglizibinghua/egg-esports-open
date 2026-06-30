package com.egg.esports.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            new ObjectMapper().writeValue(response.getWriter(),
                    Result.error(401, "未登录"));
            return false;
        }

        token = token.substring(7);
        if (!jwtUtils.validate(token)) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            new ObjectMapper().writeValue(response.getWriter(),
                    Result.error(401, "登录已过期"));
            return false;
        }

        // 把用户 ID、用户名和角色放到 request attribute 里
        Long userId = jwtUtils.getUserId(token);
        String username = jwtUtils.getUsername(token);
        Integer role = jwtUtils.getRole(token);
        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role != null ? role : 0);

        // 管理接口需要 admin 角色
        String path = request.getRequestURI();
        if (path.startsWith("/api/match/create")
                || path.startsWith("/api/match/update")
                || path.startsWith("/api/match/settle")
                || path.startsWith("/api/match/batch-import")
                || path.startsWith("/api/match/sync")
                || (path.startsWith("/api/announcement") && !path.equals("/api/announcement/active"))) {
            if (role == null || role != 1) {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(403);
                new ObjectMapper().writeValue(response.getWriter(),
                        Result.error(403, "无管理权限"));
                return false;
            }
        }

        return true;
    }
}
