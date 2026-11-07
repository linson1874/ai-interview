package com.itlzy.aiinterview;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChatClientTest {
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Test
    @Disabled("S1付费冒烟：需 DeepSeek key 有余额时手动放开；真打大模型不走 CI，合同裁决#10 E项")
    void smoke(){
        var answer = chatClientBuilder.build().prompt()
                .user("用一句话解释一下什么是SpringMVC")
                .call()
                .content();
        System.out.println(answer);
    }
}
