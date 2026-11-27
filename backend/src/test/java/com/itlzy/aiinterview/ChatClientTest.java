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
    @Disabled("付费冒烟：需 API key 有余额时手动放开，不进 CI")
    void smoke(){
        var answer = chatClientBuilder.build().prompt()
                .user("用一句话解释一下什么是SpringMVC")
                .call()
                .content();
        System.out.println(answer);
    }
}
