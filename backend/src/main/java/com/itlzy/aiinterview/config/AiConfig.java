package com.itlzy.aiinterview.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * starter 提供了 Builder(制造机)，但没直接给 ChatClient 成品——官方推荐自己 @Bean 组一次，
     * 将来加系统人设/日志 Advisor 都在这行 builder 后面接，改动集中在此
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}