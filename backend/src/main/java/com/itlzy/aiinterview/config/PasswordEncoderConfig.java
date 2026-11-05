package com.itlzy.aiinterview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean   // 告诉 Spring：这个方法的返回值存进你的"工具箱"，谁要谁注入
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}