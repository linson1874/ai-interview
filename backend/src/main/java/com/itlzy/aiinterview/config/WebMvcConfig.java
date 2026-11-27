package com.itlzy.aiinterview.config;

import com.itlzy.aiinterview.util.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC 配置：注册登录拦截器与拦截路径。
 *  @author 林子阳 */
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
                .addPathPatterns("/user/me", "/resume/**","/ai/**","/interview/**","/review/**")
                .excludePathPatterns("/user/register", "/user/login"); // 注册与登录免检
    }
}