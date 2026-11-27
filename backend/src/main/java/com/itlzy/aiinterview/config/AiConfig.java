package com.itlzy.aiinterview.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 大模型对话客户端配置。
 *  @author 林子阳 */
@Configuration
public class AiConfig {

    /** 构建全局 ChatClient，系统人设等定制统一在此处扩展 */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}