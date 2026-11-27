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

/** 登录拦截器：校验 JWT 与 Redis 登录存根，通过后把 userId 放入请求属性。
 *  @author 林子阳 */
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;   // 用于把拒绝响应序列化为 JSON

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

        // 校验签名与过期时间
        Long userId;
        try {
            userId = jwtUtil.parseUserId(token);
        } catch (JwtException e) {
            return reject(response);
        }

        // 校验 Redis 中的登录存根，不匹配视为已注销或被踢下线
        String cachedToken = redisTemplate.opsForValue().get(AuthServiceImpl.USER_KEY_PREFIX + userId);
        if (!token.equals(cachedToken)) {
            return reject(response);
        }

        // 校验通过，将 userId 放入请求属性供 Controller 使用
        request.setAttribute("userId", userId);
        return true;
    }

    /** 返回 401 + code=3；拦截器中手写响应而非抛异常（全局处理器给不出 401） */
    private boolean reject(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Result.of(ResultCode.UNAUTHORIZED, "未登录或凭证失效")));
        return false;   // false = 拦截请求
    }
}