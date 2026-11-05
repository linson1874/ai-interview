package com.itlzy.aiinterview.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itlzy.aiinterview.common.Result;
import com.itlzy.aiinterview.common.ResultCode;
import com.itlzy.aiinterview.service.impl.AuthServiceImpl;
import com.itlzy.aiinterview.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;   // Spring 自带现成实例，专门用来把 Result 写成 JSON

    public LoginInterceptor(JwtUtil jwtUtil, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 取头：Authorization: Bearer xxx，剥掉前缀 7 个字符
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response);
        }
        String token = auth.substring(7);

        // 2. 第一关：验票面（假签名、过期都会抛 JwtException）
        Long userId;
        try {
            userId = jwtUtil.parseUserId(token);
        } catch (JwtException e) {
            return reject(response);
        }

        // 3. 第二关：查存根（注销过的票，票面依然真，但存根没了 → 拒）
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(AuthServiceImpl.TOKEN_KEY_PREFIX + token))) {
            return reject(response);
        }

        // 4. 两关全过：把 userId 别在"工牌"上带给 Controller
        request.setAttribute("userId", userId);
        return true;
    }

    /** 劝退：HTTP 401 + code=3（合同 1）。注意拦截器里不能"抛异常交给全局handler"，
     *  因为我们的 BusinessException handler 返回 HTTP 200，给不出 401，所以这里手写回执 */
    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Result.of(ResultCode.UNAUTHORIZED, "未登录或凭证失效")));
        return false;   // false = 拦截，请求到此为止，不会进 Controller
    }
}