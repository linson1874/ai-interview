package com.itlzy.aiinterview.config;

import com.itlzy.aiinterview.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public WebMvcConfig(JwtUtil jwtUtil, StringRedisTemplate redisTemplate,
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor(jwtUtil, redisTemplate, objectMapper))
                .addPathPatterns("/user/me", "/resume/**")
                .excludePathPatterns("/user/register", "/user/login"); // 免检通道
    }
}